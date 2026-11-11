package morris.service;

import morris.logic.GameLogic;
import morris.model.*;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class JsonGameManagerTest {
    private final JsonGameManager json = new JsonGameManager();

    @Test
    void roundTripPreservesTheGame() throws Exception {
        GameConfig config = new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.AI, AIDifficulty.HARD,
                EnumSet.of(HouseRule.UNDO_ENABLED, HouseRule.FLYING_ALLOWED));
        GameLogic g = new GameLogic(config);
        g.place(new Position(0)); g.place(new Position(8)); g.place(new Position(1));
        g.place(new Position(9)); g.place(new Position(2)); // mill -> REMOVING

        String text = json.toJson(config, g.state());
        SaveData back = json.fromJson(text);

        assertEquals(config, back.config());
        GameState a = g.state(), b = back.state();
        assertEquals(a.currentPlayer(), b.currentPlayer());
        assertEquals(a.phase(), b.phase());
        assertEquals(a.handSize(Player.WHITE), b.handSize(Player.WHITE));
        assertEquals(a.handSize(Player.BLACK), b.handSize(Player.BLACK));
        for (Position p : a.board().topology().positions()) {
            assertEquals(a.board().getPiece(p), b.board().getPiece(p));
        }
        assertEquals(text, json.toJson(back.config(), back.state()), "stable output");
    }

    @Test
    void savedGameIsHumanReadableAndUsesTheSpecFields() {
        GameConfig config = GameConfig.defaults();
        String text = json.toJson(config, new GameState(config.mode()));
        for (String key : new String[]{"gameMode", "opponentType", "aiDifficulty", "currentTurn", "gamePhase",
                "board", "whiteHandCount", "blackHandCount", "activeHouseRules"}) {
            assertTrue(text.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void loadsAHandWrittenFile() throws Exception {
        String text = """
                {
                  "gameMode": "NINE_MENS_MORRIS", "opponentType": "PLAYER", "aiDifficulty": "EASY",
                  "currentTurn": "WHITE", "gamePhase": "PLACING",
                  "board": [ {"row": 0, "col": 0, "player": "WHITE"}, {"row": 0, "col": 3, "player": "BLACK"} ],
                  "whiteHandCount": 8, "blackHandCount": 8,
                  "activeHouseRules": ["UNDO_ENABLED"]
                }""";
        SaveData d = json.fromJson(text);
        assertEquals(Player.WHITE, d.state().board().getPiece(new Position(0)).owner());
        assertEquals(Player.BLACK, d.state().board().getPiece(new Position(1)).owner());
        assertTrue(d.config().has(HouseRule.UNDO_ENABLED));
    }

    @Test
    void rejectsBrokenFiles() {
        assertThrows(GameFormatException.class, () -> json.fromJson("not json"));
        assertThrows(GameFormatException.class, () -> json.fromJson("[]"));
        assertThrows(GameFormatException.class, () -> json.fromJson("{\"gameMode\": \"CHESS\"}"));
        String offGrid = """
                {"gameMode":"NINE_MENS_MORRIS","opponentType":"PLAYER","aiDifficulty":"EASY",
                 "currentTurn":"WHITE","gamePhase":"PLACING",
                 "board":[{"row":1,"col":2,"player":"WHITE"}],
                 "whiteHandCount":8,"blackHandCount":9,"activeHouseRules":[]}""";
        assertThrows(GameFormatException.class, () -> json.fromJson(offGrid));
        String tooMany = offGrid.replace("\"row\":1,\"col\":2", "\"row\":0,\"col\":0").replace("\"whiteHandCount\":8", "\"whiteHandCount\":9");
        assertThrows(GameFormatException.class, () -> json.fromJson(tooMany));
    }
}
