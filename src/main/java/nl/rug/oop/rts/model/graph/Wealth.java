package nl.rug.oop.rts.model.graph;

/**
 * Coarse wealth classification for a settlement.
 * <p>
 * Using an enum (rather than a free-form string or a raw int) keeps the UI
 * picker simple, prevents typos and gives the value a place where future
 * gameplay code can hang behaviour (e.g. wealthy settlements giving bonus
 * gold once an economy system is added).
 */
public enum Wealth {

    /** Poor settlement: subsistence farming, no real coinage. */
    POOR("Poor"),

    /** Modest settlement: small market, basic trade. */
    MODEST("Modest"),

    /** Comfortable settlement: regular trade routes and skilled artisans. */
    COMFORTABLE("Comfortable"),

    /** Wealthy settlement: substantial treasury and rich trade. */
    WEALTHY("Wealthy"),

    /** Opulent settlement: capital-grade wealth and prestige. */
    OPULENT("Opulent");

    /** Human readable display name shown in the UI. */
    private final String displayName;

    /**
     * Constructs a wealth constant.
     *
     * @param displayName the user-visible label
     */
    Wealth(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user-visible label for this wealth tier.
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

    /**
     * Looks up a wealth tier by its user-visible display name.
     *
     * @param displayName the display name; may be {@code null}
     * @return the matching tier, or {@code null} when not found
     */
    public static Wealth fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (Wealth w : values()) {
            if (w.displayName.equalsIgnoreCase(displayName)) {
                return w;
            }
        }
        return null;
    }
}
