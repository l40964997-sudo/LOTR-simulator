package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.UnitFactory;

/**
 * Adds extra units to the encountering army.
 * <p>
 * Concrete event that demonstrates the Strategy pattern: the simulator
 * does not care how reinforcements arrive, only that
 * {@link #applyTo(Army)} happens.
 */
public class ReinforcementsEvent extends GameEvent {

    /** Number of units added when the event fires. */
    private final int amount;

    /** The factory used to spawn reinforcements. */
    private final UnitFactory unitFactory;

    /** Default reinforcement size. */
    public static final int DEFAULT_AMOUNT = 5;

    /**
     * Constructs the event with the default reinforcement size.
     */
    public ReinforcementsEvent() {
        this(DEFAULT_AMOUNT);
    }

    /**
     * Constructs the event with a specific reinforcement size.
     *
     * @param amount the number of units to spawn; clamped to non-negative
     */
    public ReinforcementsEvent(int amount) {
        super("Reinforcements",
                "Adds " + Math.max(0, amount) + " fresh units of the army's own faction.");
        this.amount = Math.max(0, amount);
        this.unitFactory = new UnitFactory();
    }

    @Override
    public String applyTo(Army army) {
        if (army == null) {
            return "Reinforcements: no army present.";
        }
        int before = army.size();
        for (int i = 0; i < amount; i++) {
            army.addUnit(unitFactory.createRandomUnit(army.getFaction()));
        }
        return "Reinforcements: " + army.getName() + " grew from " + before + " to "
                + army.size() + " units (+" + amount + ").";
    }
}
