package nl.rug.oop.rts.model.army;

import java.util.Objects;

/**
 * A single combatant within an {@link Army}.
 * <p>
 * Each unit stores three core statistics:
 * <ul>
 *   <li>{@code name} - the unit type, e.g. "Gondor Soldier";</li>
 *   <li>{@code strength} - damage dealt per round of combat;</li>
 *   <li>{@code health} - the unit's hit points.</li>
 * </ul>
 * The class is open for extension (non-final, fields {@code protected})
 * because the assignment hints at the possibility of giving different unit
 * types special behaviours later on. The bonus path of subclassing {@code Unit}
 * (e.g. {@code Archer}, {@code Lancer}) requires nothing more than
 * overriding {@link #computeDamage()} or introducing extra fields.
 */
public class Unit {

    /** The unit's name; immutable for its lifetime to keep equality stable. */
    private final String name;

    /** Damage dealt to the enemy army per combat round. Always non-negative. */
    private int strength;

    /** Current hit points. Always non-negative. */
    private int health;

    /**
     * Constructs a unit with full health.
     *
     * @param name     the unit type name; must not be {@code null} or blank
     * @param strength the unit's damage per round; clamped to a minimum of 0
     * @param health   the unit's starting hit points; clamped to a minimum of 0
     */
    public Unit(String name, int strength, int health) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Unit name must not be null or blank");
        }
        this.name = name;
        this.strength = Math.max(0, strength);
        this.health = Math.max(0, health);
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
     * Reports whether the unit is still on the battlefield.
     *
     * @return {@code true} when {@link #getHealth()} is greater than zero
     */
    public boolean isAlive() {
        return health > 0;
    }

    /**
     * Subtracts the given damage from this unit's health, never going
     * below zero.
     *
     * @param damage the amount of damage to apply; values at most 0 are ignored
     */
    public void takeDamage(int damage) {
        if (damage <= 0) {
            return;
        }
        this.health = Math.max(0, this.health - damage);
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
                && name.equals(unit.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, strength, health);
    }

    @Override
    public String toString() {
        return name + " (HP=" + health + ", STR=" + strength + ")";
    }
}
