package morris.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The settings locked in when a game starts (Advanced Setup). Immutable.
 * The AI always plays BLACK; the human plays WHITE.
 */
public record GameConfig(GameMode mode, OpponentType opponentType, AIDifficulty aiDifficulty,
                         Set<HouseRule> houseRules) {

    public GameConfig {
        if (mode == null || opponentType == null || aiDifficulty == null || houseRules == null) {
            throw new IllegalArgumentException("config values must not be null");
        }
        EnumSet<HouseRule> rules = houseRules.isEmpty() ? EnumSet.noneOf(HouseRule.class) : EnumSet.copyOf(houseRules);
        if (mode == GameMode.THREE_MENS_MORRIS) {
            rules.remove(HouseRule.FLYING_ALLOWED); // nobody is ever reduced in 3MM
        }
        houseRules = Collections.unmodifiableSet(rules);
    }

    public static GameConfig defaults() {
        return new GameConfig(GameMode.NINE_MENS_MORRIS, OpponentType.PLAYER, AIDifficulty.MEDIUM,
                EnumSet.of(HouseRule.UNDO_ENABLED));
    }

    public boolean has(HouseRule rule) {
        return houseRules.contains(rule);
    }

    public boolean vsComputer() {
        return opponentType == OpponentType.AI;
    }

    /** The side the computer plays (only meaningful when vsComputer()). */
    public Player aiPlayer() {
        return Player.BLACK;
    }
}
