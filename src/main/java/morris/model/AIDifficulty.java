package morris.model;

public enum AIDifficulty {
    EASY, MEDIUM, HARD;

    @Override
    public String toString() {
        String s = name();
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
