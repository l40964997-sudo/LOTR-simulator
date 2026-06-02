package nl.rug.oop.rts.controller;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.command.AddArmyCommand;
import nl.rug.oop.rts.model.command.AddEdgeCommand;
import nl.rug.oop.rts.model.command.AddEventCommand;
import nl.rug.oop.rts.model.command.AddNodeCommand;
import nl.rug.oop.rts.model.command.CommandHistory;
import nl.rug.oop.rts.model.command.MoveNodeCommand;
import nl.rug.oop.rts.model.command.RemoveArmyCommand;
import nl.rug.oop.rts.model.command.RemoveEdgeCommand;
import nl.rug.oop.rts.model.command.RemoveEventCommand;
import nl.rug.oop.rts.model.command.RemoveNodeCommand;
import nl.rug.oop.rts.model.command.RenameCommand;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.model.observer.ModelEvent;
import nl.rug.oop.rts.model.observer.Observable;
import nl.rug.oop.rts.model.simulation.Simulator;

/**
 * Shared mutable state and command facade for the controller layer.
 * <p>
 * Two responsibilities:
 * <ol>
 *   <li>holding the "add edge mode" flag plus references to the model graph,
 *       command history, simulator and installed {@link PlayerController};</li>
 *   <li>exposing intention-revealing methods - {@link #addArmy}, {@link #rename},
 *       {@link #removeNode}, ... - so views and mouse handlers never need to
 *       import or instantiate concrete command classes themselves.</li>
 * </ol>
 * The facade collapses the {@code context.getCommandHistory().execute(new XCommand(context.getGraph(), ...))}
 * chant that view code would otherwise litter every callsite with, and keeps
 * the {@code model.command} package off the view's import list - low coupling,
 * high cohesion: this class is the single bottleneck for every undoable edit.
 */
public class EditorContext extends Observable {

    /** The model graph. */
    private final Graph graph;

    /** The command history used for all reversible edits. */
    private final CommandHistory commandHistory;

    /** The simulator. */
    private final Simulator simulator;

    /** Whether the editor is waiting for the second endpoint of a new edge. */
    private boolean addEdgeMode;

    /**
     * Constructs the context.
     *
     * @param graph the model graph
     * @param commandHistory the undo/redo history
     * @param simulator the simulator
     */
    public EditorContext(Graph graph, CommandHistory commandHistory, Simulator simulator) {
        if (graph == null || commandHistory == null || simulator == null) {
            throw new IllegalArgumentException("Constructor arguments must be non-null");
        }
        this.graph = graph;
        this.commandHistory = commandHistory;
        this.simulator = simulator;
    }

    /**
     * Returns the model graph.
     *
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Returns the command history.
     *
     * @return the history
     */
    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    /**
     * Returns the simulator.
     *
     * @return the simulator
     */
    public Simulator getSimulator() {
        return simulator;
    }

    /**
     * Installs the {@link PlayerController} the simulator should call when
     * a player-controlled army takes its turn.
     *
     * @param controller the controller; may be {@code null}
     */
    public void installPlayerController(PlayerController controller) {
        simulator.setPlayerController(controller);
    }

    /**
     * Reports whether the user has clicked "Draw Route" and the editor is
     * waiting for them to select the second endpoint.
     *
     * @return {@code true} when the flag is set
     */
    public boolean isAddEdgeMode() {
        return addEdgeMode;
    }

    /**
     * Toggles the edge-creation mode. The notifier fires regardless so
     * actions watching the context can refresh their enabled state.
     *
     * @param mode the desired mode
     */
    public void setAddEdgeMode(boolean mode) {
        if (this.addEdgeMode == mode) {
            return;
        }
        this.addEdgeMode = mode;
        notifyListeners(ModelEvent.Type.GENERIC);
    }

    /* ===================== Undo / redo helpers ===================== */

    /**
     * Reports whether a previous edit can be undone.
     *
     * @return {@code true} when {@link CommandHistory#canUndo()} is true
     */
    public boolean canUndo() {
        return commandHistory.canUndo();
    }

    /**
     * Reports whether a previously-undone edit can be re-applied.
     *
     * @return {@code true} when {@link CommandHistory#canRedo()} is true
     */
    public boolean canRedo() {
        return commandHistory.canRedo();
    }

    /**
     * Reverts the most recent undoable edit.
     */
    public void undo() {
        commandHistory.undo();
    }

    /**
     * Re-applies the most recently undone edit.
     */
    public void redo() {
        commandHistory.redo();
    }

    /* ===================== Command facade ===================== */

    /**
     * Adds a new node at the given coordinates.
     *
     * @param x x position in world coordinates
     * @param y y position in world coordinates
     */
    public void addNode(int x, int y) {
        commandHistory.execute(new AddNodeCommand(graph, x, y));
    }

    /**
     * Removes the given node and every incident edge.
     *
     * @param node the node to remove; ignored when {@code null}
     */
    public void removeNode(Node node) {
        if (node == null) {
            return;
        }
        commandHistory.execute(new RemoveNodeCommand(graph, node));
    }

    /**
     * Records a node move so it can be undone like any other edit.
     *
     * @param node the node being moved
     * @param fromX original x
     * @param fromY original y
     * @param toX new x
     * @param toY new y
     */
    public void moveNode(Node node, int fromX, int fromY, int toX, int toY) {
        if (node == null) {
            return;
        }
        commandHistory.execute(new MoveNodeCommand(graph, node, fromX, fromY, toX, toY));
    }

    /**
     * Connects two nodes with a new edge.
     *
     * @param a one endpoint
     * @param b the other endpoint
     */
    public void addEdge(Node a, Node b) {
        if (a == null || b == null || a == b) {
            return;
        }
        commandHistory.execute(new AddEdgeCommand(graph, a, b));
    }

    /**
     * Removes the given edge.
     *
     * @param edge the edge; ignored when {@code null}
     */
    public void removeEdge(Edge edge) {
        if (edge == null) {
            return;
        }
        commandHistory.execute(new RemoveEdgeCommand(graph, edge));
    }

    /**
     * Attaches an army to a node or edge.
     *
     * @param element the host element
     * @param army the army to attach
     */
    public void addArmy(MapElement element, Army army) {
        if (element == null || army == null) {
            return;
        }
        commandHistory.execute(new AddArmyCommand(graph, element, army));
    }

    /**
     * Detaches an army from a node or edge.
     *
     * @param element the host element
     * @param army the army to detach
     */
    public void removeArmy(MapElement element, Army army) {
        if (element == null || army == null) {
            return;
        }
        commandHistory.execute(new RemoveArmyCommand(graph, element, army));
    }

    /**
     * Attaches an event to a node or edge.
     *
     * @param element the host element
     * @param event the event to attach
     */
    public void addEvent(MapElement element, GameEvent event) {
        if (element == null || event == null) {
            return;
        }
        commandHistory.execute(new AddEventCommand(graph, element, event));
    }

    /**
     * Detaches an event from a node or edge.
     *
     * @param element the host element
     * @param event the event to detach
     */
    public void removeEvent(MapElement element, GameEvent event) {
        if (element == null || event == null) {
            return;
        }
        commandHistory.execute(new RemoveEventCommand(graph, element, event));
    }

    /**
     * Renames the given element to a new name.
     *
     * @param element the element to rename
     * @param newName the new name; blank/null values are filtered downstream
     */
    public void rename(MapElement element, String newName) {
        if (element == null) {
            return;
        }
        commandHistory.execute(new RenameCommand(graph, element, newName));
    }
}
