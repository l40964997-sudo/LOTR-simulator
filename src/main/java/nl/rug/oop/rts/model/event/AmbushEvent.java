package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

/**
 * A surprise ambush: every unit takes a flat amount of damage.
 * <p>
 * Unlike {@link PlagueEvent}, which scales with each unit's current
 * health, an ambush deals a fixed damage figure - useful for events
 * that should hit small/elite armies harder in relative terms.
 */
public class AmbushEvent extends GameEvent {

    /** Default flat damage figure. */
    public static final int DEFAULT_DAMAGE = 15;

    /** Damage applied to each unit when the event fires. */
    private final int damage;

    /**
     * Constructs the event with the default damage value.
     */
    public AmbushEvent() {
        this(DEFAULT_DAMAGE);
    }

    /**
     * Constructs the event with a specific damage value.
     *
     * @param damage flat damage applied to each unit; clamped to non-negative
     */
    public AmbushEvent(int damage) {
        super("Ambush",
                "A surprise attack hits every unit for " + Math.max(0, damage)
                + " damage (armour and evasion still apply).");
        this.damage = Math.max(0, damage);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Ambush: no army to ambush.";
        }
        int before = army.size();
        for (Unit u : army.getUnits()) {
            u.takeDamage(damage);
        }
        army.removeDead();
        int killed = before - army.size();
        return "Ambush: " + army.getName() + " - every unit took " + damage
                + " damage" + (killed > 0 ? " (" + killed + " killed)." : ".");
    }
}
