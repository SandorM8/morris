package morris.logic;

import morris.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Computer opponent. EASY plays a random legal turn; MEDIUM and HARD run a depth-limited
 * minimax search with alpha-beta pruning (depth counted in whole turns, i.e. a placement or
 * move together with its capture).
 */
public class AIPlayer {
    private static final int WIN = 100_000;
    private static final int MEDIUM_DEPTH = 2;
    private static final int HARD_DEPTH = 3;

    private final AIDifficulty difficulty;
    private final Random random;

    public AIPlayer(AIDifficulty difficulty) {
        this(difficulty, new Random());
    }

    public AIPlayer(AIDifficulty difficulty, Random random) {
        this.difficulty = difficulty;
        this.random = random;
    }

    /** Chooses a turn for the current player. Returns null if the game is over. Does not modify {@code logic}. */
    public Turn chooseTurn(GameLogic logic) {
        List<Turn> turns = generateTurns(logic);
        if (turns.isEmpty()) {
            return null;
        }
        Collections.shuffle(turns, random);
        if (difficulty == AIDifficulty.EASY) {
            return turns.get(0);
        }
        int depth = difficulty == AIDifficulty.MEDIUM ? MEDIUM_DEPTH : HARD_DEPTH;
        Player me = logic.state().currentPlayer();
        Turn best = turns.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (Turn t : turns) {
            GameLogic child = logic.copy();
            t.applyTo(child);
            int score = search(child, depth - 1, bestScore, Integer.MAX_VALUE, me);
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }

    private int search(GameLogic g, int depth, int alpha, int beta, Player me) {
        GameState s = g.state();
        if (s.isFinished()) {
            return s.winner() == me ? WIN + depth : -(WIN + depth);
        }
        if (depth == 0) {
            return evaluate(g, me);
        }
        List<Turn> turns = generateTurns(g);
        boolean maximizing = s.currentPlayer() == me;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Turn t : turns) {
            GameLogic child = g.copy();
            t.applyTo(child);
            int score = search(child, depth - 1, alpha, beta, me);
            if (maximizing) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    /** Static evaluation from {@code me}'s point of view. */
    int evaluate(GameLogic g, Player me) {
        GameState s = g.state();
        Player opp = me.opponent();
        int score = 100 * (s.totalPieces(me) - s.totalPieces(opp));
        score += 15 * (openTwos(g, me) - openTwos(g, opp));
        score += 4 * (mobility(g, me) - mobility(g, opp));
        return score;
    }

    /** Mill lines where the player has two pieces and the third node is empty. */
    private int openTwos(GameLogic g, Player p) {
        Board b = g.state().board();
        int n = 0;
        for (List<Position> line : g.topology().mills()) {
            int own = 0, empty = 0;
            for (Position pos : line) {
                Piece piece = b.getPiece(pos);
                if (piece == null) empty++;
                else if (piece.owner() == p) own++;
            }
            if (own == 2 && empty == 1) n++;
        }
        return n;
    }

    private int mobility(GameLogic g, Player p) {
        Board b = g.state().board();
        int n = 0;
        for (Position pos : b.positionsOf(p)) {
            for (Position nb : g.topology().neighbors(pos)) {
                if (b.isEmpty(nb)) n++;
            }
        }
        return n;
    }

    /** Every legal complete turn for the current player. */
    public static List<Turn> generateTurns(GameLogic g) {
        List<Turn> turns = new ArrayList<>();
        GameState s = g.state();
        switch (s.phase()) {
            case PLACING -> {
                for (Position to : g.emptyPositions()) {
                    addWithCaptures(g, turns, null, to);
                }
            }
            case MOVING, FLYING -> {
                for (Position from : s.board().positionsOf(s.currentPlayer())) {
                    for (Position to : g.legalDestinations(from)) {
                        addWithCaptures(g, turns, from, to);
                    }
                }
            }
            default -> { /* REMOVING / FINISHED: not a turn start */ }
        }
        return turns;
    }

    private static void addWithCaptures(GameLogic g, List<Turn> turns, Position from, Position to) {
        GameLogic probe = g.copy();
        boolean ok = from == null ? probe.place(to) : probe.move(from, to);
        if (!ok) {
            return;
        }
        if (probe.state().phase() == GamePhase.REMOVING) {
            for (Position r : probe.removablePieces()) {
                turns.add(new Turn(from, to, r));
            }
        } else {
            turns.add(new Turn(from, to, null));
        }
    }
}
