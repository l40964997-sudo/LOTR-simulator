package nl.rug.oop.rts.model.observer;

import java.util.Objects;

/**
 * Immutable description of a change that occurred in an {@link Observable}.
 *
 * <p>Each event carries a {@link Type} indicating what kind of change
 * happened, so listeners can react selectively (for example the renderer
 * repaints on every change while the action enabler only cares about
 * selection changes), plus the source object, which lets a listener
 * disambiguate when it is subscribed to several observables. The class
 * deliberately exposes very little: model events should be coarse grained
 * and self contained.</p>
 */
public final class ModelEvent {

    /** The kind of change that occurred. */
    private final Type type;

    /** The observable that triggered the notification. */
    private final Object source;

    /**
     * Constructs a new event.
     *
     * @param type the kind of change, must not be {@code null}
     * @param source the originator of the event, must not be {@code null}
     */
    public ModelEvent(Type type, Object source) {
        this.type = Objects.requireNonNull(type, "type");
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * Returns the kind of change.
     *
     * @return the event type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the object that fired the notification.
     *
     * @return the source
     */
    public Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "ModelEvent[" + type + " from " + source.getClass().getSimpleName() + "]";
    }

    /**
     * Classification of model changes.
     *
     * <p>The categories are intentionally broad. They are not a full
     * taxonomy of every mutation; rather they let listeners decide whether
     * to repaint, refresh button state, or rebuild a side panel without
     * parsing payload fields.</p>
     */
    public enum Type {

        /** A node or edge was added, removed, renamed or moved. */
        GRAPH_STRUCTURE,

        /** The currently selected element changed. */
        SELECTION,

        /** An army was added, moved, lost units, or was destroyed. */
        ARMY,

        /** An event was added or removed from a node or edge. */
        EVENT,

        /** The simulation advanced one step. */
        SIMULATION,

        /** Catch-all when none of the above is precise enough. */
        GENERIC
    }
}
