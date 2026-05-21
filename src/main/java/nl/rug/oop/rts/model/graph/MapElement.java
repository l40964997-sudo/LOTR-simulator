package nl.rug.oop.rts.model.graph;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.event.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common base class for {@link Node} and {@link Edge}.
 * <p>
 * Both kinds of map elements need to:
 * <ul>
 *   <li>own a unique id and a mutable name;</li>
 *   <li>host a list of {@link Army} instances currently resident at this
 *       location;</li>
 *   <li>host a list of {@link GameEvent} instances that armies may
 *       encounter while at this location.</li>
 * </ul>
 * Putting the shared state and behaviour on a common ancestor keeps the
 * code DRY and provides a uniform target type for the simulator, the
 * renderer, and the JSON writer (each of which has to deal with both
 * nodes and edges in essentially the same way). The {@code Graph} stays
 * free to keep separate collections because the two element kinds still
 * play different structural roles.
 */
public abstract class MapElement {

    /** Unique identifier within its element kind (node or edge). */
    private final int id;

    /** Human readable name; mutable, surfaced through the UI. */
    private String name;

    /** Armies currently stationed on this element. */
    private final List<Army> armies = new ArrayList<>();

    /** Events available to trigger on this element. */
    private final List<GameEvent> events = new ArrayList<>();

    /**
     * Constructs a {@code MapElement}.
     *
     * @param id   unique id within the element's category
     * @param name initial display name; falls back to {@code "Element " + id} when blank
     */
    protected MapElement(int id, String name) {
        this.id = id;
        // Do not call the overridable defaultName() in the constructor to
        // avoid the "this-escape" pitfall. Subclasses can replace a blank
        // name in their own constructors if they wish.
        this.name = (name == null || name.isBlank()) ? "Element" + id : name;
    }

    /**
     * Returns this element's id.
     *
     * @return the unique id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns this element's display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Renames the element. A blank name is silently ignored.
     *
     * @param name the new name
     */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        this.name = name;
    }

    /**
     * Returns an unmodifiable view of the resident armies. Use
     * {@link #addArmy(Army)} and {@link #removeArmy(Army)} to mutate.
     *
     * @return the armies on this element
     */
    public List<Army> getArmies() {
        return Collections.unmodifiableList(armies);
    }

    /**
     * Adds an army to the element.
     *
     * @param army the army; ignored when {@code null}
     */
    public void addArmy(Army army) {
        if (army == null) {
            return;
        }
        armies.add(army);
    }

    /**
     * Removes a specific army instance.
     *
     * @param army the army; ignored when {@code null}
     * @return {@code true} if the army was present and removed
     */
    public boolean removeArmy(Army army) {
        if (army == null) {
            return false;
        }
        return armies.remove(army);
    }

    /**
     * Clears every army from the element.
     */
    public void clearArmies() {
        armies.clear();
    }

    /**
     * Returns an unmodifiable view of the events attached to the element.
     *
     * @return the available events
     */
    public List<GameEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Attaches a {@link GameEvent} to this element.
     *
     * @param event the event; ignored when {@code null}
     */
    public void addEvent(GameEvent event) {
        if (event == null) {
            return;
        }
        events.add(event);
    }

    /**
     * Removes an event from the element.
     *
     * @param event the event; ignored when {@code null}
     * @return {@code true} if it was present and removed
     */
    public boolean removeEvent(GameEvent event) {
        if (event == null) {
            return false;
        }
        return events.remove(event);
    }

    /**
     * Drops every event from the element.
     */
    public void clearEvents() {
        events.clear();
    }
}
