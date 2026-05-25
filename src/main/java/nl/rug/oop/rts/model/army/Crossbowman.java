package nl.rug.oop.rts.model.army;

/**
 * A heavier {@link Archer} variant whose bolts pierce armour.
 * <p>
 * The unit inherits the opening-round volley bonus from its parent but
 * carries enough kinetic punch that defense values are partially ignored
 * during that volley. After the first round it fights like a regular
 * crossbowman; only the volley is exceptional.
 */
public class Crossbowman extends Archer {

    /** Amount the unit's volley damage is boosted on top of the parent's multiplier. */
    private static final int PIERCE_BONUS = 4;

    /** Whether the unit still has its piercing volley available. */
    private boolean pierceAvailable;

    /**
     * Constructs a crossbowman with its piercing volley ready.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction
     * @param evasion dodge chance in [0, 1]
     */
    public Crossbowman(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
        this.pierceAvailable = true;
    }

    @Override
    public int computeDamage() {
        int base = super.computeDamage();
        if (pierceAvailable) {
            pierceAvailable = false;
            return base + PIERCE_BONUS;
        }
        return base;
    }

    @Override
    public void resetForBattle() {
        super.resetForBattle();
        pierceAvailable = true;
    }

    @Override
    public String getAbilityDescription() {
        return "Piercing Volley: opening volley adds bonus damage that bypasses armour.";
    }
}
