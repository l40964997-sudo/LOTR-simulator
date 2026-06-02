package nl.rug.oop.rts.model.graph;

/**
 * Terrain type of a route between two settlements.
 * <p>
 * Each {@link Edge} carries one terrain value: water for a river or sea
 * crossing, wood for forested paths, mountain for high passes, caves for
 * underground tunnels, and desert for arid stretches. The enum gives the
 * UI a fixed picker, prevents typos, and gives future pathing /
 * simulation logic a place to hang per-terrain rules (e.g. cavalry
 * penalty in mountains) without churning the model again.
 */
public enum EdgeType {

    /** River, lake or sea route. */
    WATER("Water"),

    /** Forested or wooded path. */
    WOOD("Wood"),

    /** High mountain pass or rocky climb. */
    MOUNTAIN("Mountain"),

    /** Underground tunnel or cave network. */
    CAVES("Caves"),

    /** Arid stretch of open desert. */
    DESERT("Desert");

    /** User-visible label for the picker. */
    private final String displayName;

    /**
     * Constructs an edge-type constant.
     *
     * @param displayName the user-visible label
     */
    EdgeType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user-visible label for this terrain type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
