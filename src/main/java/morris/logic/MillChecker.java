package morris.logic;

import morris.model.Board;
import morris.model.Piece;
import morris.model.Player;
import morris.model.Position;

import java.util.List;

/** Answers mill questions using the mill lines of the board's topology. */
public final class MillChecker {
    private MillChecker() {
    }

    /** True if the piece standing on pos (owned by player) is part of a complete mill. */
    public static boolean isInMill(Board board, Position pos, Player player) {
        for (List<Position> line : board.topology().millsContaining(pos)) {
            if (isComplete(board, line, player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isComplete(Board board, List<Position> line, Player player) {
        for (Position p : line) {
            Piece piece = board.getPiece(p);
            if (piece == null || piece.owner() != player) {
                return false;
            }
        }
        return true;
    }
}
