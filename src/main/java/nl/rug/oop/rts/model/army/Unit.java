package nl.rug.oop.rts.model.army;

import java.util.Objects;
import java.util.Random;

/**
 * A single combatant within an {@link Army}.
 * <p>
 * Each unit stores four core statistics:
 * <ul>
 *   <li>{@code name} - the unit type, e.g. "Gondor Soldier";</li>
 *   <li>{@code strength} - damage dealt per round of combat;</li>
 *   <li>{@code health} - the unit's hit points;</li>
 *   <li>{@code defense} - flat damage absorbed per hit (clamped);</li>
 *   <li>{@code evasion} - probability in [0, 1] to dodge a hit entirely.</li>
 * </ul>
 * The class is open for extension because the assignment encourages
 * specialised unit types: subclasses like {@link Archer}, {@code Cavalry}
 * or {@code Heavy} override {@link #computeDamage()} or
 * {@link #takeDamage(int)} to give each unit its own flavour.
 */
public class Unit {

    /** Shared random source for evasion rolls; deterministic enough for the UI. */
    private static final Random EVASION_RANDOM = new Random();

    /** The unit's name; immutable for its lifetime to keep equality stable. */
    private final String name;
    /** Damage dealt to the enemy army per combat round. Always non-negative. */
    private int strength;
    /** Current hit points. Always non-negative. */
    private int health;
    /** Flat damage reduction per hit; clamped to a minimum of zero. */
    private final int defense;
    /** Chance in [0, 1] that an incoming hit is dodged entirely. */
    private final double evasion;

    /**
     * Constructs a unit with full health and no special defenses.
     *
     * @param name     the unit type name; must not be {@code null} or blank
     * @param strength the unit's damage per round; clamped to a minimum of 0
     * @param health   the unit's starting hit points; clamped to a minimum of 0
     */
    public Unit(String name, int strength, int health) {
        this(name, strength, health, 0, 0.0);
    }

    /**
     * Constructs a unit with full defensive stats.
     *
     * @param name     the unit type name; must not be {@code null} or blank
     * @param strength the unit's damage per round; clamped to a minimum of 0
     * @param health   the unit's starting hit points; clamped to a minimum of 0
     * @param defense  flat damage absorbed per incoming hit
     * @param evasion  chance in [0, 1] to dodge an incoming hit entirely
     */
    public Unit(String name, int strength, int health, int defense, double evasion) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Unit name must not be null or blank");
        }
        this.name = name;
        this.strength = Math.max(0, strength);
        this.health = Math.max(0, health);
        this.defense = Math.max(0, defense);
        this.evasion = Math.max(0.0, Math.min(1.0, evasion));
    }

    /**
     * Returns the unit's name.
     *
     * @return the unit type name, never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unit's strength.
     *
     * @return the per-round damage figure
     */
    public int getStrength() {
        return strength;
    }

    /**
     * Sets the unit's strength.
     *
     * @param strength the new damage figure (clamped to non-negative)
     */
    public void setStrength(int strength) {
        this.strength = Math.max(0, strength);
    }

    /**
     * Returns the unit's current health.
     *
     * @return the hit point count
     */
    public int getHealth() {
        return health;
    }

    /**
     * Sets the unit's current health.
     *
     * @param health the new hit point count (clamped to non-negative)
     */
    public void setHealth(int health) {
        this.health = Math.max(0, health);
    }

    /**
     * Returns the flat damage reduction applied to each incoming hit.
     *
     * @return the unit's defense rating
     */
    public int getDefense() {
        return defense;
    }

    /**
     * Returns the chance in {@code [0, 1]} that a hit is dodged outright.
     *
     * @return the unit's evasion probability
     */
    public double getEvasion() {
        return evasion;
    }

    /**
     * Reports whether the unit is still on the battlefield.
     *
     * @return {@code true} when {@link #getHealth()} is greater than zero
     */
    public boolean isAlive() {
        return health > 0;
    }

    /**
     * Heals the unit by a flat amount, never exceeding its starting hit
     * point ceiling tracked elsewhere (this method only goes up).
     *
     * @param amount how many hit points to restore; values at most 0 are ignored
     */
    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        this.health = this.health + amount;
    }

    /**
     * Subtracts the given damage from this unit's health, never going
     * below zero. Defense and evasion are applied here so subclasses get
     * the behaviour for free.
     *
     * @param damage the amount of damage to apply; values at most 0 are ignored
     */
    public void takeDamage(int damage) {
        if (damage <= 0) {
            return;
        }
        if (evasion > 0.0 && EVASION_RANDOM.nextDouble() < evasion) {
            return;
        }
        int effective = Math.max(1, damage - defense);
        this.health = Math.max(0, this.health - effective);
    }

    /**
     * Returns the effective damage the unit deals during one combat round.
     * <p>
     * The default implementation simply returns {@link #getStrength()}.
     * Subclasses (e.g. cavalry that does more damage on a charge) may
     * override this to implement more complex combat semantics - a
     * pure polymorphism hook.
     *
     * @return the damage value used by the battle resolver
     */
    public int computeDamage() {
        return strength;
    }

    /**
     * Called by the battle resolver at the start of each engagement so
     * subclasses can refresh per-battle state (volleys, charges, ...).
     */
    public void resetForBattle() {
        // No-op for the base unit.
    }

    /**
     * Returns a short, user-facing description of any special ability.
     * Subclasses override this so the unit detail dialog can explain why a
     * unit feels different.
     *
     * @return a non-null description; the empty string means "no ability"
     */
    public String getAbilityDescription() {
        return "";
    }

    /**
     * Two units are equal if every field matches. Equality is used by the
     * action history when comparing snapshots; therefore it must be both
     * symmetric and consistent.
     *
     * @param o the other object
     * @return {@code true} when the objects represent the same unit data
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Unit)) {
            return false;
        }
        Unit unit = (Unit) o;
        return strength == unit.strength
                && health == unit.health
                && defense == unit.defense
                && Double.compare(unit.evasion, evasion) == 0
                && name.equals(unit.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, strength, health, defense, evasion);
    }

    @Override
    public String toString() {
        return name + " (HP=" + health + ", STR=" + strength + ")";
    }
}
