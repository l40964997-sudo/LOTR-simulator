package nl.rug.oop.rts.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique integer identifiers for nodes and edges in the graph.
 * <p>
 * The class uses a single {@link AtomicInteger} per category so that ids are
 * monotonically increasing within the lifetime of the JVM. Although the
 * Swing application is single threaded, using atomic counters costs nothing
 * and protects against accidental misuse from worker threads (for example,
 * when extending the project with background loading or networking).
 * <p>
 * Two separate counters are kept - one for nodes and one for edges
 * - so that the ids of the two element kinds do not interfere. This
 * matches the JSON specification of the assignment, where node and edge
 * ids live in independent namespaces.
 */
public final class IdGenerator {

    /** Counter used for {@link nl.rug.oop.rts.model.graph.Node} instances. */
    private static final AtomicInteger NODE_COUNTER = new AtomicInteger(0);

    /** Counter used for {@link nl.rug.oop.rts.model.graph.Edge} instances. */
    private static final AtomicInteger EDGE_COUNTER = new AtomicInteger(0);

    /** Utility class, no instances. */
    private IdGenerator() {
        throw new AssertionError("IdGenerator is a utility class and must not be instantiated.");
    }

    /**
     * Returns the next available node id.
     *
     * @return a positive, monotonically increasing integer
     */
    public static int nextNodeId() {
        return NODE_COUNTER.getAndIncrement();
    }

    /**
     * Returns the next available edge id.
     *
     * @return a positive, monotonically increasing integer
     */
    public static int nextEdgeId() {
        return EDGE_COUNTER.getAndIncrement();
    }

    /**
     * Ensures the next node id will be at least {@code minimum}. This is
     * useful when the application loads a state from disk and wants new
     * node ids to avoid clashing with the loaded ones.
     *
     * @param minimum the value the next id must be greater than or equal to
     */
    public static void seedNodeIdAtLeast(int minimum) {
        int current;
        do {
            current = NODE_COUNTER.get();
            if (current >= minimum + 1) {
                return;
            }
        } while (!NODE_COUNTER.compareAndSet(current, minimum + 1));
    }

    /**
     * Ensures the next edge id will be at least {@code minimum}.
     *
     * @param minimum the value the next id must be greater than or equal to
     * @see #seedNodeIdAtLeast(int)
     */
    public static void seedEdgeIdAtLeast(int minimum) {
        int current;
        do {
            current = EDGE_COUNTER.get();
            if (current >= minimum + 1) {
                return;
            }
        } while (!EDGE_COUNTER.compareAndSet(current, minimum + 1));
    }

    /**
     * Resets both counters to zero. Provided for tests; should never be
     * called by the production application.
     */
    static void resetForTests() {
        NODE_COUNTER.set(0);
        EDGE_COUNTER.set(0);
    }
}
