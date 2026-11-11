package morris.model;

public enum GameMode {
    /** 3x3 board, 3 pieces each, no captures: the first mill wins. */
    THREE_MENS_MORRIS("Three Men's Morris", 3),
    /** Classic board of three nested squares, 9 pieces each, mills capture. */
    NINE_MENS_MORRIS("Nine Men's Morris", 9);

    private final String displayName;
    private final int piecesPerPlayer;

    GameMode(String displayName, int piecesPerPlayer) {
        this.displayName = displayName;
        this.piecesPerPlayer = piecesPerPlayer;
    }

    public int piecesPerPlayer() {
        return piecesPerPlayer;
    }

    /** A mill captures a piece only in Nine Men's Morris. */
    public boolean capturesOnMill() {
        return this == NINE_MENS_MORRIS;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
