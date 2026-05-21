package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;

/**
 * Removes an army from a node or edge.
 */
public class RemoveArmyCommand implements Command {

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
     * @param target the element the army currently resides on
     * @param army the army; must not be {@code null}
     */
    public RemoveArmyCommand(Graph graph, MapElement target, Army army) {
        if (graph == null || target == null || army == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.target = target;
        this.army = army;
    }

    @Override
    public void execute() {
        target.removeArmy(army);
        graph.fireArmyChanged();
    }

    @Override
    public void undo() {
        target.addArmy(army);
        graph.fireArmyChanged();
    }

    @Override
    public String label() {
        return "Remove Army";
    }
}
