package morris.model;

public enum OpponentType {
    PLAYER("Player vs. Player"),
    AI("Player vs. Computer");

    private final String label;

    OpponentType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
