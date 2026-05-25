package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Sophisticated pathing strategy that adapts to the army's situation.
 * <p>
 * The strategy makes a tactical decision in three steps:
 * <ol>
 *   <li>BFS the graph from the army's current node to gather every reachable
 *       node along with the first edge that begins the shortest path to it.</li>
 *   <li>Score each candidate by a weighted blend of distance, the target's
 *       combat power and whether the target node is held by an ally
 *       (reinforcement) or by an enemy (engagement). A weaker enemy is
 *       preferred over a stronger one; a stronger enemy is avoided.</li>
 *   <li>Pick the best-scoring candidate; if everything is equally bad, fall
 *       back to a uniform random edge so the army still moves.</li>
 * </ol>
 * The behaviour is deterministic relative to the supplied {@link Random}
 * source, which makes it predictable for tests but lively in normal play.
 */
public class SmartPathing implements PathingStrategy {

    /** Multiplier on distance when scoring engagements. */
    private static final double DISTANCE_WEIGHT = 4.0;

    /** Bonus for picking a target where allied armies already wait. */
    private static final double ALLY_BONUS = 6.0;

    /** Threshold ratio under which we consider an enemy too strong to engage. */
    private static final double RETREAT_RATIO = 1.4;

    /** Random source used as a tie breaker. */
    private final Random random;

    /**
     * Default constructor.
     */
    public SmartPathing() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source.
     *
     * @param random the random source; must not be {@code null}
     */
    public SmartPathing(Random random) {
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
        ScoutResult scout = scout(currentNode);
        Edge bestEdge = pickBestEdge(army, outgoing, scout);
        if (bestEdge != null) {
            return bestEdge;
        }
        return outgoing.get(random.nextInt(outgoing.size()));
    }

    /**
     * Selects the highest-scoring outgoing edge.
     *
     * @param army the army to be routed
     * @param outgoing the candidate edges
     * @param scout the BFS result starting from the army's node
     * @return the chosen edge, or {@code null} when no positive choice exists
     */
    private Edge pickBestEdge(Army army, List<Edge> outgoing, ScoutResult scout) {
        Edge bestEdge = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Node, Integer> entry : scout.distance.entrySet()) {
            double score = scoreTarget(army, entry.getKey(), entry.getValue());
            Edge step = scout.firstEdge.get(entry.getKey());
            if (step != null && outgoing.contains(step) && score > bestScore) {
                bestScore = score;
                bestEdge = step;
            }
        }
        return bestScore > 0 ? bestEdge : null;
    }

    /**
     * Computes a tactical score for a candidate destination node.
     *
     * @param me the army doing the routing
     * @param target the candidate destination
     * @param distance the number of edges between the army and the target
     * @return higher is better; non-positive means "don't go there"
     */
    private double scoreTarget(Army me, Node target, int distance) {
        double myPower = me.combatPower() + 1.0;
        double enemyPower = 0.0;
        double allyPower = 0.0;
        for (Army other : target.getArmies()) {
            if (other == me) {
                continue;
            }
            if (other.getTeam() == me.getTeam()) {
                allyPower += other.combatPower();
            } else {
                enemyPower += other.combatPower();
            }
        }
        if (enemyPower > 0) {
            return scoreEngagement(myPower, enemyPower, distance);
        }
        if (allyPower > 0) {
            return ALLY_BONUS - distance * DISTANCE_WEIGHT * 0.25;
        }
        // Empty node: a small exploration bonus, decaying with distance.
        return 1.0 - distance * 0.3;
    }

    /**
     * Scores an engagement based on the relative combat power of the parties.
     *
     * @param myPower the army's combat power
     * @param enemyPower aggregate enemy combat power at the target
     * @param distance steps to the target
     * @return the engagement score
     */
    private double scoreEngagement(double myPower, double enemyPower, int distance) {
        double ratio = myPower / enemyPower;
        if (ratio >= RETREAT_RATIO) {
            // Strong enough to overpower the enemy: actively hunt them.
            return 10.0 + ratio - distance * DISTANCE_WEIGHT * 0.1;
        }
        if (ratio >= 1.0 / RETREAT_RATIO) {
            // Even fight: prefer to engage but slightly cautious.
            return 4.0 - distance * DISTANCE_WEIGHT * 0.2;
        }
        // Too strong; retreat - never returns a positive score.
        return -10.0 + distance * 0.5;
    }

    /**
     * BFS the graph from {@code start}, recording the distance to each
     * reachable node and the first edge on the shortest path to it.
     *
     * @param start the BFS root
     * @return the populated scout result
     */
    private ScoutResult scout(Node start) {
        ScoutResult result = new ScoutResult();
        Set<Node> visited = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        visited.add(start);
        for (Edge e : start.getEdges()) {
            Node next = e.opposite(start);
            if (visited.add(next)) {
                result.firstEdge.put(next, e);
                result.distance.put(next, 1);
                queue.add(next);
            }
        }
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            int dist = result.distance.get(n);
            for (Edge e : n.getEdges()) {
                Node next = e.opposite(n);
                if (visited.add(next)) {
                    result.firstEdge.put(next, result.firstEdge.get(n));
                    result.distance.put(next, dist + 1);
                    queue.add(next);
                }
            }
        }
        return result;
    }

    /** Bundles the BFS outputs: distance and first edge per reachable node. */
    private static final class ScoutResult {
        /** First edge to take on the shortest path to each reachable node. */
        private final Map<Node, Edge> firstEdge = new HashMap<>();
        /** Number of edges on the shortest path from the start to each node. */
        private final Map<Node, Integer> distance = new HashMap<>();

        /**
         * Constructs an empty result.
         */
        private ScoutResult() {
            // Empty - populated by the BFS in the enclosing class.
        }

        /**
         * Returns the BFS-derived candidate nodes as a fresh list.
         *
         * @return a snapshot of the discovered nodes
         */
        @SuppressWarnings("unused")
        private List<Node> candidates() {
            return new ArrayList<>(distance.keySet());
        }
    }
}
