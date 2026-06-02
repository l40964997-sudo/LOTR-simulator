package nl.rug.oop.rts.model.graph;

import nl.rug.oop.rts.model.army.Faction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A location in the world map.
 * <p>
 * In addition to the data inherited from {@link MapElement} (id, name,
 * armies, events), a node knows the {@link Edge}s incident to it and its
 * spatial coordinates in graph space. The coordinates are intentionally
 * stored on the model rather than computed by the view, because:
 * <ul>
 *   <li>they survive across panning and zooming;</li>
 *   <li>they round-trip through Undo/Redo (moving a node is a recorded
 *       action);</li>
 *   <li>they would be needed if the JSON exporter were extended with
 *       layout persistence.</li>
 * </ul>
 */
public class Node extends MapElement {

    /** Default population for a freshly created settlement. */
    private static final int DEFAULT_POPULATION = 5000;

    /** Default literacy percentage (0-100) for a new settlement. */
    private static final int DEFAULT_LITERACY = 35;

    /** Edges currently incident to this node. */
    private final List<Edge> edges = new ArrayList<>();

    /** X coordinate in graph space. */
    private int x;

    /** Y coordinate in graph space. */
    private int y;

    /** Faction the settlement belongs to (its political home, not necessarily the occupier). */
    private Faction faction = Faction.MEN;

    /** Inhabitant headcount. */
    private int population = DEFAULT_POPULATION;

    /** Percentage of inhabitants who can read; clamped to [0, 100]. */
    private int literacy = DEFAULT_LITERACY;

    /** Coarse wealth tier of the settlement. */
    private Wealth wealth = Wealth.MODEST;

    /** Primary language spoken at the settlement. */
    private String language = Faction.MEN.defaultLanguage();

    /**
     * Constructs a node with explicit coordinates.
     *
     * @param id   unique node id
     * @param name initial display name
     * @param x    horizontal position
     * @param y    vertical position
     */
    public Node(int id, String name, int x, int y) {
        super(id, name);
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return getName() + "(" + getId() + ")@(" + x + "," + y + ")";
    }

    /**
     * Returns the x coordinate.
     *
     * @return horizontal position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y coordinate.
     *
     * @return vertical position
     */
    public int getY() {
        return y;
    }

    /**
     * Updates the node position.
     *
     * @param x new x
     * @param y new y
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the faction this settlement belongs to politically.
     *
     * @return the faction
     */
    public Faction getFaction() {
        return faction;
    }

    /**
     * Updates the settlement's home faction. Also resets the spoken language
     * to that faction's default if the language has not been customised
     * (i.e. still matches one of the canonical defaults).
     *
     * @param faction the new faction; ignored when {@code null}
     */
    public void setFaction(Faction faction) {
        if (faction == null) {
            return;
        }
        boolean isDefaultLanguage = false;
        for (Faction f : Faction.values()) {
            if (f.defaultLanguage().equals(this.language)) {
                isDefaultLanguage = true;
                break;
            }
        }
        this.faction = faction;
        if (isDefaultLanguage) {
            this.language = faction.defaultLanguage();
        }
    }

    /**
     * Returns the settlement's population.
     *
     * @return inhabitant headcount
     */
    public int getPopulation() {
        return population;
    }

    /**
     * Sets the settlement's population. Negative values are clamped to zero.
     *
     * @param population the new headcount
     */
    public void setPopulation(int population) {
        this.population = Math.max(0, population);
    }

    /**
     * Returns the literacy percentage (0-100).
     *
     * @return the percentage of inhabitants who can read
     */
    public int getLiteracy() {
        return literacy;
    }

    /**
     * Sets the literacy percentage. Values are clamped to [0, 100].
     *
     * @param literacy the new percentage
     */
    public void setLiteracy(int literacy) {
        this.literacy = Math.max(0, Math.min(100, literacy));
    }

    /**
     * Returns the settlement's wealth tier.
     *
     * @return the wealth
     */
    public Wealth getWealth() {
        return wealth;
    }

    /**
     * Sets the settlement's wealth tier.
     *
     * @param wealth the new wealth tier; ignored when {@code null}
     */
    public void setWealth(Wealth wealth) {
        if (wealth == null) {
            return;
        }
        this.wealth = wealth;
    }

    /**
     * Returns the settlement's spoken language.
     *
     * @return the language name
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the settlement's spoken language. Blank values are ignored.
     *
     * @param language the new language
     */
    public void setLanguage(String language) {
        if (language == null || language.isBlank()) {
            return;
        }
        this.language = language;
    }

    /**
     * Returns an unmodifiable view of the incident edges.
     *
     * @return the edges adjacent to this node
     */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /**
     * Registers an incident edge. Package private because only the
     * {@link Graph} should call it.
     *
     * @param edge the edge to register; ignored when {@code null} or already present
     */
    void attachEdge(Edge edge) {
        if (edge == null || edges.contains(edge)) {
            return;
        }
        edges.add(edge);
    }

    /**
     * Unregisters an incident edge. Package private; see {@link #attachEdge(Edge)}.
     *
     * @param edge the edge
     */
    void detachEdge(Edge edge) {
        edges.remove(edge);
    }
}
