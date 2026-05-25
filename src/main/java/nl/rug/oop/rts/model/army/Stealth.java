package nl.rug.oop.rts.model.army;

/**
 * A nimble {@link Unit} that relies on evasion rather than armour.
 * <p>
 * Hobbits, scouts and other small units that dodge instead of standing
 * their ground use this subclass. The base class already implements
 * evasion rolls inside {@link Unit#takeDamage(int)}; the subclass exists
 * to make the role explicit in the unit detail dialog and to reserve a
 * polymorphic seam for future tweaks.
 */
public class Stealth extends Unit {

    /**
     * Constructs a stealthy unit with elevated evasion.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction
     * @param evasion dodge chance in [0, 1]; typical values are 0.25 - 0.45
     */
    public Stealth(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
    }

    @Override
    public String getAbilityDescription() {
        return "Stealth: high chance to dodge incoming attacks entirely.";
    }
}
