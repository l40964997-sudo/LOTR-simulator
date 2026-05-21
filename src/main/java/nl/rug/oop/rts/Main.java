package nl.rug.oop.rts;

import com.formdev.flatlaf.FlatDarculaLaf;
import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.model.command.CommandHistory;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.simulation.Simulator;
import nl.rug.oop.rts.view.panel.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point.
 * <p>
 * The {@code main} method follows the standard Swing recipe: install the
 * look and feel, build the model on the calling thread, then schedule the
 * construction of the frame on the Event Dispatch Thread via
 * {@link SwingUtilities#invokeLater}. The entry point's only job is to
 * install the theme and wire the model, history, simulator and view
 * together.
 */
public final class Main {

    /** Utility class; not instantiable. */
    private Main() {
        throw new AssertionError("Utility class");
    }

    /**
     * Launches the editor.
     *
     * @param args command line arguments (ignored)
     */
    public static void main(String[] args) {
        // Dark mode, matching the course-provided FlatLaf theme.
        FlatDarculaLaf.setup();

        Graph graph = new Graph();
        CommandHistory history = new CommandHistory();
        Simulator simulator = new Simulator(graph);
        EditorContext context = new EditorContext(graph, history, simulator);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(context);
            frame.setVisible(true);
        });
    }
}