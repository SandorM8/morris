package morris.logic;

import morris.model.*;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AIPlayerTest {

    private static GameConfig config(GameMode mode) {
        return new GameConfig(mode, OpponentType.AI, AIDifficulty.MEDIUM, EnumSet.noneOf(HouseRule.class));
    }

    @Test
    void everyDifficultyReturnsALegalTurn() {
        for (AIDifficulty d : AIDifficulty.values()) {
            for (GameMode m : GameMode.values()) {
                GameLogic g = new GameLogic(config(m));
                Turn t = new AIPlayer(d, new Random(1)).chooseTurn(g);
                assertNotNull(t);
                assertTrue(t.applyTo(g.copy()), d + " " + m);
            }
        }
    }

    @Test
    void mediumAndHardTakeAMillWhenOneIsAvailable() {
        for (AIDifficulty d : new AIDifficulty[]{AIDifficulty.MEDIUM, AIDifficulty.HARD}) {
            GameState s = new GameState(GameMode.NINE_MENS_MORRIS);
            s.board().placePiece(new Position(0), new Piece(Player.WHITE));
            s.board().placePiece(new Position(1), new Piece(Player.WHITE));
            while (s.handSize(Player.WHITE) > 1) s.hand(Player.WHITE).remove(0); // last piece: no fork possible
            s.board().placePiece(new Position(16), new Piece(Player.BLACK));
            s.board().placePiece(new Position(20), new Piece(Player.BLACK));
            s.hand(Player.BLACK).remove(0);
            s.hand(Player.BLACK).remove(0);
            GameLogic g = new GameLogic(config(GameMode.NINE_MENS_MORRIS), s);
            Turn t = new AIPlayer(d, new Random(3)).chooseTurn(g);
            assertEquals(new Position(2), t.to(), d.toString());
            assertNotNull(t.remove());
        }
    }

    @Test
    void hardBlocksAnImmediateThreeMensMorrisWin() {
        // Black threatens 3-4-5 (row 1). White to move must occupy a threatened node... white has 0 and 8.
        GameState s = new GameState(GameMode.THREE_MENS_MORRIS);
        s.board().placePiece(new Position(0), new Piece(Player.WHITE));
        s.board().placePiece(new Position(8), new Piece(Player.WHITE));
        s.board().placePiece(new Position(3), new Piece(Player.BLACK));
        s.board().placePiece(new Position(4), new Piece(Player.BLACK));
        for (int i = 0; i < 2; i++) { s.hand(Player.WHITE).remove(0); s.hand(Player.BLACK).remove(0); }
        GameLogic g = new GameLogic(config(GameMode.THREE_MENS_MORRIS), s);
        Turn t = new AIPlayer(AIDifficulty.HARD, new Random(5)).chooseTurn(g);
        assertEquals(new Position(5), t.to(), "the only way to stop 3-4-5");
    }

    @Test
    void computerVsComputerGamesTerminateCleanly() {
        for (GameMode m : GameMode.values()) {
            GameLogic g = new GameLogic(new GameConfig(m, OpponentType.PLAYER, AIDifficulty.EASY,
                    EnumSet.of(HouseRule.FLYING_ALLOWED)));
            AIPlayer easy = new AIPlayer(AIDifficulty.EASY, new Random(42));
            AIPlayer medium = new AIPlayer(AIDifficulty.MEDIUM, new Random(43));
            for (int i = 0; i < 300 && !g.state().isFinished(); i++) {
                AIPlayer ai = g.state().currentPlayer() == Player.WHITE ? easy : medium;
                Turn t = ai.chooseTurn(g);
                assertNotNull(t);
                assertTrue(t.applyTo(g));
            }
        }
    }

    @Test
    void hardOpeningMoveIsFastEnough() {
        GameLogic g = new GameLogic(config(GameMode.NINE_MENS_MORRIS));
        long start = System.nanoTime();
        new AIPlayer(AIDifficulty.HARD, new Random(1)).chooseTurn(g);
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Hard AI opening move: " + ms + " ms");
        assertTrue(ms < 15_000);
    }
}
