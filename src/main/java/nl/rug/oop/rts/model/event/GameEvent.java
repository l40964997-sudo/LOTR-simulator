package nl.rug.oop.rts.model.event;

import nl.rug.oop.rts.model.army.Army;

/**
 * Base class for events that an {@link Army} may encounter while traversing
 * the map.
 * <p>
 * Each concrete subclass implements the {@link #applyTo(Army)} hook to
 * modify the given army - for example by adding reinforcements,
 * inflicting casualties, or boosting damage. The class therefore plays
 * the role of <em>Strategy</em> in the Strategy pattern: different
 * algorithms (different events) plug into a single call site (the
 * simulator) without that call site having to know what kind of event it
 * is operating on.
 * <p>
 * The class is deliberately a plain {@code abstract class} rather than an
 * interface so that fields common to all events - the display name
 * and the description - live in one place.
 */
public abstract class GameEvent {

    /** Name shown in the UI and persisted to JSON. */
    private final String name;

    /** Short description for the side panel. */
    private final String description;

    /**
     * Constructs an event with a display name and a human readable
     * description.
     *
     * @param name        the unique name; must not be {@code null} or blank
     * @param description short explanation of what the event does
     */
    protected GameEvent(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name must not be null or blank");
        }
        this.name = name;
        this.description = description == null ? "" : description;
    }

    /**
     * Applies the event's effect to the supplied army.
     *
     * @param army the army that triggered the event; must not be {@code null}
     * @return a short human readable description of what happened
     */
    public abstract String applyTo(Army army);

    /**
     * Returns the event's display name. Used as the JSON identifier.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the event's description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
