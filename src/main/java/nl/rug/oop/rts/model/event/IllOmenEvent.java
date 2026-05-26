package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;

/**
 * Lowers the army's morale by a fixed percentage.
 * <p>
 * Counterpart to {@link MoraleBoostEvent}: same units, lower effective
 * combat power. A sufficiently bad omen can drop morale far enough that
 * the army routs mid-battle (see
 * {@link nl.rug.oop.rts.model.battle.ComplexBattleStrategy}).
 */
public class IllOmenEvent extends GameEvent {

    /** Default morale penalty (0.20 = -20 percentage points). */
    public static final double DEFAULT_PENALTY = 0.20;

    /** Morale delta subtracted when the event fires. */
    private final double penalty;

    /**
     * Constructs the event with the default penalty.
     */
    public IllOmenEvent() {
        this(DEFAULT_PENALTY);
    }

    /**
     * Constructs the event with a specific penalty.
     *
     * @param penalty the delta subtracted from morale (clamped at the army)
     */
    public IllOmenEvent(double penalty) {
        super("Ill Omen",
                "Drops the army's morale multiplier by "
                        + String.format("%.0f%%", Math.max(0, penalty) * 100) + ".");
        this.penalty = Math.max(0, penalty);
    }

    @Override
    public String applyTo(Army army) {
        if (army == null) {
            return "Ill Omen: no army to dishearten.";
        }
        double before = army.getMorale();
        army.setMorale(before - penalty);
        return String.format("Ill Omen: %s - morale dropped from %.0f%% to %.0f%%.",
                army.getName(), before * 100, army.getMorale() * 100);
    }
}
