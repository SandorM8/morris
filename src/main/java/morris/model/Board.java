package morris.model;

import morris.logic.BoardTopology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The logical board: which piece (if any) sits on which position. */
public class Board {
    private final BoardTopology topology;
    private final Map<Position, Piece> grid = new HashMap<>();

    public Board(GameMode mode) {
        this.topology = BoardTopology.of(mode);
        initializeBoard(mode);
    }

    private Board(Board other) {
        this.topology = other.topology;
        this.grid.putAll(other.grid);
    }

    public void initializeBoard(GameMode mode) {
        if (BoardTopology.of(mode) != topology) {
            throw new IllegalArgumentException("board was created for a different mode");
        }
        grid.clear();
    }

    public BoardTopology topology() {
        return topology;
    }

    public void placePiece(Position pos, Piece piece) {
        checkValid(pos);
        if (grid.containsKey(pos)) {
            throw new IllegalStateException("position " + pos.id() + " is occupied");
        }
        grid.put(pos, piece);
    }

    public Piece removePiece(Position pos) {
        checkValid(pos);
        return grid.remove(pos);
    }

    public Piece getPiece(Position pos) {
        return grid.get(pos);
    }

    public boolean isEmpty(Position pos) {
        return !grid.containsKey(pos);
    }

    public List<Position> positionsOf(Player player) {
        List<Position> result = new ArrayList<>();
        for (Position p : topology.positions()) {
            Piece piece = grid.get(p);
            if (piece != null && piece.owner() == player) {
                result.add(p);
            }
        }
        return result;
    }

    public int count(Player player) {
        int n = 0;
        for (Piece piece : grid.values()) {
            if (piece.owner() == player) {
                n++;
            }
        }
        return n;
    }

    public Board copy() {
        return new Board(this);
    }

    private void checkValid(Position pos) {
        if (pos == null || pos.id() < 0 || pos.id() >= topology.size()) {
            throw new IllegalArgumentException("invalid position " + pos);
        }
    }
}
