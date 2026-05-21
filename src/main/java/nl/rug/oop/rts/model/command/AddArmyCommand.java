package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;

/**
 * Adds an army to a node or edge.
 */
public class AddArmyCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The node or edge this command operates on. */
    private final MapElement target;

    /** The army involved in this command. */
    private final Army army;

    /**
     * Constructs the command.
     *
     * @param graph the graph (used for notification)
     * @param target the node or edge to receive the army
     * @param army the army; must not be {@code null}
     */
    public AddArmyCommand(Graph graph, MapElement target, Army army) {
        if (graph == null || target == null || army == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.target = target;
        this.army = army;
    }

    @Override
    public void execute() {
        target.addArmy(army);
        graph.fireArmyChanged();
    }

    @Override
    public void undo() {
        target.removeArmy(army);
        graph.fireArmyChanged();
    }

    @Override
    public String label() {
        return "Add Army";
    }
}
