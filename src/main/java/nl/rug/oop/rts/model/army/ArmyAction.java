package nl.rug.oop.rts.model.army;

/**
 * The discrete actions an army may carry out at the start of a simulation
 * turn, before pathing and combat resolve.
 * <p>
 * Each constant describes the rule it implements in its Javadoc; the
 * actual side effects are applied by {@link ActionExecutor}, which keeps
 * the enum free of behaviour-heavy code (high cohesion, low coupling).
 * Actions are presented to the user when a player-controlled army is
 * waiting for orders, and rolled randomly for AI armies.
 */
public enum ArmyAction {

    /** Stand fast: gain a temporary damage shield for the next battle. */
    FORTIFY("Fortify", "Brace for impact: absorb 30% of damage in the next battle."),

    /** Inspire the troops: morale ticks up. */
    RALLY("Rally", "Banners raised, morale climbs by 10%."),

    /** Field medics patch up the wounded: heal each unit. */
    REGROUP("Regroup", "Field medics tend the wounded, healing every unit."),

    /** Quick double-move: the army still moves this turn but more aggressively. */
    FORCED_MARCH("Forced March", "Push the men hard: the army covers ground faster (and tires)."),

    /** Stay put: no movement this turn but small morale bonus. */
    HOLD("Hold the Line", "Hold position this turn; the army does not advance."),

    /** Skirmish: take a small immediate toll on enemies at the destination. */
    SKIRMISH("Skirmish", "Pre-emptive harassment: enemies at the destination take light damage."),

    /** Raid: extra damage in the next battle at the cost of morale. */
    RAID("Raid", "Reckless raid: +20% damage but morale drops next turn.");

    /** Display name shown in the UI. */
    private final String displayName;
    /** One-line description shown in tooltips and prompts. */
    private final String description;

    /**
     * Constructs an action constant.
     *
     * @param displayName the user-facing name
     * @param description a short user-facing description
     */
    ArmyAction(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human readable name of the action.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a one-line summary of what the action does.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
