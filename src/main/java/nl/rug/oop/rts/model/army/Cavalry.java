package nl.rug.oop.rts.model.army;

/**
 * Mounted {@link Unit} that strikes hard on the charge but tires quickly.
 * <p>
 * The opening round of every battle, the cavalryman deals double damage to
 * model the impact of a thundering charge. Subsequent rounds revert to a
 * standard sword-and-shield melee, so an army of nothing but cavalry will
 * deal a punishing first round but is balanced by enemies that survive it.
 */
public class Cavalry extends Unit {

    /** Multiplier applied to the charge round damage. */
    private static final double CHARGE_MULTIPLIER = 2.0;

    /** Whether the unit still has its charge bonus this battle. */
    private boolean chargeAvailable;

    /**
     * Constructs a cavalry unit with its charge ready.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction
     * @param evasion dodge chance in [0, 1]
     */
    public Cavalry(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
        this.chargeAvailable = true;
    }

    @Override
    public int computeDamage() {
        if (chargeAvailable) {
            chargeAvailable = false;
            return (int) Math.round(super.computeDamage() * CHARGE_MULTIPLIER);
        }
        return super.computeDamage();
    }

    @Override
    public void resetForBattle() {
        chargeAvailable = true;
    }

    @Override
    public String getAbilityDescription() {
        return "Charge: double damage on the opening round of every battle.";
    }
}
