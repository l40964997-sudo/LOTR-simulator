package nl.rug.oop.rts.model.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A collection of {@link Unit} instances that all belong to the same
 * {@link Faction} and therefore the same team.
 * <p>
 * An army is the smallest entity that can move around the graph during a
 * simulation step. The class is intentionally light: it offers query and
 * mutation helpers on the unit list, but contains no rendering or
 * simulation logic of its own. That keeps the cohesion high and the
 * coupling low, since the renderer and the simulator are free to evolve
 * without touching this class.
 */
public class Army {

    /** Display name for this army, surfaced in the side panel. */
    private String name;

    /** The faction the army belongs to. */
    private final Faction faction;

    /** Units making up the army. Defensively wrapped on read. */
    private final List<Unit> units;

    /**
     * Constructs an army with an explicit name.
     *
     * @param name    the display name; defaults to the faction display name
     *                if {@code null} or blank
     * @param faction the faction the army belongs to; must not be {@code null}
     * @param units   initial units; may be {@code null}, which yields an
     *                empty army
     */
    public Army(String name, Faction faction, List<Unit> units) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        this.faction = faction;
        this.name = (name == null || name.isBlank()) ? faction.getDisplayName() : name;
        this.units = (units == null) ? new ArrayList<>() : new ArrayList<>(units);
    }

    /**
     * Returns the army's display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Renames the army.
     *
     * @param name the new name; falls back to the faction name when blank
     */
    public void setName(String name) {
        this.name = (name == null || name.isBlank()) ? faction.getDisplayName() : name;
    }

    /**
     * Returns the army's faction.
     *
     * @return the faction
     */
    public Faction getFaction() {
        return faction;
    }

    /**
     * Returns the army's team identifier. Two armies fight if their teams
     * differ.
     *
     * @return the team id
     */
    public int getTeam() {
        return faction.getTeam();
    }

    /**
     * Returns an unmodifiable view of the unit list.
     * <p>
     * Use {@link #addUnit(Unit)} and {@link #removeUnit(Unit)} to mutate
     * the army; the unmodifiable view enforces this discipline.
     *
     * @return the units in the army
     */
    public List<Unit> getUnits() {
        return Collections.unmodifiableList(units);
    }

    /**
     * Adds a unit to the army.
     *
     * @param unit the unit; ignored when {@code null}
     */
    public void addUnit(Unit unit) {
        if (unit == null) {
            return;
        }
        units.add(unit);
    }

    /**
     * Removes a specific unit instance.
     *
     * @param unit the unit; ignored when {@code null}
     * @return {@code true} if the unit was present and removed
     */
    public boolean removeUnit(Unit unit) {
        if (unit == null) {
            return false;
        }
        return units.remove(unit);
    }

    /**
     * Removes all dead units (health {@code <= 0}).
     */
    public void removeDead() {
        units.removeIf(u -> !u.isAlive());
    }

    /**
     * Reports whether the army has any units left.
     *
     * @return {@code true} if {@link #getUnits()} is empty
     */
    public boolean isDefeated() {
        return units.isEmpty();
    }

    /**
     * Convenience accessor for the army's unit count.
     *
     * @return the number of units currently in the army
     */
    public int size() {
        return units.size();
    }

    /**
     * Sum of {@link Unit#getStrength()} across all units. Used by the
     * battle resolver to gauge an army's combat power.
     *
     * @return the total strength
     */
    public int totalStrength() {
        int total = 0;
        for (Unit u : units) {
            total += u.getStrength();
        }
        return total;
    }

    /**
     * Sum of {@link Unit#getHealth()} across all units.
     *
     * @return the total health
     */
    public int totalHealth() {
        int total = 0;
        for (Unit u : units) {
            total += u.getHealth();
        }
        return total;
    }

    /**
     * A rough single-number combat power, used as a tie breaker in the
     * battle resolver.
     *
     * @return weighted combat power
     */
    public double combatPower() {
        return totalStrength() * 1.0 + totalHealth() * 0.1;
    }

    @Override
    public boolean equals(Object o) {
        // Reference equality is the right choice here: two armies created
        // independently are distinct combatants even when they share stats.
        return this == o;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return name + " [" + faction.getDisplayName() + ", size=" + units.size() + "]";
    }
}
