package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Node;

import java.util.List;

/**
 * Picks the next edge an army will traverse from its current node.
 * <p>
 * The default {@link RandomPathing} strategy chooses uniformly at random
 * among the outgoing edges, matching the assignment specification. An
 * alternative implementation could prefer edges that lead towards enemy
 * armies (a simple A&#42; on the graph) - this is exactly the
 * sort of extension the assignment hints at under "more sophisticated
 * pathing".
 */
public interface PathingStrategy {

    /**
     * Selects the edge along which {@code army} will move.
     *
     * @param army the army being routed; must not be {@code null}
     * @param currentNode the node the army is on
     * @param outgoing the candidate edges (every edge incident to
     *                 {@code currentNode}); never {@code null} but may
     *                 be empty
     * @return the chosen edge, or {@code null} if {@code outgoing} is empty
     */
    Edge selectNextEdge(Army army, Node currentNode, List<Edge> outgoing);
}
