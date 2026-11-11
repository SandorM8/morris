package morris.service;

import morris.model.GameConfig;
import morris.model.GameState;

/** Everything stored in a save file. The undo history is deliberately not saved. */
public record SaveData(GameConfig config, GameState state) {
}
