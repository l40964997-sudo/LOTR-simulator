package nl.rug.oop.rts.model.observer;

/**
 * Receives notifications when an {@link Observable} model component changes.
 * <p>
 * The interface forms the listener half of the bespoke Observer pattern used
 * throughout the project. Java's built-in {@code java.util.Observable} class
 * has been deprecated since Java 9 and is explicitly forbidden by the
 * assignment specification, hence this hand-rolled equivalent.
 * <p>
 * Listeners receive an opaque {@link ModelEvent} describing what changed.
 * Because the event carries the source, a single listener may legitimately
 * subscribe to several observables and still be able to tell them apart.
 */
@FunctionalInterface
public interface ModelListener {

    /**
     * Called whenever the observed model fires {@code notifyListeners}.
     *
     * @param event description of the change; never {@code null}
     */
    void modelChanged(ModelEvent event);
}
