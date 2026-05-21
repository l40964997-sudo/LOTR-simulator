package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;

/**
 * Command that adds a new node to the graph.
 * <p>
 * On the first execution the node is created via {@link Graph#addNode(int, int)},
 * which allocates a new id. Subsequent {@code execute()} calls
 * (re-applied after an undo) reinsert the same instance via
 * {@link Graph#reinsertNode(Node)} so the id and name stay stable.
 */
public class AddNodeCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The x coordinate. */
    private final int x;

    /** The y coordinate. */
    private final int y;

    /** The node created by this command, kept for undo. */
    private Node createdNode;

    /**
     * Constructs the command.
     *
     * @param graph the graph
     * @param x     x coordinate
     * @param y     y coordinate
     */
    public AddNodeCommand(Graph graph, int x, int y) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        this.graph = graph;
        this.x = x;
        this.y = y;
    }

    @Override
    public void execute() {
        if (createdNode == null) {
            createdNode = graph.addNode(x, y);
        } else {
            graph.reinsertNode(createdNode);
        }
    }

    @Override
    public void undo() {
        if (createdNode != null) {
            graph.removeNode(createdNode);
        }
    }

    @Override
    public String label() {
        return "Add Node";
    }

    /**
     * Returns the node created by this command.
     *
     * @return the newly created node (or {@code null} if execute has not yet run)
     */
    public Node getCreatedNode() {
        return createdNode;
    }
}
