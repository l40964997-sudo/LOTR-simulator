package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

/**
 * A short training session: every unit gains a small strength and health
 * bump.
 * <p>
 * Combines the offensive boost of {@link HiddenWeaponryEvent} with the
 * healing of {@link SupplyCacheEvent}, but in smaller doses, so it's a
 * good "balanced" event for testing combat scenarios.
 */
public class TrainingDrillEvent extends GameEvent {

    /** Default strength bonus. */
    public static final int DEFAULT_STRENGTH = 3;

    /** Default health bonus. */
    public static final int DEFAULT_HEALTH = 10;

    /** Strength bonus applied to each unit. */
    private final int strengthBonus;

    /** Health bonus applied to each unit. */
    private final int healthBonus;

    /**
     * Constructs the event with default bonuses.
     */
    public TrainingDrillEvent() {
        this(DEFAULT_STRENGTH, DEFAULT_HEALTH);
    }

    /**
     * Constructs the event with custom bonuses.
     *
     * @param strengthBonus per-unit strength gain
     * @param healthBonus per-unit hit point gain
     */
    public TrainingDrillEvent(int strengthBonus, int healthBonus) {
        super("Training Drill",
                "Every unit gains +" + Math.max(0, strengthBonus) + " strength and +"
                        + Math.max(0, healthBonus) + " HP permanently.");
        this.strengthBonus = Math.max(0, strengthBonus);
        this.healthBonus = Math.max(0, healthBonus);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Training Drill: no army to train.";
        }
        for (Unit u : army.getUnits()) {
            u.setStrength(u.getStrength() + strengthBonus);
            u.heal(healthBonus);
        }
        return "Training Drill: " + army.getName() + " - every unit gained +"
                + strengthBonus + " STR and +" + healthBonus + " HP.";
    }
}
