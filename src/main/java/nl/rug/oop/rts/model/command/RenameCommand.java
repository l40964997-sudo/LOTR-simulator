package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;

/**
 * Renames a {@link MapElement} (node or edge).
 * <p>
 * Records the previous name so that {@link #undo()} can restore it.
 */
public class RenameCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The node or edge this command operates on. */
    private final MapElement target;

    /** The previous name, kept for undo. */
    private final String oldName;

    /** The new name to apply. */
    private final String newName;

    /**
     * Constructs the command.
     *
     * @param graph the graph (used for notification)
     * @param target the element being renamed
     * @param newName the new name
     */
    public RenameCommand(Graph graph, MapElement target, String newName) {
        if (graph == null || target == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.target = target;
        this.oldName = target.getName();
        this.newName = newName;
    }

    @Override
    public void execute() {
        target.setName(newName);
        graph.fireStructureChanged();
    }

    @Override
    public void undo() {
        target.setName(oldName);
        graph.fireStructureChanged();
    }

    @Override
    public String label() {
        return "Rename";
    }
}
