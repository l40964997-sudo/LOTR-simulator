package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;

/**
 * Records the move of a node to a new position so that it can be undone.
 * <p>
 * The command remembers both the start and end coordinates. It only fires
 * a structure-change notification on undo/redo because the actual drag is
 * already animating the view via the mouse controller in real time.
 */
public class MoveNodeCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The node involved in this command. */
    private final Node node;

    /** The original x coordinate. */
    private final int fromX;

    /** The original y coordinate. */
    private final int fromY;

    /** The target x coordinate. */
    private final int toX;

    /** The target y coordinate. */
    private final int toY;

    /**
     * Constructs the command.
     *
     * @param graph the graph (used for notification)
     * @param node the node that moved
     * @param fromX original x
     * @param fromY original y
     * @param toX   new x
     * @param toY   new y
     */
    public MoveNodeCommand(Graph graph, Node node,
                           int fromX, int fromY, int toX, int toY) {
        if (graph == null || node == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.node = node;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    @Override
    public void execute() {
        node.setPosition(toX, toY);
        graph.fireStructureChanged();
    }

    @Override
    public void undo() {
        node.setPosition(fromX, fromY);
        graph.fireStructureChanged();
    }

    @Override
    public String label() {
        return "Move Node";
    }
}
