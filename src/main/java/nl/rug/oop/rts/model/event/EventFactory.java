package nl.rug.oop.rts.model.event;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolves event names to concrete {@link GameEvent} instances.
 *
 * <p>The factory acts as the single integration point between the UI and
 * the event hierarchy. Adding a new event type is a one-line change in the
 * internal {@code Registry}; the rest of the project (the option menu, the
 * JSON reader, the action history) picks it up automatically. This is a
 * small but practical example of the Open/Closed Principle.</p>
 */
public final class EventFactory {

    /**
     * Prevents instantiation of this utility class.
     */
    private EventFactory() {
        throw new AssertionError();
    }

    /**
     * Returns the list of event names the UI may offer to the user.
     *
     * @return immutable list of display names
     */
    public static List<String> availableEventNames() {
        return Arrays.stream(Registry.values())
                .map(entry -> entry.displayName)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Creates a new event from its display name.
     *
     * @param name the event's display name; may be {@code null}
     * @return a fresh event instance, or {@code null} if the name is unknown
     */
    public static GameEvent fromName(String name) {
        if (name == null) {
            return null;
        }
        for (Registry entry : Registry.values()) {
            if (entry.displayName.equalsIgnoreCase(name)) {
                return entry.supplier.get();
            }
        }
        return null;
    }

    /**
     * Internal registry coupling display names with constructors.
     */
    private enum Registry {

        /** Reinforcements event entry. */
        REINFORCEMENTS("Reinforcements", ReinforcementsEvent::new),

        /** Natural disaster event entry. */
        NATURAL_DISASTER("Natural Disaster", NaturalDisasterEvent::new),

        /** Hidden weaponry event entry. */
        HIDDEN_WEAPONRY("Hidden Weaponry", HiddenWeaponryEvent::new),

        /** Plague event entry. */
        PLAGUE("Plague", PlagueEvent::new);

        /** Display name surfaced to the user. */
        private final String displayName;

        /** Factory invoked when a fresh event is requested. */
        private final Supplier<GameEvent> supplier;

        /**
         * Constructs a registry entry.
         *
         * @param displayName the user facing name
         * @param supplier the constructor reference for the event
         */
        Registry(String displayName, Supplier<GameEvent> supplier) {
            this.displayName = displayName;
            this.supplier = supplier;
        }
    }
}
