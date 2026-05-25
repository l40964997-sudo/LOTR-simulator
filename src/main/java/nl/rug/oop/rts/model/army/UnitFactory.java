package nl.rug.oop.rts.model.army;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Factory that produces {@link Unit} instances belonging to a given faction.
 * <p>
 * The factory consolidates two responsibilities:
 * <ol>
 *   <li>picking a unit name from the faction's pool;</li>
 *   <li>generating reasonable starting statistics, with per-name flavour
 *       so that, e.g., trolls feel like trolls and Saruman feels like
 *       Saruman.</li>
 * </ol>
 * Each known unit name is wired to a small template that captures its stat
 * profile and the concrete {@link Unit} subclass to instantiate. Unknown
 * names fall back to a generic warrior so loading older save files keeps
 * working - a deliberate Open/Closed concession.
 */
public final class UnitFactory {

    /** Templates keyed by lower-case unit name. */
    private static final Map<String, UnitTemplate> TEMPLATES = buildTemplates();

    /** Fallback template used when an unknown unit name is supplied. */
    private static final UnitTemplate FALLBACK =
            new UnitTemplate(10, 16, 80, 120, 1, 0.05, UnitFactory::buildPlain);

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
     * @return a fresh {@link Unit} (possibly a subclass)
     */
    public Unit createRandomUnit(Faction faction) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        String name = faction.getUnitNames().get(random.nextInt(faction.getUnitNames().size()));
        return createNamedUnit(faction, name);
    }

    /**
     * Creates a unit with a specific name, drawing stats from its template.
     *
     * @param faction the faction the unit belongs to
     * @param name    the exact unit name; falls back to a generic warrior
     *                if no template is registered
     * @return the constructed unit
     */
    public Unit createNamedUnit(Faction faction, String name) {
        if (faction == null) {
            throw new IllegalArgumentException("faction must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        UnitTemplate template = TEMPLATES.getOrDefault(name.toLowerCase(Locale.ROOT), FALLBACK);
        int strength = randomInRange(template.minStrength, template.maxStrength);
        int health = randomInRange(template.minHealth, template.maxHealth);
        return template.builder.apply(name, new StatPack(strength, health, template.defense, template.evasion));
    }

    /**
     * Creates a unit from explicit stats. Used by the JSON reader so a
     * saved game restores the exact unit a player remembers.
     *
     * @param faction the faction; ignored for the actual stats but validated
     *                for non-null friendliness with callers
     * @param name unit name; resolved to its template for the subclass type
     * @param strength damage per round
     * @param health starting hit points
     * @return a fresh {@link Unit}
     */
    public Unit createUnitWithStats(Faction faction, String name, int strength, int health) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        UnitTemplate template = TEMPLATES.getOrDefault(name.toLowerCase(Locale.ROOT), FALLBACK);
        return template.builder.apply(name, new StatPack(strength, health, template.defense, template.evasion));
    }

    /**
     * Returns the registered template for a unit name, if any. The result
     * is read-only data, exposed for UI tooltips.
     *
     * @param name the unit name; case insensitive
     * @return the template, or the generic fallback when none is registered
     */
    public static UnitTemplate templateFor(String name) {
        if (name == null || name.isBlank()) {
            return FALLBACK;
        }
        return TEMPLATES.getOrDefault(name.toLowerCase(Locale.ROOT), FALLBACK);
    }

    /**
     * Helper around {@link Random#nextInt(int)} that returns a value in
     * {@code [low, high)}.
     *
     * @param low the inclusive lower bound
     * @param high the exclusive upper bound
     * @return a random value in the range
     */
    private int randomInRange(int low, int high) {
        return low + random.nextInt(Math.max(1, high - low));
    }

    /* ===================== Template registry ===================== */

    /**
     * Builds the unit name to template mapping.
     *
     * @return the registry
     */
    private static Map<String, UnitTemplate> buildTemplates() {
        Map<String, UnitTemplate> map = new HashMap<>();
        registerFreePeople(map);
        registerMordor(map);
        registerIsengard(map);
        return map;
    }

    /**
     * Adds Men, Elves, Dwarves and Hobbit unit templates.
     *
     * @param map the registry to populate
     */
    private static void registerFreePeople(Map<String, UnitTemplate> map) {
        map.put("gondor soldier", new UnitTemplate(12, 18, 100, 130, 2, 0.05, UnitFactory::buildPlain));
        map.put("tower guard", new UnitTemplate(10, 14, 130, 170, 5, 0.02, UnitFactory::buildHeavy));
        map.put("ithilien ranger", new UnitTemplate(11, 16, 85, 110, 1, 0.10, UnitFactory::buildArcher));
        map.put("knight of dol amroth", new UnitTemplate(14, 19, 110, 140, 3, 0.05, UnitFactory::buildCavalry));
        map.put("lorien warrior", new UnitTemplate(13, 18, 95, 120, 1, 0.10, UnitFactory::buildPlain));
        map.put("mirkwood archer", new UnitTemplate(13, 18, 80, 105, 0, 0.10, UnitFactory::buildArcher));
        map.put("rivendell lancer", new UnitTemplate(13, 19, 100, 130, 2, 0.10, UnitFactory::buildCavalry));
        map.put("galadhrim sentinel", new UnitTemplate(12, 17, 110, 140, 3, 0.05, UnitFactory::buildHeavy));
        map.put("guardian", new UnitTemplate(10, 14, 130, 160, 6, 0.0, UnitFactory::buildHeavy));
        map.put("phalanx", new UnitTemplate(9, 13, 140, 175, 7, 0.0, UnitFactory::buildHeavy));
        map.put("axe thrower", new UnitTemplate(13, 18, 90, 115, 1, 0.05, UnitFactory::buildArcher));
        map.put("iron hill berserker", new UnitTemplate(16, 22, 100, 130, 2, 0.0, UnitFactory::buildPlain));
        map.put("shire slinger", new UnitTemplate(7, 11, 60, 80, 0, 0.25, UnitFactory::buildArcher));
        map.put("stout hobbit", new UnitTemplate(8, 12, 75, 95, 1, 0.20, UnitFactory::buildStealth));
        map.put("bounder", new UnitTemplate(9, 13, 70, 90, 0, 0.30, UnitFactory::buildStealth));
        map.put("took scout", new UnitTemplate(8, 12, 65, 85, 0, 0.35, UnitFactory::buildStealth));
    }

    /**
     * Adds Mordor unit templates (Orcs, Goblins, Trolls, Men of Darkness).
     *
     * @param map the registry to populate
     */
    private static void registerMordor(Map<String, UnitTemplate> map) {
        map.put("orc", new UnitTemplate(10, 15, 80, 110, 1, 0.0, UnitFactory::buildPlain));
        map.put("goblin", new UnitTemplate(6, 10, 50, 70, 0, 0.15, UnitFactory::buildStealth));
        map.put("troll", new UnitTemplate(15, 22, 200, 260, 6, 0.0, UnitFactory::buildTroll));
        map.put("man of darkness", new UnitTemplate(12, 17, 95, 125, 2, 0.05, UnitFactory::buildPlain));
    }

    /**
     * Adds Isengard unit templates (Saruman, Uruk-hai, Warg Rider, Uruk Crossbowman).
     *
     * @param map the registry to populate
     */
    private static void registerIsengard(Map<String, UnitTemplate> map) {
        map.put("saruman", new UnitTemplate(25, 35, 180, 220, 4, 0.10, UnitFactory::buildWizard));
        map.put("uruk-hai", new UnitTemplate(14, 19, 120, 150, 3, 0.0, UnitFactory::buildPlain));
        map.put("warg rider", new UnitTemplate(13, 18, 100, 130, 2, 0.05, UnitFactory::buildCavalry));
        map.put("uruk crossbowman", new UnitTemplate(14, 19, 90, 120, 2, 0.05, UnitFactory::buildCrossbowman));
    }

    /* ===================== Concrete builders ===================== */

    /**
     * Builds a generic line unit with no specialised subclass behaviour.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Unit}
     */
    private static Unit buildPlain(String name, StatPack s) {
        return new Unit(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds an archer.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Archer}
     */
    private static Unit buildArcher(String name, StatPack s) {
        return new Archer(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds an Uruk crossbowman.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Crossbowman}
     */
    private static Unit buildCrossbowman(String name, StatPack s) {
        return new Crossbowman(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds a cavalry unit.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Cavalry}
     */
    private static Unit buildCavalry(String name, StatPack s) {
        return new Cavalry(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds a heavily armoured line unit.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Heavy}
     */
    private static Unit buildHeavy(String name, StatPack s) {
        return new Heavy(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds a troll.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Troll}
     */
    private static Unit buildTroll(String name, StatPack s) {
        return new Troll(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds a stealthy small unit.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Stealth}
     */
    private static Unit buildStealth(String name, StatPack s) {
        return new Stealth(name, s.strength, s.health, s.defense, s.evasion);
    }

    /**
     * Builds a wizard hero unit.
     *
     * @param name the unit name
     * @param s the rolled stats
     * @return a freshly constructed {@link Wizard}
     */
    private static Unit buildWizard(String name, StatPack s) {
        return new Wizard(name, s.strength, s.health, s.defense, s.evasion);
    }

    /** Small immutable stat bundle passed to each builder. */
    private static final class StatPack {
        /** Rolled strength. */
        private final int strength;
        /** Rolled health. */
        private final int health;
        /** Template defense rating. */
        private final int defense;
        /** Template evasion probability. */
        private final double evasion;

        /**
         * Constructs a stat pack.
         *
         * @param strength rolled strength
         * @param health rolled health
         * @param defense template defense
         * @param evasion template evasion in [0, 1]
         */
        private StatPack(int strength, int health, int defense, double evasion) {
            this.strength = strength;
            this.health = health;
            this.defense = defense;
            this.evasion = evasion;
        }
    }

    /**
     * Immutable per-unit template describing the rolled stat ranges and the
     * concrete builder responsible for instantiating the right subclass.
     */
    public static final class UnitTemplate {
        /** Inclusive lower bound on strength. */
        private final int minStrength;
        /** Exclusive upper bound on strength. */
        private final int maxStrength;
        /** Inclusive lower bound on health. */
        private final int minHealth;
        /** Exclusive upper bound on health. */
        private final int maxHealth;
        /** Flat defense applied to instances of this template. */
        private final int defense;
        /** Evasion probability applied to instances of this template. */
        private final double evasion;
        /** Constructor invoked once stats have been rolled. */
        private final BiFunction<String, StatPack, Unit> builder;

        /**
         * Constructs a template.
         *
         * @param minStrength inclusive lower bound on strength
         * @param maxStrength exclusive upper bound on strength
         * @param minHealth inclusive lower bound on health
         * @param maxHealth exclusive upper bound on health
         * @param defense flat defense rating
         * @param evasion evasion probability in [0, 1]
         * @param builder constructor for the right {@link Unit} subclass
         */
        private UnitTemplate(int minStrength, int maxStrength, int minHealth, int maxHealth,
                             int defense, double evasion, BiFunction<String, StatPack, Unit> builder) {
            this.minStrength = minStrength;
            this.maxStrength = maxStrength;
            this.minHealth = minHealth;
            this.maxHealth = maxHealth;
            this.defense = defense;
            this.evasion = evasion;
            this.builder = builder;
        }

        /**
         * Returns the average strength suggested by the template.
         *
         * @return the midpoint strength value
         */
        public int averageStrength() {
            return (minStrength + maxStrength) / 2;
        }

        /**
         * Returns the average health suggested by the template.
         *
         * @return the midpoint health value
         */
        public int averageHealth() {
            return (minHealth + maxHealth) / 2;
        }

        /**
         * Returns the template's defense rating.
         *
         * @return the defense value
         */
        public int defense() {
            return defense;
        }

        /**
         * Returns the template's evasion probability.
         *
         * @return the evasion value in [0, 1]
         */
        public double evasion() {
            return evasion;
        }
    }
}
