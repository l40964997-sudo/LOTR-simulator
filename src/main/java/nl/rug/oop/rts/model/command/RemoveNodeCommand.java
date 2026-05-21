package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes a node from the graph, also removing every incident edge. The
 * command remembers the removed edges so that undo can put them back.
 */
public class RemoveNodeCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The node involved in this command. */
    private final Node node;

    /** The incident edges removed, kept so undo can restore them. */
    private List<Edge> removedEdges;

    /**
     * Constructs the command.
     *
     * @param graph the graph
     * @param node  the node to remove (must currently be in the graph)
     */
    public RemoveNodeCommand(Graph graph, Node node) {
        if (graph == null || node == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.node = node;
    }

    @Override
    public void execute() {
        removedEdges = new ArrayList<>(graph.removeNode(node));
    }

    @Override
    public void undo() {
        graph.reinsertNode(node);
        if (removedEdges != null) {
            for (Edge e : removedEdges) {
                graph.reinsertEdge(e);
            }
        }
    }

    @Override
    public String label() {
        return "Remove Node";
    }
}
