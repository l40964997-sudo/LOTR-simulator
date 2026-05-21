package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;

/**
 * Removes a {@link GameEvent} from a node or edge.
 */
public class RemoveEventCommand implements Command {

    /** The graph, used to fire change notifications. */
    private final Graph graph;

    /** The node or edge this command operates on. */
    private final MapElement target;

    /** The event involved in this command. */
    private final GameEvent event;

    /**
     * Constructs the command.
     *
     * @param graph the graph
     * @param target the element from which to remove the event
     * @param event the event; must not be {@code null}
     */
    public RemoveEventCommand(Graph graph, MapElement target, GameEvent event) {
        if (graph == null || target == null || event == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        this.graph = graph;
        this.target = target;
        this.event = event;
    }

    @Override
    public void execute() {
        target.removeEvent(event);
        graph.fireEventChanged();
    }

    @Override
    public void undo() {
        target.addEvent(event);
        graph.fireEventChanged();
    }

    @Override
    public String label() {
        return "Remove Event";
    }
}
