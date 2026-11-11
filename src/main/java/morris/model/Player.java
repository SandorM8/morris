package morris.model;

/** The two sides of the game. WHITE always moves first. */
public enum Player {
    WHITE, BLACK;

    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
