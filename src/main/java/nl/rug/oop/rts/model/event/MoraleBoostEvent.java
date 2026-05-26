package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;

/**
 * Raises the army's morale by a fixed percentage.
 * <p>
 * Morale is a multiplier applied to combat power, so this is a strict
 * positive event: same units, higher effective output. Capped by the
 * {@link Army#setMorale(double)} clamp.
 */
public class MoraleBoostEvent extends GameEvent {

    /** Default morale bonus (0.20 = +20 percentage points). */
    public static final double DEFAULT_BOOST = 0.20;

    /** Morale delta applied when the event fires. */
    private final double boost;

    /**
     * Constructs the event with the default boost.
     */
    public MoraleBoostEvent() {
        this(DEFAULT_BOOST);
    }

    /**
     * Constructs the event with a specific boost.
     *
     * @param boost the delta added to the army's morale (clamped at the army)
     */
    public MoraleBoostEvent(double boost) {
        super("Morale Boost",
                "Raises the army's morale multiplier by "
                        + String.format("%.0f%%", Math.max(0, boost) * 100) + ".");
        this.boost = Math.max(0, boost);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null) {
            return "Morale Boost: no army to inspire.";
        }
        double before = army.getMorale();
        army.setMorale(before + boost);
        return String.format("Morale Boost: %s - morale rose from %.0f%% to %.0f%%.",
                army.getName(), before * 100, army.getMorale() * 100);
    }
}
