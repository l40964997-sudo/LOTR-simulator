package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Removes a number of units from the encountering army to simulate a
 * destructive natural event (avalanche, flood, etc.).
 */
public class NaturalDisasterEvent extends GameEvent {

    /** Maximum number of casualties this event will cause. */
    private final int maxCasualties;

    /** Random source for picking the victims. */
    private final Random random;

    /** Default upper bound on casualties. */
    public static final int DEFAULT_MAX_CASUALTIES = 8;

    /**
     * Constructs the event with the default casualty cap.
     */
    public NaturalDisasterEvent() {
        this(DEFAULT_MAX_CASUALTIES);
    }

    /**
     * Constructs the event with a specific casualty cap.
     *
     * @param maxCasualties the upper bound on casualties; clamped to non-negative
     */
    public NaturalDisasterEvent(int maxCasualties) {
        super("Natural Disaster",
                "A catastrophe strikes and removes some units from the army.");
        this.maxCasualties = Math.max(0, maxCasualties);
        this.random = new Random();
    }

    @Override
    public String applyTo(Army army) {
        if (army == null || army.size() == 0) {
            return "Natural Disaster: no army to harm.";
        }
        int casualties = Math.min(army.size(),
                1 + random.nextInt(Math.max(1, maxCasualties)));
        List<Unit> victims = new ArrayList<>();
        // Snapshot to avoid concurrent modification.
        List<Unit> remaining = new ArrayList<>(army.getUnits());
        for (int i = 0; i < casualties && !remaining.isEmpty(); i++) {
            int idx = random.nextInt(remaining.size());
            victims.add(remaining.remove(idx));
        }
        for (Unit victim : victims) {
            army.removeUnit(victim);
        }
        return "Natural Disaster: " + army.getName()
                + " lost " + victims.size() + " units.";
    }
}
