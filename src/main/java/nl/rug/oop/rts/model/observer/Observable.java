package nl.rug.oop.rts.model.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for observable model components.
 * <p>
 * Subclasses call {@link #notifyListeners(ModelEvent.Type)} or
 * {@link #fire(ModelEvent)} whenever a state change should be made visible
 * to interested views and controllers. Listeners can be added and removed
 * with {@link #addListener(ModelListener)} and {@link #removeListener(ModelListener)}.
 * <p>
 * This is a deliberate replacement for the deprecated {@code java.util.Observable}
 * class, mandated by the assignment.
 * <p>
 * The implementation uses a defensive copy when dispatching so that
 * listeners that mutate the listener list during their callback do not
 * cause a {@code ConcurrentModificationException}.
 */
public abstract class Observable {

    /** The active listener subscriptions. */
    private final List<ModelListener> listeners = new ArrayList<>();

    /**
     * Subscribes a listener for future change notifications. Adding the
     * same listener twice will result in it being notified twice; this is
     * intentional so the class does not have to do equality checks on
     * lambdas, which never compare equal.
     *
     * @param listener the listener; must not be {@code null}
     */
    public void addListener(ModelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Removes a previously subscribed listener. Has no effect if the
     * listener was never added.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Convenience helper that builds a {@link ModelEvent} of the given type
     * with {@code this} as the source and dispatches it.
     *
     * @param type the type of change
     */
    protected void notifyListeners(ModelEvent.Type type) {
        fire(new ModelEvent(type, this));
    }

    /**
     * Dispatches the given event to every subscribed listener.
     * <p>
     * A snapshot copy is taken so that listeners may add or remove other
     * listeners during their callback without corrupting iteration.
     *
     * @param event the event; must not be {@code null}
     */
    protected void fire(ModelEvent event) {
        if (event == null) {
            return;
        }
        List<ModelListener> snapshot = new ArrayList<>(listeners);
        for (ModelListener listener : snapshot) {
            listener.modelChanged(event);
        }
    }
}
