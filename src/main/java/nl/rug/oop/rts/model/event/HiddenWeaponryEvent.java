package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

/**
 * Boosts the strength of every unit in the encountering army.
 */
public class HiddenWeaponryEvent extends GameEvent {

    /** Flat damage bonus granted to each unit. */
    private final int strengthBonus;

    /** Default bonus. */
    public static final int DEFAULT_BONUS = 5;

    /** Default constructor with a {@value #DEFAULT_BONUS} bonus. */
    public HiddenWeaponryEvent() {
        this(DEFAULT_BONUS);
    }

    /**
     * Constructs the event with a custom bonus.
     *
     * @param strengthBonus the per-unit strength bonus; clamped to non-negative
     */
    public HiddenWeaponryEvent(int strengthBonus) {
        super("Hidden Weaponry",
                "A weapons cache is found, giving every unit increased damage.");
        this.strengthBonus = Math.max(0, strengthBonus);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Hidden Weaponry: no army to arm.";
        }
        for (Unit u : army.getUnits()) {
            u.setStrength(u.getStrength() + strengthBonus);
        }
        return "Hidden Weaponry: " + army.getName()
                + " gained +" + strengthBonus + " strength per unit.";
    }
}
