package nl.rug.oop.rts.model.army;

/**
 * A monstrous Mordor {@link Heavy} whose swings hit multiple foes.
 * <p>
 * Trolls already inherit the heavy armour absorption from {@link Heavy};
 * on top of that, every round they deal an extra splash bonus to model the
 * area damage of a sweeping club. The bonus persists for every round (not
 * only the first), but it is modest enough that a single troll cannot
 * single-handedly demolish a balanced army.
 */
public class Troll extends Heavy {

    /** Bonus damage added on every round to represent splash. */
    private static final int SPLASH_BONUS = 6;

    /**
     * Constructs a troll.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction per hit
     * @param evasion dodge chance in [0, 1]
     */
    public Troll(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
    }

    @Override
    public int computeDamage() {
        return super.computeDamage() + SPLASH_BONUS;
    }

    @Override
    public String getAbilityDescription() {
        return "Splash Smash: every swing deals bonus damage to the enemy line.";
    }
}
