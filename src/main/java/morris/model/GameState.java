package morris.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A complete snapshot of a game in progress: board, pieces in hand, whose turn it is
 * and what they must do. Copies are deep, so a copy can be pushed on the undo stack.
 */
public class GameState {
    private final GameMode mode;
    private Board board;
    private final Map<Player, List<Piece>> hands = new EnumMap<>(Player.class);
    private Player currentPlayer = Player.WHITE;
    private GamePhase phase = GamePhase.PLACING;
    private Player winner; // only set when phase == FINISHED

    public GameState(GameMode mode) {
        this.mode = mode;
        this.board = new Board(mode);
        for (Player p : Player.values()) {
            List<Piece> hand = new ArrayList<>();
            for (int i = 0; i < mode.piecesPerPlayer(); i++) {
                hand.add(new Piece(p));
            }
            hands.put(p, hand);
        }
    }

    private GameState(GameState o) {
        this.mode = o.mode;
        this.board = o.board.copy();
        for (Player p : Player.values()) {
            hands.put(p, new ArrayList<>(o.hands.get(p)));
        }
        this.currentPlayer = o.currentPlayer;
        this.phase = o.phase;
        this.winner = o.winner;
    }

    public GameState copy() {
        return new GameState(this);
    }

    public GameMode mode() {
        return mode;
    }

    public Board board() {
        return board;
    }

    public List<Piece> hand(Player p) {
        return hands.get(p);
    }

    public int handSize(Player p) {
        return hands.get(p).size();
    }

    /** Pieces still in the game: on the board plus in hand. */
    public int totalPieces(Player p) {
        return board.count(p) + handSize(p);
    }

    public Player currentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player p) {
        this.currentPlayer = p;
    }

    public GamePhase phase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Player winner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }
}
