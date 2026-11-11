package morris.model;

public enum HouseRule {
    UNDO_ENABLED("Allow undo"),
    /** A player with exactly three pieces left (and none in hand) may jump anywhere. Nine Men's Morris only. */
    FLYING_ALLOWED("Flying (at 3 pieces)");

    private final String label;

    HouseRule(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
