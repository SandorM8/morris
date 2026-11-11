package morris.service;

import morris.logic.BoardTopology;
import morris.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Saves and loads games as human-readable JSON (hand-assembled, no external library).
 * The "row"/"col" of each board entry are grid coordinates of the node (0-6 for Nine
 * Men's Morris, 0-2 for Three Men's Morris).
 */
public class JsonGameManager {

    public void save(Path file, GameConfig config, GameState state) throws IOException {
        Files.writeString(file, toJson(config, state), StandardCharsets.UTF_8);
    }

    public SaveData load(Path file) throws IOException {
        return fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    public String toJson(GameConfig config, GameState state) {
        BoardTopology topo = state.board().topology();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        field(sb, "gameMode", config.mode().name());
        field(sb, "opponentType", config.opponentType().name());
        field(sb, "aiDifficulty", config.aiDifficulty().name());
        field(sb, "currentTurn", state.currentPlayer().name());
        field(sb, "gamePhase", state.phase().name());
        sb.append("  \"winner\": ")
                .append(state.winner() == null ? "null" : MiniJson.quote(state.winner().name())).append(",\n");
        sb.append("  \"board\": [");
        boolean first = true;
        for (Position p : topo.positions()) {
            Piece piece = state.board().getPiece(p);
            if (piece == null) continue;
            sb.append(first ? "\n" : ",\n");
            first = false;
            sb.append("    {\"row\": ").append(topo.row(p)).append(", \"col\": ").append(topo.col(p))
                    .append(", \"player\": ").append(MiniJson.quote(piece.owner().name())).append("}");
        }
        sb.append(first ? "],\n" : "\n  ],\n");
        sb.append("  \"whiteHandCount\": ").append(state.handSize(Player.WHITE)).append(",\n");
        sb.append("  \"blackHandCount\": ").append(state.handSize(Player.BLACK)).append(",\n");
        sb.append("  \"activeHouseRules\": [");
        boolean firstRule = true;
        for (HouseRule r : config.houseRules()) {
            sb.append(firstRule ? "\n    " : ",\n    ").append(MiniJson.quote(r.name()));
            firstRule = false;
        }
        sb.append(firstRule ? "]\n" : "\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public SaveData fromJson(String json) throws GameFormatException {
        try {
            Object root = MiniJson.parse(json);
            if (!(root instanceof Map<?, ?> m)) {
                throw new GameFormatException("The file does not contain a JSON object");
            }
            GameMode mode = GameMode.valueOf(str(m, "gameMode"));
            OpponentType opponent = OpponentType.valueOf(str(m, "opponentType"));
            AIDifficulty difficulty = m.get("aiDifficulty") == null
                    ? AIDifficulty.MEDIUM : AIDifficulty.valueOf(str(m, "aiDifficulty"));
            Set<HouseRule> rules = EnumSet.noneOf(HouseRule.class);
            for (Object o : list(m, "activeHouseRules")) {
                rules.add(HouseRule.valueOf((String) o));
            }
            GameConfig config = new GameConfig(mode, opponent, difficulty, rules);

            GameState state = new GameState(mode);
            state.setCurrentPlayer(Player.valueOf(str(m, "currentTurn")));
            state.setPhase(GamePhase.valueOf(str(m, "gamePhase")));
            if (m.get("winner") != null) {
                state.setWinner(Player.valueOf(str(m, "winner")));
            }
            setHand(state, Player.WHITE, intValue(m, "whiteHandCount"), mode);
            setHand(state, Player.BLACK, intValue(m, "blackHandCount"), mode);

            BoardTopology topo = BoardTopology.of(mode);
            for (Object o : list(m, "board")) {
                if (!(o instanceof Map<?, ?> e)) throw new GameFormatException("Bad board entry");
                Position p = topo.at(intValue(e, "row"), intValue(e, "col"));
                if (p == null) throw new GameFormatException("No board node at that row/col for " + mode);
                if (!state.board().isEmpty(p)) throw new GameFormatException("Two pieces on the same node");
                state.board().placePiece(p, new Piece(Player.valueOf(str(e, "player"))));
            }
            validate(state, mode);
            return new SaveData(config, state);
        } catch (GameFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new GameFormatException("Invalid save file: " + e.getMessage(), e);
        }
    }

    private static void validate(GameState s, GameMode mode) throws GameFormatException {
        for (Player p : Player.values()) {
            if (s.totalPieces(p) > mode.piecesPerPlayer()) {
                throw new GameFormatException("Too many " + p + " pieces for " + mode);
            }
        }
        if (s.phase() == GamePhase.FINISHED && s.winner() == null) {
            throw new GameFormatException("A finished game needs a winner");
        }
        if (s.phase() == GamePhase.REMOVING && !mode.capturesOnMill()) {
            throw new GameFormatException("REMOVING is not a valid phase in " + mode);
        }
        if (s.phase() == GamePhase.PLACING && s.handSize(s.currentPlayer()) == 0) {
            throw new GameFormatException("PLACING phase but the current player has no pieces in hand");
        }
        if ((s.phase() == GamePhase.MOVING || s.phase() == GamePhase.FLYING)
                && s.handSize(s.currentPlayer()) > 0) {
            throw new GameFormatException("Moving phase but the current player still has pieces in hand");
        }
    }

    private static void setHand(GameState s, Player p, int count, GameMode mode) throws GameFormatException {
        if (count < 0 || count > mode.piecesPerPlayer()) {
            throw new GameFormatException("Invalid hand count for " + p);
        }
        s.hand(p).clear();
        for (int i = 0; i < count; i++) s.hand(p).add(new Piece(p));
    }

    private static void field(StringBuilder sb, String key, String value) {
        sb.append("  ").append(MiniJson.quote(key)).append(": ").append(MiniJson.quote(value)).append(",\n");
    }

    private static String str(Map<?, ?> m, String key) throws GameFormatException {
        Object v = m.get(key);
        if (!(v instanceof String s)) throw new GameFormatException("Missing or invalid field: " + key);
        return s;
    }

    private static int intValue(Map<?, ?> m, String key) throws GameFormatException {
        Object v = m.get(key);
        if (!(v instanceof Double d) || d != Math.rint(d)) {
            throw new GameFormatException("Missing or invalid number: " + key);
        }
        return d.intValue();
    }

    private static List<?> list(Map<?, ?> m, String key) throws GameFormatException {
        Object v = m.get(key);
        if (!(v instanceof List<?> l)) throw new GameFormatException("Missing or invalid list: " + key);
        return l;
    }
}
