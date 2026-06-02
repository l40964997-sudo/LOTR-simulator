package nl.rug.oop.rts.model.army;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enumeration of the seven factions in the simulation.
 * <p>
 * The roster reflects the canonical Lord of the Rings line-up:
 * <ul>
 *   <li>Free Peoples (team 0): Men, Elves, Dwarves, Hobbits.</li>
 *   <li>Sauron's Servants (team 1): Mordor, Isengard.</li>
 * </ul>
 * Mordor fields Orcs, Goblins, Trolls and Men of Darkness; Isengard fields
 * Saruman himself together with Uruk-hai, Warg Riders and Uruk Crossbowmen.
 * Bundling all faction specific data in the enum gives a single source of
 * truth, keeping cohesion high.
 */
public enum Faction {

    /** Soldiers of Gondor and surrounding kingdoms of Men. */
    MEN("Men", 0, new Color(0xC9B57C), "Men",
            "Gondor Soldier", "Tower Guard", "Ithilien Ranger", "Knight of Dol Amroth"),

    /** Elven warriors from Lothlorien, Mirkwood, and Rivendell. */
    ELVES("Elves", 0, new Color(0x8BC4A0), "Elves",
            "Lorien Warrior", "Mirkwood Archer", "Rivendell Lancer", "Galadhrim Sentinel"),

    /** Dwarves of Erebor and the Iron Hills. */
    DWARVES("Dwarves", 0, new Color(0xB35E27), "Dwarves",
            "Guardian", "Phalanx", "Axe Thrower", "Iron Hill Berserker"),

    /** The Halflings of the Shire, small but unexpectedly stout-hearted. */
    HOBBITS("Hobbits", 0, new Color(0x6FA86F), "Men",
            "Shire Slinger", "Stout Hobbit", "Bounder", "Took Scout"),

    /** Sauron's hosts from the Black Land. */
    MORDOR("Mordor", 1, new Color(0x6C1F1F), "Mordor",
            "Orc", "Goblin", "Troll", "Man of Darkness"),

    /** Saruman's hosts from Isengard. */
    ISENGARD("Isengard", 1, new Color(0x2F2F2F), "Isengard",
            "Saruman", "Uruk-hai", "Warg Rider", "Uruk Crossbowman");

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
    /** Texture key segment used by the renderer (faction sprite folder). */
    private final String textureKey;
    /** The unit names this faction is allowed to field. */
    private final List<String> unitNames;

    /**
     * Constructs a faction constant.
     *
     * @param displayName the user facing name
     * @param team the team id this faction belongs to
     * @param color the rendering colour
     * @param textureKey the texture key segment, e.g. {@code "Men"}
     * @param unitNames the unit names this faction may field
     */
    Faction(String displayName, int team, Color color, String textureKey, String... unitNames) {
        this.displayName = displayName;
        this.team = team;
        this.color = color;
        this.textureKey = textureKey;
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
     * The renderer prefixes this segment with {@code "faction"} or
     * {@code "fortress"} to build the full key. Factions that lack their own
     * sprite (like the Hobbits) declare a fallback key so the renderer still
     * has something to draw.
     *
     * @return the key segment, e.g. {@code "Men"}
     */
    public String textureKey() {
        return textureKey;
    }

    /**
     * Reports whether this faction belongs to the Free Peoples of Middle Earth.
     *
     * @return {@code true} if its team is {@link #TEAM_FREE_PEOPLES}
     */
    public boolean isFreePeople() {
        return team == TEAM_FREE_PEOPLES;
    }

    /**
     * Returns the canonical Tolkien language spoken by this faction. Used as
     * a sensible default for new settlements belonging to the faction.
     *
     * @return the language name
     */
    public String defaultLanguage() {
        switch (this) {
            case MEN: return "Westron";
            case ELVES: return "Sindarin";
            case DWARVES: return "Khuzdul";
            case HOBBITS: return "Hobbitish";
            case MORDOR: return "Black Speech";
            case ISENGARD: return "Common Orcish";
            default: return "Westron";
        }
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
