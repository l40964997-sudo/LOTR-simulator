package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Node;

import java.util.*;

/**
 * Pathing strategy that biases armies toward the nearest enemy.
 * <p>
 * Implemented via a breadth-first search from the army's current node;
 * the first encountered node that hosts at least one enemy army determines
 * the direction. When no enemies are reachable the strategy degrades to
 * uniform random selection, ensuring the behaviour is well defined for
 * every possible map state.
 * <p>
 * Provided as the bonus "more sophisticated pathing" option from the
 * assignment.
 */
public class HuntingPathing implements PathingStrategy {

    /** Random source used as a fallback and tie breaker. */
    private final Random random;

    /** Default constructor. */
    public HuntingPathing() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source.
     *
     * @param random the random source; must not be {@code null}
     */
    public HuntingPathing(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    @Override
    public Edge selectNextEdge(Army army, Node currentNode, List<Edge> outgoing) {
        if (outgoing == null || outgoing.isEmpty()) {
            return null;
        }
        if (army == null || currentNode == null) {
            return outgoing.get(random.nextInt(outgoing.size()));
        }
        Edge towardsEnemy = findFirstStepTowardsEnemy(army, currentNode);
        if (towardsEnemy != null && outgoing.contains(towardsEnemy)) {
            return towardsEnemy;
        }
        return outgoing.get(random.nextInt(outgoing.size()));
    }

    /**
     * BFS from {@code start} that returns the first edge of the shortest
     * path to a node hosting an enemy army.
     *
     * @return the first edge to take, or {@code null} when no enemy is reachable
     */
    private Edge findFirstStepTowardsEnemy(Army me, Node start) {
        Set<Node> visited = new HashSet<>();
        Map<Node, Edge> firstEdge = new HashMap<>();
        Deque<Node> queue = new ArrayDeque<>();
        visited.add(start);
        for (Edge e : start.getEdges()) {
            Node next = e.opposite(start);
            if (visited.add(next)) {
                firstEdge.put(next, e);
                queue.add(next);
            }
        }
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (hasEnemy(me, n)) {
                return firstEdge.get(n);
            }
            for (Edge e : n.getEdges()) {
                Node next = e.opposite(n);
                if (visited.add(next)) {
                    firstEdge.put(next, firstEdge.get(n));
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /**
     * Reports whether a node hosts at least one enemy army.
     *
     * @param me the reference army
     * @param node the node to inspect
     * @return {@code true} if an opposing-team army is present
     */
    private boolean hasEnemy(Army me, Node node) {
        List<Army> here = new ArrayList<>(node.getArmies());
        for (Army a : here) {
            if (a.getTeam() != me.getTeam()) {
                return true;
            }
        }
        return false;
    }
}
