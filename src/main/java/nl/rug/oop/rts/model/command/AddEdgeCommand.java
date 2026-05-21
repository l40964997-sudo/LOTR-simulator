package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;

/**
 * Adds an undirected edge between two existing nodes.
 */
public class AddEdgeCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The first endpoint node. */
    private final Node a;

    /** The second endpoint node. */
    private final Node b;

    /** The edge created by this command, kept for undo. */
    private Edge createdEdge;

    /**
     * Constructs the command.
     *
     * @param graph the graph
     * @param a one endpoint
     * @param b the other endpoint
     */
    public AddEdgeCommand(Graph graph, Node a, Node b) {
        if (graph == null || a == null || b == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.a = a;
        this.b = b;
    }

    @Override
    public void execute() {
        if (createdEdge == null) {
            createdEdge = graph.addEdge(a, b);
        } else {
            graph.reinsertEdge(createdEdge);
        }
    }

    @Override
    public void undo() {
        if (createdEdge != null) {
            graph.removeEdge(createdEdge);
        }
    }

    @Override
    public String label() {
        return "Add Edge";
    }

    /**
     * Returns the edge created by this command.
     *
     * @return the new edge, or {@code null} if creation failed or has not yet run
     */
    public Edge getCreatedEdge() {
        return createdEdge;
    }
}
