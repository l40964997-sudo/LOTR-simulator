package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

/**
 * Heals every unit in the army by a flat amount.
 * <p>
 * The opposite of {@link AmbushEvent}: a supply cache, healer's spring,
 * or Elven waybread. Useful when the user wants a clear "we got better"
 * event without rolling stats.
 */
public class SupplyCacheEvent extends GameEvent {

    /** Default heal amount. */
    public static final int DEFAULT_HEAL = 25;

    /** Hit points restored to each unit. */
    private final int healAmount;

    /**
     * Constructs the event with the default heal amount.
     */
    public SupplyCacheEvent() {
        this(DEFAULT_HEAL);
    }

    /**
     * Constructs the event with a specific heal amount.
     *
     * @param healAmount how many hit points each unit recovers
     */
    public SupplyCacheEvent(int healAmount) {
        super("Supply Cache",
                "Heals every unit for " + Math.max(0, healAmount) + " HP.");
        this.healAmount = Math.max(0, healAmount);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Supply Cache: no army to resupply.";
        }
        for (Unit u : army.getUnits()) {
            u.heal(healAmount);
        }
        return "Supply Cache: " + army.getName() + " - every unit recovered "
                + healAmount + " HP (army total now " + army.totalHealth() + ").";
    }
}
