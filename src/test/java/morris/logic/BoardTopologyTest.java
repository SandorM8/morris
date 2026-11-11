package morris.logic;

import morris.model.GameMode;
import morris.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTopologyTest {

    @Test
    void nineMensMorrisHas24NodesAnd16Mills() {
        BoardTopology t = BoardTopology.of(GameMode.NINE_MENS_MORRIS);
        assertEquals(24, t.size());
        assertEquals(16, t.mills().size());
        for (Position p : t.positions()) {
            assertEquals(2, t.millsContaining(p).size(), "every node lies on exactly two mills");
        }
        int degreeSum = t.positions().stream().mapToInt(p -> t.neighbors(p).size()).sum();
        assertEquals(64, degreeSum, "the board has 32 edges");
    }

    @Test
    void nineMensMorrisAdjacencyIsSymmetricAndOnGrid() {
        BoardTopology t = BoardTopology.of(GameMode.NINE_MENS_MORRIS);
        for (Position p : t.positions()) {
            for (Position n : t.neighbors(p)) {
                assertTrue(t.neighbors(n).contains(p));
                assertTrue(t.row(p) == t.row(n) || t.col(p) == t.col(n), "edges are horizontal or vertical");
            }
        }
        assertEquals(t.positions().get(0), t.at(0, 0));
        assertNull(t.at(1, 2));
    }

    @Test
    void threeMensMorrisHas9NodesAnd8Mills() {
        BoardTopology t = BoardTopology.of(GameMode.THREE_MENS_MORRIS);
        assertEquals(9, t.size());
        assertEquals(8, t.mills().size());
        assertEquals(8, t.neighbors(new Position(4)).size(), "centre touches every other node");
        assertEquals(3, t.neighbors(new Position(0)).size(), "corner: two edges + the centre diagonal");
        assertEquals(3, t.neighbors(new Position(1)).size());
        List<Position> diagonal = List.of(new Position(0), new Position(4), new Position(8));
        assertTrue(t.mills().contains(diagonal));
    }
}
