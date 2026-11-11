package morris.model;

/**
 * A node (intersection) of the board, identified by its index in the board
 * topology.
 * A record, so equals()/hashCode() are generated and it is safe as a HashMap
 * key.
 */
public record Position(int id) {
}
