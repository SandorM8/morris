package morris.logic;

import morris.model.GameMode;
import morris.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed geometry of a board: node coordinates on a square grid, adjacency and mill lines.
 * Nine Men's Morris: 24 nodes on a 7x7 grid (ids 0-7 outer ring, 8-15 middle, 16-23 inner),
 * 16 mill lines. Three Men's Morris: 9 nodes on a 3x3 grid (id = row * 3 + col), 8 mill lines.
 */
public final class BoardTopology {
    private static final BoardTopology THREE = buildThree();
    private static final BoardTopology NINE = buildNine();

    private final int gridSize;
    private final int[][] coords;                    // id -> {row, col}
    private final List<Position> positions;
    private final List<List<Position>> neighbors;    // id -> adjacent positions
    private final List<List<Position>> mills;        // every mill line
    private final List<List<List<Position>>> millsByPosition;

    public static BoardTopology of(GameMode mode) {
        return mode == GameMode.THREE_MENS_MORRIS ? THREE : NINE;
    }

    private BoardTopology(int gridSize, int[][] coords, int[][] edges, int[][] lines) {
        this.gridSize = gridSize;
        this.coords = coords;
        List<Position> ps = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            ps.add(new Position(i));
        }
        this.positions = List.copyOf(ps);

        List<List<Position>> nb = new ArrayList<>();
        List<List<List<Position>>> mp = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            nb.add(new ArrayList<>());
            mp.add(new ArrayList<>());
        }
        for (int[] e : edges) {
            nb.get(e[0]).add(positions.get(e[1]));
            nb.get(e[1]).add(positions.get(e[0]));
        }
        List<List<Position>> ml = new ArrayList<>();
        for (int[] line : lines) {
            List<Position> l = List.of(positions.get(line[0]), positions.get(line[1]), positions.get(line[2]));
            ml.add(l);
            for (Position p : l) {
                mp.get(p.id()).add(l);
            }
        }
        this.neighbors = nb.stream().map(List::copyOf).toList();
        this.mills = List.copyOf(ml);
        this.millsByPosition = mp.stream().map(List::copyOf).toList();
    }

    private static BoardTopology buildNine() {
        int[][] coords = new int[24][];
        List<int[]> edges = new ArrayList<>();
        List<int[]> lines = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            int lo = r, mid = 3, hi = 6 - r;
            int[][] ring = {{lo, lo}, {lo, mid}, {lo, hi}, {mid, hi}, {hi, hi}, {hi, mid}, {hi, lo}, {mid, lo}};
            for (int i = 0; i < 8; i++) {
                int id = r * 8 + i;
                coords[id] = ring[i];
                edges.add(new int[]{id, r * 8 + (i + 1) % 8});
                if (i % 2 == 0) { // corner-started line along one side of the square
                    lines.add(new int[]{id, r * 8 + (i + 1) % 8, r * 8 + (i + 2) % 8});
                }
                if (i % 2 == 1 && r < 2) { // spokes between rings
                    edges.add(new int[]{id, id + 8});
                }
            }
        }
        for (int i = 1; i < 8; i += 2) {
            lines.add(new int[]{i, 8 + i, 16 + i});
        }
        return new BoardTopology(7, coords, edges.toArray(new int[0][]), lines.toArray(new int[0][]));
    }

    private static BoardTopology buildThree() {
        int[][] coords = new int[9][];
        List<int[]> edges = new ArrayList<>();
        List<int[]> lines = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int id = r * 3 + c;
                coords[id] = new int[]{r, c};
                if (c < 2) edges.add(new int[]{id, id + 1});
                if (r < 2) edges.add(new int[]{id, id + 3});
            }
            lines.add(new int[]{r * 3, r * 3 + 1, r * 3 + 2}); // row
            lines.add(new int[]{r, r + 3, r + 6});             // column
        }
        for (int corner : new int[]{0, 2, 6, 8}) {
            edges.add(new int[]{corner, 4});                    // diagonals meet in the centre
        }
        lines.add(new int[]{0, 4, 8});
        lines.add(new int[]{2, 4, 6});
        return new BoardTopology(3, coords, edges.toArray(new int[0][]), lines.toArray(new int[0][]));
    }

    public int size() {
        return coords.length;
    }

    /** Number of grid rows/columns the board is drawn on (7 or 3). */
    public int gridSize() {
        return gridSize;
    }

    public List<Position> positions() {
        return positions;
    }

    public List<Position> neighbors(Position p) {
        return neighbors.get(p.id());
    }

    public List<List<Position>> mills() {
        return mills;
    }

    public List<List<Position>> millsContaining(Position p) {
        return millsByPosition.get(p.id());
    }

    public int row(Position p) {
        return coords[p.id()][0];
    }

    public int col(Position p) {
        return coords[p.id()][1];
    }

    /** The position at a grid cell, or null if there is no node there. */
    public Position at(int row, int col) {
        for (int i = 0; i < coords.length; i++) {
            if (coords[i][0] == row && coords[i][1] == col) {
                return positions.get(i);
            }
        }
        return null;
    }
}
