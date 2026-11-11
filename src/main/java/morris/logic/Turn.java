package morris.logic;

import morris.model.Position;

/**
 * One complete turn: a placement (from == null) or a move/fly, plus the optional capture.
 */
public record Turn(Position from, Position to, Position remove) {

    /** Plays this turn on the given logic. Returns false if any part was illegal. */
    public boolean applyTo(GameLogic logic) {
        boolean ok = from == null ? logic.place(to) : logic.move(from, to);
        if (ok && remove != null) {
            ok = logic.remove(remove);
        }
        return ok;
    }
}
