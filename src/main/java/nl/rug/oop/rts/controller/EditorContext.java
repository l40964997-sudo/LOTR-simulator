package nl.rug.oop.rts.controller;

import nl.rug.oop.rts.model.command.CommandHistory;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.observer.ModelEvent;
import nl.rug.oop.rts.model.observer.Observable;
import nl.rug.oop.rts.model.simulation.Simulator;

/**
 * Shared mutable state that the various controllers consult while
 * processing user input.
 * <p>
 * Specifically, this is where the "Add Edge" flag lives. The flag is a
 * small state machine: when the user clicks the "Draw Route" button with
 * a node selected, the flag is set; the next node click finalises the
 * edge; clicking the button again, or clicking empty space, cancels.
 * <p>
 * The context also caches the {@link PlayerController} the simulator
 * should call when a player-controlled army needs orders. Holding the
 * reference here (rather than wiring it through the simulator constructor)
 * keeps the view's setup code short - one call to
 * {@link #installPlayerController(PlayerController)} after the frame has
 * built its dialog factory.
 * <p>
 * The context exposes itself as an {@link Observable} so the button
 * action panel can grey out controls based on the current state, and so
 * the renderer can hint the user (for example by highlighting the source
 * node) that they are now in edge-creation mode.
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
     * a player-controlled army takes its turn. A {@code null} controller
     * tells the simulator to fall back to AI behaviour even for
     * player-flagged armies.
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
}
