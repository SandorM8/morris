package morris.controller;

import morris.logic.AIPlayer;
import morris.logic.GameLogic;
import morris.logic.Turn;
import morris.model.*;
import morris.service.JsonGameManager;
import morris.service.SaveData;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Connects the views to the rules: translates clicks into actions, remembers the selected
 * piece and runs the computer opponent on a background thread. All methods are called on the
 * Swing event thread; listeners are notified on it too.
 */
public class GameController {

    public interface Listener {
        void stateChanged();

        void gameOver(Player winner);
    }

    private final List<Listener> listeners = new ArrayList<>();
    private final JsonGameManager jsonManager = new JsonGameManager();
    private final Random random = new Random();
    private GameLogic logic;
    private Position selected;
    private boolean aiThinking;
    private int generation; // bumped on new game / load so stale AI results are ignored

    public GameController(GameConfig config) {
        this.logic = new GameLogic(config);
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    // ------------------------------------------------------------- read access

    public GameLogic logic() {
        return logic;
    }

    public GameConfig config() {
        return logic.config();
    }

    public GameState state() {
        return logic.state();
    }

    public Position selected() {
        return selected;
    }

    public boolean isAiThinking() {
        return aiThinking;
    }

    /** True when the human(s) may currently click on the board. */
    public boolean acceptsInput() {
        return !state().isFinished() && !aiThinking && !isAiTurn();
    }

    public boolean canUndo() {
        return !aiThinking && logic.canUndo();
    }

    /** Nodes to highlight as possible targets of the next click. */
    public Set<Position> targets() {
        if (!acceptsInput()) {
            return Collections.emptySet();
        }
        return switch (state().phase()) {
            case MOVING, FLYING -> selected == null ? Collections.emptySet() : logic.legalDestinations(selected);
            default -> Collections.emptySet();
        };
    }

    /** Opponent pieces that may be removed right now (empty unless in the REMOVING phase). */
    public List<Position> removable() {
        if (state().phase() != GamePhase.REMOVING || !acceptsInput()) {
            return List.of();
        }
        return logic.removablePieces();
    }

    // ----------------------------------------------------------------- actions

    public void newGame(GameConfig config) {
        generation++;
        aiThinking = false;
        selected = null;
        logic = new GameLogic(config);
        fireChanged();
        maybeStartAi();
    }

    public void load(Path file) throws IOException {
        SaveData data = jsonManager.load(file);
        generation++;
        aiThinking = false;
        selected = null;
        logic = new GameLogic(data.config(), data.state());
        fireChanged();
        maybeStartAi();
    }

    public void save(Path file) throws IOException {
        jsonManager.save(file, logic.config(), logic.state());
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        selected = null;
        logic.undo();
        fireChanged();
    }

    public void onPositionClicked(Position pos) {
        if (!acceptsInput()) {
            return;
        }
        GameState s = state();
        boolean acted = false;
        switch (s.phase()) {
            case PLACING -> acted = logic.place(pos);
            case REMOVING -> acted = logic.remove(pos);
            case MOVING, FLYING -> acted = handleMoveClick(pos);
            default -> { }
        }
        if (acted) {
            selected = null;
        }
        fireChanged();
        if (acted) {
            afterAction();
        }
    }

    private boolean handleMoveClick(Position pos) {
        Piece clicked = state().board().getPiece(pos);
        if (selected != null && pos.equals(selected)) {
            selected = null;
            return false;
        }
        if (clicked != null && clicked.owner() == state().currentPlayer()) {
            selected = logic.legalDestinations(pos).isEmpty() ? null : pos;
            return false;
        }
        if (selected != null) {
            return logic.move(selected, pos);
        }
        return false;
    }

    // --------------------------------------------------------------------- AI

    private boolean isAiTurn() {
        return config().vsComputer() && state().currentPlayer() == config().aiPlayer();
    }

    private void afterAction() {
        if (state().isFinished()) {
            for (Listener l : new ArrayList<>(listeners)) {
                l.gameOver(state().winner());
            }
            return;
        }
        maybeStartAi();
    }

    private void maybeStartAi() {
        if (state().isFinished() || !isAiTurn() || aiThinking) {
            return;
        }
        if (state().phase() == GamePhase.REMOVING) { // only possible after loading a mid-turn save
            List<Position> options = logic.removablePieces();
            logic.remove(options.get(random.nextInt(options.size())));
            fireChanged();
            afterAction();
            return;
        }
        aiThinking = true;
        fireChanged();
        final int myGeneration = generation;
        final GameLogic snapshot = logic.copy();
        final AIPlayer ai = new AIPlayer(config().aiDifficulty());
        new SwingWorker<Turn, Void>() {
            @Override
            protected Turn doInBackground() {
                return ai.chooseTurn(snapshot);
            }

            @Override
            protected void done() {
                if (myGeneration != generation) {
                    return; // a new game or load replaced this one
                }
                aiThinking = false;
                try {
                    Turn turn = get();
                    if (turn != null) {
                        turn.applyTo(logic);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                selected = null;
                fireChanged();
                afterAction();
            }
        }.execute();
    }

    private void fireChanged() {
        for (Listener l : new ArrayList<>(listeners)) {
            l.stateChanged();
        }
    }
}
