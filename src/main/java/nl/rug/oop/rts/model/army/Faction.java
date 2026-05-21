package nl.rug.oop.rts.model.army;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enumeration of the five factions in the simulation.
 * <p>
 * Each faction declares:
 * <ul>
 *   <li>its display name,</li>
 *   <li>the team it belongs to (Free Peoples = team 0, Shadow = team 1),</li>
 *   <li>a palette colour used by the renderer so that armies of the same
 *       faction are visually consistent,</li>
 *   <li>the names of the unit types that armies of this faction may field.</li>
 * </ul>
 * Bundling all faction specific data in the enum (instead of in scattered
 * constants) gives a single source of truth and keeps related information
 * together - a textbook application of high cohesion.
 */
public enum Faction {

    /** Soldiers of Gondor and surrounding kingdoms of Men. */
    MEN("Men", 0, new Color(0xC9B57C),
            "Gondor Soldier", "Tower Guard", "Ithilien Ranger"),

    /** Elven warriors from Lothlórien, Mirkwood, and Rivendell. */
    ELVES("Elves", 0, new Color(0x8BC4A0),
            "Lorien Warrior", "Mirkwood Archer", "Rivendell Lancer"),

    /** Dwarves of Erebor and the Iron Hills. */
    DWARVES("Dwarves", 0, new Color(0xB35E27),
            "Guardian", "Phalanx", "Axe Thrower"),

    /** Sauron's hosts from Mordor and Harad. */
    MORDOR("Mordor", 1, new Color(0x6C1F1F),
            "Orc Warrior", "Orc Pikeman", "Haradrim Archer"),

    /** Saruman's hosts from Isengard. */
    ISENGARD("Isengard", 1, new Color(0x2F2F2F),
            "Uruk-hai", "Uruk Crossbowman", "Warg Rider");

    /** Convenience constant naming team 0 (Free Peoples). */
    public static final int TEAM_FREE_PEOPLES = 0;

    /** Convenience constant naming team 1 (Shadow). */
    public static final int TEAM_SHADOW = 1;

    /** Human readable name as required by the JSON specification. */
    private final String displayName;

    /** Team identifier; armies on the same team do not fight each other. */
    private final int team;

    /** Rendering colour for this faction. */
    private final Color color;

    /** The unit names this faction is allowed to field. */
    private final List<String> unitNames;

    Faction(String displayName, int team, Color color, String... unitNames) {
        this.displayName = displayName;
        this.team = team;
        this.color = color;
        this.unitNames = Collections.unmodifiableList(Arrays.asList(unitNames));
    }

    /**
     * Returns the human readable faction name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the team identifier.
     * <p>
     * Two armies fight each other if and only if their teams differ.
     *
     * @return the team id (0 or 1 for the current ruleset)
     */
    public int getTeam() {
        return team;
    }

    /**
     * Returns the colour the renderer should use for armies of this faction.
     *
     * @return a non-null {@link Color}
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the immutable list of permitted unit names.
     *
     * @return the unit name pool
     */
    public List<String> getUnitNames() {
        return unitNames;
    }

    /**
     * Returns the capitalised key segment used to locate this faction's
     * textures through {@link nl.rug.oop.rts.util.TextureLoader}.
     * <p>
     * The course texture loader exposes its sprites under camel-case names
     * such as {@code factionMen} and {@code fortressElves}. The renderer
     * prefixes this segment with {@code "faction"} or {@code "fortress"} to
     * build the full key, so a future rename of the assets only touches the
     * mapping declared here.
     *
     * @return the key segment, e.g. {@code "Men"}
     */
    public String textureKey() {
        String lower = displayName.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Finds a faction by its display name. The comparison is case insensitive
     * so that the JSON deserialisation logic does not have to normalise the
     * input.
     *
     * @param displayName the name to look up; may be {@code null}
     * @return the matching faction, or {@code null} when no match is found
     */
    public static Faction fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (Faction f : values()) {
            if (Objects.equals(f.displayName.toLowerCase(), displayName.toLowerCase())) {
                return f;
            }
        }
        return null;
    }
}
