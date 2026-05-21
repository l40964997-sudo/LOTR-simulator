package nl.rug.oop.rts.model.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Convenience builder that produces fully populated {@link Army} instances.
 * <p>
 * The class wraps a {@link UnitFactory} and the size policy that newly
 * recruited armies should respect. Keeping this responsibility separate
 * from {@link Army} itself (which only owns the units) follows the
 * Single Responsibility Principle - the army knows about its
 * members, the factory knows how to populate it.
 */
public final class ArmyFactory {

    /** Minimum units per fresh army. */
    private static final int MIN_UNITS = 10;

    /** Maximum units per fresh army (inclusive). */
    private static final int MAX_UNITS = 50;

    /** Random source. */
    private final Random random;

    /** The factory that produces the individual units. */
    private final UnitFactory unitFactory;

    /** Default constructor, non-deterministic. */
    public ArmyFactory() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source. Useful for tests.
     *
     * @param random the random source; must not be {@code null}
     */
    public ArmyFactory(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
        this.unitFactory = new UnitFactory(random);
    }

    /**
     * Creates a new army of the given faction with a randomly chosen unit
     * count in the range [{@value #MIN_UNITS}, {@value #MAX_UNITS}].
     *
     * @param faction the faction; must not be {@code null}
     * @return the freshly recruited army
     */
    public Army createRandomArmy(Faction faction) {
        int count = MIN_UNITS + random.nextInt(MAX_UNITS - MIN_UNITS + 1);
        return createArmy(faction, count);
    }

    /**
     * Creates an army of the given faction with an explicit unit count.
     *
     * @param faction the faction; must not be {@code null}
     * @param size the number of units; clamped to {@code [0, infinity)}
     * @return the freshly recruited army
     */
    public Army createArmy(Faction faction, int size) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        int n = Math.max(0, size);
        List<Unit> units = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            units.add(unitFactory.createRandomUnit(faction));
        }
        return new Army(faction.getDisplayName() + " Host", faction, units);
    }
}
