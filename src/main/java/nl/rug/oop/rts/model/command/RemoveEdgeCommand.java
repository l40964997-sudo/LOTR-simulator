package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;

/**
 * Removes an edge from the graph.
 */
public class RemoveEdgeCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The edge involved in this command. */
    private final Edge edge;

    /**
     * Constructs the command.
     *
     * @param graph the graph
     * @param edge the edge to remove
     */
    public RemoveEdgeCommand(Graph graph, Edge edge) {
        if (graph == null || edge == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.edge = edge;
    }

    @Override
    public void execute() {
        graph.removeEdge(edge);
    }

    @Override
    public void undo() {
        graph.reinsertEdge(edge);
    }

    @Override
    public String label() {
        return "Remove Edge";
    }
}
