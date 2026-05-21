package nl.rug.oop.rts.model.army;

import java.util.Locale;
import java.util.Random;

/**
 * Factory that produces {@link Unit} instances belonging to a given faction.
 * <p>
 * The factory consolidates two responsibilities:
 * <ol>
 *   <li>picking a unit name from the faction's pool;</li>
 *   <li>generating reasonable starting statistics, with mild per-name
 *       customisation so that, e.g., archers really feel like archers.</li>
 * </ol>
 * Centralising creation here means {@link Army} construction does not need
 * to know about any specific unit subclass; the rest of the system happily
 * works against the abstract {@code Unit} type. This both showcases the
 * Factory pattern and keeps coupling between the army and the unit
 * hierarchy low.
 */
public final class UnitFactory {

    /** Lower bound on randomly generated strength. */
    private static final int MIN_STRENGTH = 8;

    /** Upper bound (exclusive) on randomly generated strength. */
    private static final int MAX_STRENGTH = 18;

    /** Lower bound on randomly generated starting health. */
    private static final int MIN_HEALTH = 80;

    /** Upper bound (exclusive) on randomly generated starting health. */
    private static final int MAX_HEALTH = 130;

    /** Random source; replaceable for testing. */
    private final Random random;

    /**
     * Constructs a factory with a fresh non-deterministic random source.
     */
    public UnitFactory() {
        this(new Random());
    }

    /**
     * Constructs a factory with the supplied random source. Useful in tests
     * where deterministic behaviour is required.
     *
     * @param random the random source; must not be {@code null}
     */
    public UnitFactory(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /**
     * Creates a random unit belonging to {@code faction}.
     *
     * @param faction the faction the unit will fight for
     * @return a fresh {@link Unit} (possibly a subclass such as {@link Archer})
     */
    public Unit createRandomUnit(Faction faction) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        String name = faction.getUnitNames().get(random.nextInt(faction.getUnitNames().size()));
        return createNamedUnit(faction, name);
    }

    /**
     * Creates a unit with a specific name. Allowed for deserialisation
     * use cases where the name has been read back from disk.
     *
     * @param faction the faction the unit belongs to
     * @param name    the exact unit name; must be in the faction's pool
     * @return the constructed unit
     */
    public Unit createNamedUnit(Faction faction, String name) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        int strength = randomInRange(MIN_STRENGTH, MAX_STRENGTH);
        int health = randomInRange(MIN_HEALTH, MAX_HEALTH);
        // Archers favour ranged damage but have less health: tiny stat shift.
        boolean isArcher = name.toLowerCase(Locale.ROOT).contains("archer")
                || name.toLowerCase(Locale.ROOT).contains("crossbowman")
                || name.toLowerCase(Locale.ROOT).contains("ranger");
        if (isArcher) {
            return new Archer(name, strength + 2, Math.max(40, health - 20));
        }
        return new Unit(name, strength, health);
    }

    /**
     * Creates a unit from explicit stats. Used by the JSON reader (bonus).
     *
     * @param faction the faction; only validated for consistency with
     *                {@code name} when not {@code null}
     * @param name unit name
     * @param strength damage per round
     * @param health starting hit points
     * @return a fresh {@link Unit}
     */
    public Unit createUnitWithStats(Faction faction, String name, int strength, int health) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        boolean isArcher = name.toLowerCase(Locale.ROOT).contains("archer")
                || name.toLowerCase(Locale.ROOT).contains("crossbowman")
                || name.toLowerCase(Locale.ROOT).contains("ranger");
        if (isArcher) {
            return new Archer(name, strength, health);
        }
        return new Unit(name, strength, health);
    }

    /**
     * Helper around {@link Random#nextInt(int)} that returns a value in
     * {@code [low, high)}.
     */
    private int randomInRange(int low, int high) {
        return low + random.nextInt(Math.max(1, high - low));
    }
}
