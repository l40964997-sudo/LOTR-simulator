package nl.rug.oop.rts.model.army;

import lombok.Getter;

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
 * mutation helpers on the unit list plus a handful of book-keeping fields
 * (morale, last action, player-control flag, fortify shield) that the
 * simulator consults when wiring up the per-step random-action and
 * player-control features. Rendering and combat logic still live elsewhere
 * so cohesion stays high.
 */
@Getter
public class Army {

    /** Sentinel value indicating "no last action recorded". */
    private static final String NO_ACTION = "";

    /** Display name for this army, surfaced in the side panel. */
    private String name;
    /** The faction the army belongs to. */
    private final Faction faction;
    /** Units making up the army. Defensively wrapped on read. */
    private final List<Unit> units;
    /** Whether the user has taken direct control of this army. */
    private boolean playerControlled;
    /** Morale modifier in [0, 1.5]; combat power scales with it. */
    private double morale = 1.0;
    /** Name of the action executed during the most recent simulation step. */
    private String lastAction = NO_ACTION;
    /** Active fortify shield in [0, 1]: damage fraction absorbed next battle. */
    private double fortifyShield;

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
     * Renames the army.
     *
     * @param name the new name; falls back to the faction name when blank
     */
    public void setName(String name) {
        this.name = (name == null || name.isBlank()) ? faction.getDisplayName() : name;
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
     * battle resolver. Scaled by the current morale modifier.
     *
     * @return weighted combat power
     */
    public double combatPower() {
        return (totalStrength() * 1.0 + totalHealth() * 0.1) * morale;
    }

    /**
     * Flags the army as being commanded by the user. The simulator pauses
     * for player input on each step for any army with this flag set.
     *
     * @param playerControlled whether to enable player control
     */
    public void setPlayerControlled(boolean playerControlled) {
        this.playerControlled = playerControlled;
    }

    /**
     * Sets the morale modifier, clamped to a safe range.
     *
     * @param morale the desired morale value
     */
    public void setMorale(double morale) {
        this.morale = Math.max(0.0, Math.min(1.5, morale));
    }

    /**
     * Records the human readable name of the action executed this turn.
     *
     * @param lastAction the action name; {@code null} clears the field
     */
    public void setLastAction(String lastAction) {
        this.lastAction = lastAction == null ? NO_ACTION : lastAction;
    }

    /**
     * Sets the fortify shield percentage; clamped to [0, 1].
     *
     * @param fortifyShield the new shield value
     */
    public void setFortifyShield(double fortifyShield) {
        this.fortifyShield = Math.max(0.0, Math.min(1.0, fortifyShield));
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
