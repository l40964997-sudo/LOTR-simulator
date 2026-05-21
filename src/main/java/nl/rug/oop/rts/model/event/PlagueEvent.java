package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

/**
 * A plague that drains a percentage of every unit's health.
 * <p>
 * Provided as a fourth event type to enrich the simulation beyond the
 * mandatory three.
 */
public class PlagueEvent extends GameEvent {

    /** Percentage (0-100) of current health drained from each unit. */
    private final int drainPercent;

    /** Default drain percentage. */
    public static final int DEFAULT_DRAIN = 20;

    /** Default constructor - 20% drain. */
    public PlagueEvent() {
        this(DEFAULT_DRAIN);
    }

    /**
     * Constructs the event with a custom drain percentage.
     *
     * @param drainPercent the percent of health to remove; clamped to [0, 100]
     */
    public PlagueEvent(int drainPercent) {
        super("Plague",
                "Disease sweeps through the army, weakening every unit.");
        this.drainPercent = Math.max(0, Math.min(100, drainPercent));
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Plague: no army to afflict.";
        }
        for (Unit u : army.getUnits()) {
            int loss = (u.getHealth() * drainPercent) / 100;
            u.takeDamage(loss);
        }
        army.removeDead();
        return "Plague: " + army.getName() + " lost " + drainPercent + "% health per unit.";
    }
}
