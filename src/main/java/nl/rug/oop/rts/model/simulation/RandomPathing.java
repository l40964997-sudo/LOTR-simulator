package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Node;

import java.util.List;
import java.util.Random;

/**
 * The mandatory pathing strategy: pick an outgoing edge uniformly at random.
 */
public class RandomPathing implements PathingStrategy {

    /** Random source. */
    private final Random random;

    /** Default constructor with a fresh random source. */
    public RandomPathing() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source.
     *
     * @param random the random source; must not be {@code null}
     */
    public RandomPathing(Random random) {
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
        return outgoing.get(random.nextInt(outgoing.size()));
    }
}
