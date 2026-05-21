package nl.rug.oop.rts.controller.action;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.io.JsonReader;
import nl.rug.oop.rts.io.JsonWriter;
import nl.rug.oop.rts.model.graph.Graph;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Actions that read or write the simulator state to disk.
 * <p>
 * Both actions interact with the user through dialogs, hence the
 * dependency on a {@link JFrame} owner. The owner is supplied lazily so
 * that the action can be wired up before the frame is realised; if
 * the supplier returns {@code null} the dialogs fall back to a non-modal
 * parent, which is acceptable for the edge case of a headless smoke test.
 */
public final class JsonIoActions {

    /** Class logger. */
    private static final Logger LOGGER = Logger.getLogger(JsonIoActions.class.getName());

    /**
     * Prevents instantiation of this action holder.
     */
    private JsonIoActions() {
        throw new AssertionError("Utility holder");
    }

    /** "To JSON" save action. Always enabled. */
    public static class SaveAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Supplies the parent frame for dialogs. */
        private final java.util.function.Supplier<JFrame> ownerSupplier;

        /**
         * Constructs the save action.
         * @param ctx the editor context
         * @param ownerSupplier supplies the parent frame for dialogs
         */
        public SaveAction(EditorContext ctx, java.util.function.Supplier<JFrame> ownerSupplier) {
            super("To JSON", ctx);
            this.ownerSupplier = ownerSupplier;
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame owner = ownerSupplier.get();
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save simulation");
            FileFilter jsonFilter = new FileNameExtensionFilter("JSON files (*.json)", "json");
            chooser.setFileFilter(jsonFilter);
            chooser.setSelectedFile(new File("simulation.json"));
            int choice = chooser.showSaveDialog(owner);
            if (choice != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File target = chooser.getSelectedFile();
            try {
                File written = new JsonWriter().saveToFile(context.getGraph(), target);
                JOptionPane.showMessageDialog(owner,
                        "Saved to: " + written.getAbsolutePath(),
                        "Save successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to save", ex);
                JOptionPane.showMessageDialog(owner,
                        "Failed to save: " + ex.getMessage(),
                        "Save failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** "From JSON" load action (bonus: loading from JSON). */
    public static class LoadAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Supplies the parent frame for dialogs. */
        private final java.util.function.Supplier<JFrame> ownerSupplier;

        /** Callback that swaps in a freshly loaded graph. */
        private final Consumer<Graph> graphReplacer;

        /**
         * Constructs the load action.
         *
         * @param ctx the editor context
         * @param ownerSupplier supplies the parent frame
         * @param graphReplacer callback invoked with the loaded graph so the
         *                      application can swap models cleanly
         */
        public LoadAction(EditorContext ctx,
                          java.util.function.Supplier<JFrame> ownerSupplier,
                          Consumer<Graph> graphReplacer) {
            super("From JSON", ctx);
            this.ownerSupplier = ownerSupplier;
            this.graphReplacer = graphReplacer;
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame owner = ownerSupplier.get();
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Load simulation");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            int choice = chooser.showOpenDialog(owner);
            if (choice != JFileChooser.APPROVE_OPTION) {
                return;
            }
            try {
                Graph loaded = new JsonReader().loadFromFile(chooser.getSelectedFile());
                graphReplacer.accept(loaded);
                JOptionPane.showMessageDialog(owner,
                        "Loaded.", "Load successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | JsonReader.JsonParseException ex) {
                LOGGER.log(Level.WARNING, "Failed to load", ex);
                JOptionPane.showMessageDialog(owner,
                        "Failed to load: " + ex.getMessage(),
                        "Load failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
