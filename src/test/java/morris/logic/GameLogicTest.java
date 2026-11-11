package morris.logic;

import morris.model.*;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {

    private static final GameConfig NINE = new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.PLAYER,
            AIDifficulty.MEDIUM, EnumSet.of(HouseRule.UNDO_ENABLED));
    private static final GameConfig THREE = new GameConfig(GameMode.THREE_MENS_MORRIS, OpponentType.PLAYER,
            AIDifficulty.MEDIUM, EnumSet.of(HouseRule.UNDO_ENABLED));
    private static final GameConfig NINE_FLY = new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.PLAYER,
            AIDifficulty.MEDIUM, EnumSet.of(HouseRule.FLYING_ALLOWED));

    private static Position p(int id) {
        return new Position(id);
    }

    private static void put(GameState s, Player owner, int... ids) {
        for (int id : ids) s.board().placePiece(p(id), new Piece(owner));
    }

    private static void setHand(GameState s, Player owner, int n) {
        s.hand(owner).clear();
        for (int i = 0; i < n; i++) s.hand(owner).add(new Piece(owner));
    }

    @Test
    void turnsAlternateWhilePlacing() {
        GameLogic g = new GameLogic(NINE);
        assertEquals(Player.WHITE, g.state().currentPlayer());
        assertTrue(g.place(p(0)));
        assertEquals(Player.BLACK, g.state().currentPlayer());
        assertFalse(g.place(p(0)), "occupied");
        assertTrue(g.place(p(8)));
        assertEquals(8, g.state().handSize(Player.WHITE));
        assertEquals(GamePhase.PLACING, g.state().phase());
    }

    @Test
    void millWhilePlacingStartsRemovalAndRemovalEndsTurn() {
        GameLogic g = new GameLogic(NINE);
        g.place(p(0)); g.place(p(8)); g.place(p(1)); g.place(p(9));
        assertTrue(g.place(p(2)));
        assertEquals(GamePhase.REMOVING, g.state().phase());
        assertEquals(Player.WHITE, g.state().currentPlayer());
        assertFalse(g.place(p(3)), "must remove first");
        assertFalse(g.remove(p(0)), "cannot remove own piece");
        assertTrue(g.remove(p(8)));
        assertEquals(Player.BLACK, g.state().currentPlayer());
        assertEquals(GamePhase.PLACING, g.state().phase());
        assertEquals(8, g.state().totalPieces(Player.BLACK));
    }

    @Test
    void piecesInMillsAreProtectedUnlessAllAre() {
        GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s, Player.WHITE, 0); setHand(s, Player.BLACK, 0);
        put(s, Player.BLACK, 0, 1, 2, 12);
        put(s, Player.WHITE, 16, 18, 20, 22);
        s.setPhase(GamePhase.REMOVING);
        GameLogic g = new GameLogic(NINE, s);
        assertEquals(List.of(p(12)), g.removablePieces());
        assertFalse(g.remove(p(0)));

        GameState s2 = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s2, Player.WHITE, 0); setHand(s2, Player.BLACK, 0);
        put(s2, Player.BLACK, 0, 1, 2);
        put(s2, Player.WHITE, 16, 18, 20, 22);
        s2.setPhase(GamePhase.REMOVING);
        GameLogic g2 = new GameLogic(NINE, s2);
        assertEquals(3, g2.removablePieces().size());
    }

    @Test
    void threeMensMorrisFirstMillWins() {
        GameLogic g = new GameLogic(THREE);
        g.place(p(0)); g.place(p(3)); g.place(p(1)); g.place(p(4));
        assertTrue(g.place(p(2)));
        assertTrue(g.state().isFinished());
        assertEquals(Player.WHITE, g.state().winner());
    }

    @Test
    void threeMensMorrisMovesAfterPlacing() {
        GameLogic g = new GameLogic(THREE);
        // W: 0,5,7  B: 1,4,8 -> no mills
        g.place(p(0)); g.place(p(1)); g.place(p(5)); g.place(p(4)); g.place(p(7)); g.place(p(8));
        assertEquals(GamePhase.MOVING, g.state().phase());
        assertEquals(Player.WHITE, g.state().currentPlayer());
        assertTrue(g.legalDestinations(p(0)).contains(p(3)));
        assertFalse(g.legalDestinations(p(0)).contains(p(4)), "occupied");
    }

    @Test
    void movingOnlyToAdjacentEmptyNodes() {
        GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s, Player.WHITE, 0); setHand(s, Player.BLACK, 0);
        put(s, Player.WHITE, 0, 12, 13, 17);
        put(s, Player.BLACK, 20, 21, 22, 23);
        s.setPhase(GamePhase.MOVING);
        GameLogic g = new GameLogic(NINE, s);
        assertFalse(g.move(p(0), p(2)), "not adjacent");
        assertFalse(g.move(p(20), p(19)), "not your piece");
        assertTrue(g.move(p(0), p(1)));
        assertEquals(Player.BLACK, g.state().currentPlayer());
    }

    @Test
    void flyingOnlyAtThreePiecesWhenEnabled() {
        GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s, Player.WHITE, 0); setHand(s, Player.BLACK, 0);
        put(s, Player.WHITE, 0, 5, 12);
        put(s, Player.BLACK, 16, 18, 20, 22);
        s.setPhase(GamePhase.FLYING);
        GameLogic g = new GameLogic(NINE_FLY, s);
        assertEquals(GamePhase.FLYING, g.phaseFor(Player.WHITE));
        assertEquals(GamePhase.MOVING, g.phaseFor(Player.BLACK));
        assertTrue(g.move(p(0), p(9)), "jump to a non-adjacent node");

        GameLogic noFly = new GameLogic(NINE, s.copy());
        assertEquals(GamePhase.MOVING, noFly.phaseFor(Player.WHITE));
    }

    @Test
    void reducingOpponentBelowThreeWins() {
        GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s, Player.WHITE, 1); setHand(s, Player.BLACK, 0);
        put(s, Player.WHITE, 0, 1);
        put(s, Player.BLACK, 8, 11, 13);
        GameLogic g = new GameLogic(NINE, s);
        assertTrue(g.place(p(2)));
        assertEquals(GamePhase.REMOVING, g.state().phase());
        assertTrue(g.remove(p(8)));
        assertTrue(g.state().isFinished());
        assertEquals(Player.WHITE, g.state().winner());
    }

    @Test
    void blockedPlayerLoses() {
        GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
        setHand(s, Player.WHITE, 0); setHand(s, Player.BLACK, 0);
        put(s, Player.WHITE, 1, 3, 5, 7, 16);
        put(s, Player.BLACK, 0, 2, 6);
        s.setPhase(GamePhase.MOVING);
        GameLogic g = new GameLogic(NINE, s);
        assertTrue(g.move(p(16), p(17)));
        assertTrue(g.state().isFinished());
        assertEquals(Player.WHITE, g.state().winner());
    }

    @Test
    void undoRevertsLastTurnOnlyWhenEnabled() {
        GameLogic g = new GameLogic(NINE);
        assertFalse(g.canUndo());
        g.place(p(0));
        g.place(p(8));
        assertTrue(g.undo());
        assertEquals(Player.BLACK, g.state().currentPlayer());
        assertTrue(g.state().board().isEmpty(p(8)));
        assertEquals(9, g.state().handSize(Player.BLACK));
        assertFalse(g.state().board().isEmpty(p(0)));

        GameConfig noUndo = new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.PLAYER, AIDifficulty.EASY,
                EnumSet.noneOf(HouseRule.class));
        GameLogic h = new GameLogic(noUndo);
        h.place(p(0));
        assertFalse(h.undo());
    }

    @Test
    void undoDuringRemovalRevertsTheWholeTurn() {
        GameLogic g = new GameLogic(NINE);
        g.place(p(0)); g.place(p(8)); g.place(p(1)); g.place(p(9)); g.place(p(2));
        assertEquals(GamePhase.REMOVING, g.state().phase());
        assertTrue(g.undo());
        assertEquals(GamePhase.PLACING, g.state().phase());
        assertEquals(Player.WHITE, g.state().currentPlayer());
        assertTrue(g.state().board().isEmpty(p(2)));
    }

    @Test
    void undoAgainstComputerReturnsToTheHumansTurn() {
        GameConfig vsAi = new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.AI, AIDifficulty.EASY,
                EnumSet.of(HouseRule.UNDO_ENABLED));
        GameLogic g = new GameLogic(vsAi);
        g.place(p(0));   // human
        g.place(p(8));   // computer
        g.place(p(1));   // human
        g.place(p(9));   // computer
        assertTrue(g.undo());
        assertEquals(Player.WHITE, g.state().currentPlayer());
        assertTrue(g.state().board().isEmpty(p(1)));
        assertFalse(g.state().board().isEmpty(p(0)));
        assertFalse(g.state().board().isEmpty(p(8)));
    }
}
