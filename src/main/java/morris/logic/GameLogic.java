package morris.logic;

import morris.model.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Enforces the rules. A turn is one action (place / move / fly), optionally followed by
 * one removal when a mill was formed. All mutating methods return false and change nothing
 * when the action is illegal.
 */
public class GameLogic {
    private final GameConfig config;
    private final BoardTopology topology;
    private GameState state;
    private final Stack<GameState> history = new Stack<>(); // snapshots taken at the start of each turn

    public GameLogic(GameConfig config) {
        this(config, new GameState(config.mode()));
    }

    public GameLogic(GameConfig config, GameState state) {
        this.config = config;
        this.topology = BoardTopology.of(config.mode());
        this.state = state;
    }

    /** A copy without undo history, used by the AI to search. */
    public GameLogic copy() {
        return new GameLogic(config, state.copy());
    }

    public GameConfig config() {
        return config;
    }

    public GameState state() {
        return state;
    }

    public BoardTopology topology() {
        return topology;
    }

    // ---------------------------------------------------------------- actions

    public boolean place(Position pos) {
        if (state.phase() != GamePhase.PLACING || !state.board().isEmpty(pos)) {
            return false;
        }
        history.push(state.copy());
        Player me = state.currentPlayer();
        Piece piece = state.hand(me).remove(state.hand(me).size() - 1);
        state.board().placePiece(pos, piece);
        afterAction(pos);
        return true;
    }

    /** Slides a piece to an adjacent empty node, or jumps anywhere when the phase is FLYING. */
    public boolean move(Position from, Position to) {
        if (!legalDestinations(from).contains(to)) {
            return false;
        }
        history.push(state.copy());
        Piece piece = state.board().removePiece(from);
        state.board().placePiece(to, piece);
        afterAction(to);
        return true;
    }

    public boolean remove(Position pos) {
        if (state.phase() != GamePhase.REMOVING || !removablePieces().contains(pos)) {
            return false;
        }
        state.board().removePiece(pos);
        endTurn();
        return true;
    }

    // ---------------------------------------------------------------- queries

    /** Positions the current player may move the piece on {@code from} to. Empty if not allowed. */
    public Set<Position> legalDestinations(Position from) {
        Set<Position> result = new LinkedHashSet<>();
        GamePhase phase = state.phase();
        if (phase != GamePhase.MOVING && phase != GamePhase.FLYING) {
            return result;
        }
        Piece piece = state.board().getPiece(from);
        if (piece == null || piece.owner() != state.currentPlayer()) {
            return result;
        }
        if (phase == GamePhase.FLYING) {
            for (Position p : topology.positions()) {
                if (state.board().isEmpty(p)) result.add(p);
            }
        } else {
            for (Position p : topology.neighbors(from)) {
                if (state.board().isEmpty(p)) result.add(p);
            }
        }
        return result;
    }

    public List<Position> emptyPositions() {
        List<Position> result = new ArrayList<>();
        for (Position p : topology.positions()) {
            if (state.board().isEmpty(p)) result.add(p);
        }
        return result;
    }

    /**
     * Opponent pieces that may be captured now: those not in a mill, or all of them
     * if every opponent piece is part of a mill.
     */
    public List<Position> removablePieces() {
        Player opp = state.currentPlayer().opponent();
        List<Position> all = state.board().positionsOf(opp);
        List<Position> free = new ArrayList<>();
        for (Position p : all) {
            if (!MillChecker.isInMill(state.board(), p, opp)) free.add(p);
        }
        return free.isEmpty() ? all : free;
    }

    /** What {@code player} would have to do on their next turn (ignoring mills). */
    public GamePhase phaseFor(Player player) {
        if (state.handSize(player) > 0) {
            return GamePhase.PLACING;
        }
        if (config.has(HouseRule.FLYING_ALLOWED) && state.totalPieces(player) == 3) {
            return GamePhase.FLYING;
        }
        return GamePhase.MOVING;
    }

    public boolean hasLegalMove(Player player) {
        GamePhase phase = phaseFor(player);
        if (phase == GamePhase.PLACING || phase == GamePhase.FLYING) {
            return true; // placing is always possible; a flyer can jump (the board is never full)
        }
        for (Position p : state.board().positionsOf(player)) {
            for (Position n : topology.neighbors(p)) {
                if (state.board().isEmpty(n)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------- undo

    public boolean canUndo() {
        return config.has(HouseRule.UNDO_ENABLED) && !history.isEmpty() && !state.isFinished();
    }

    /**
     * Reverts to the start of the previous turn. Against the computer this reverts both
     * the computer's reply and the human's move, so it is the human's turn again.
     */
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        GameState restored = history.pop();
        if (config.vsComputer()) {
            Player human = config.aiPlayer().opponent();
            while (restored.currentPlayer() != human && !history.isEmpty()) {
                restored = history.pop();
            }
        }
        state = restored;
        return true;
    }

    // --------------------------------------------------------------- internals

    private void afterAction(Position moved) {
        Player me = state.currentPlayer();
        if (MillChecker.isInMill(state.board(), moved, me)) {
            if (config.mode().capturesOnMill()) {
                state.setPhase(GamePhase.REMOVING);
            } else {
                finish(me); // Three Men's Morris: the first mill wins
            }
            return;
        }
        endTurn();
    }

    private void endTurn() {
        Player mover = state.currentPlayer();
        Player next = mover.opponent();
        state.setCurrentPlayer(next);
        if (config.mode().capturesOnMill() && state.totalPieces(next) < 3) {
            finish(mover);
            return;
        }
        if (!hasLegalMove(next)) {
            finish(mover); // blocked
            return;
        }
        state.setPhase(phaseFor(next));
    }

    private void finish(Player winner) {
        state.setWinner(winner);
        state.setCurrentPlayer(winner);
        state.setPhase(GamePhase.FINISHED);
    }
}
