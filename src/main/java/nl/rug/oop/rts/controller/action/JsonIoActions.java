package nl.rug.oop.rts.controller.action;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.io.JsonParseException;
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
import java.util.function.Supplier;
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

    /**
     * Opens a Save chooser and returns the user's pick, or {@code null} when
     * the dialog is cancelled.
     *
     * @param owner the dialog parent
     * @param initial the file to suggest in the chooser; may be {@code null}
     * @return the chosen file, or {@code null} when cancelled
     */
    private static File promptForSaveTarget(JFrame owner, File initial) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save simulation");
        FileFilter jsonFilter = new FileNameExtensionFilter("JSON files (*.json)", "json");
        chooser.setFileFilter(jsonFilter);
        chooser.setSelectedFile(initial != null ? initial : new File("simulation.json"));
        int choice = chooser.showSaveDialog(owner);
        return choice == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    /**
     * Writes the editor's graph to {@code target}, reports the outcome, and
     * notifies {@code sink} of the file that was actually written.
     *
     * @param ctx the editor context
     * @param owner the dialog parent for feedback popups
     * @param target the file to write to
     * @param sink receives the written file on success; may be {@code null}
     */
    private static void writeAndReport(EditorContext ctx, JFrame owner,
                                       File target, Consumer<File> sink) {
        try {
            File written = new JsonWriter().saveToFile(ctx.getGraph(), target);
            if (sink != null) {
                sink.accept(written);
            }
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

    /** "To JSON" save-as action. Always enabled; always prompts for a path. */
    public static class SaveAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Supplies the parent frame for dialogs. */
        private final Supplier<JFrame> ownerSupplier;

        /** Callback invoked with the actually-written file path. */
        private final Consumer<File> currentFileSink;

        /**
         * Constructs the save-as action.
         * @param ctx the editor context
         * @param ownerSupplier supplies the parent frame for dialogs
         * @param currentFileSink receives the written file so a later "Save"
         *                        can re-use it; may be {@code null}
         */
        public SaveAction(EditorContext ctx,
                          Supplier<JFrame> ownerSupplier,
                          Consumer<File> currentFileSink) {
            super("To JSON", ctx);
            this.ownerSupplier = ownerSupplier;
            this.currentFileSink = currentFileSink;
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File target = promptForSaveTarget(ownerSupplier.get(), null);
            if (target == null) {
                return;
            }
            writeAndReport(context, ownerSupplier.get(), target, currentFileSink);
        }
    }

    /**
     * "Save" action that writes back to the file the editor is currently
     * tracking (last loaded or last saved-as). Falls back to a Save-As style
     * chooser the first time the user hits it on a fresh session.
     */
    public static class SaveCurrentAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Supplies the parent frame for dialogs. */
        private final Supplier<JFrame> ownerSupplier;

        /** Supplies the file currently being edited, or {@code null}. */
        private final Supplier<File> currentFileSource;

        /** Receives the file that was actually written. */
        private final Consumer<File> currentFileSink;

        /**
         * Constructs the in-place save action.
         *
         * @param ctx the editor context
         * @param ownerSupplier supplies the parent frame
         * @param currentFileSource current target file supplier
         * @param currentFileSink updated with the file actually written
         */
        public SaveCurrentAction(EditorContext ctx,
                                 Supplier<JFrame> ownerSupplier,
                                 Supplier<File> currentFileSource,
                                 Consumer<File> currentFileSink) {
            super("Save", ctx);
            this.ownerSupplier = ownerSupplier;
            this.currentFileSource = currentFileSource;
            this.currentFileSink = currentFileSink;
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame owner = ownerSupplier.get();
            File target = currentFileSource == null ? null : currentFileSource.get();
            if (target == null) {
                target = promptForSaveTarget(owner, null);
                if (target == null) {
                    return;
                }
            }
            writeAndReport(context, owner, target, currentFileSink);
        }
    }

    /** "From JSON" load action (bonus: loading from JSON). */
    public static class LoadAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Supplies the parent frame for dialogs. */
        private final Supplier<JFrame> ownerSupplier;

        /** Callback that swaps in a freshly loaded graph. */
        private final Consumer<Graph> graphReplacer;

        /** Callback invoked with the loaded file path. */
        private final Consumer<File> currentFileSink;

        /**
         * Constructs the load action.
         *
         * @param ctx the editor context
         * @param ownerSupplier supplies the parent frame
         * @param graphReplacer callback invoked with the loaded graph so the
         *                      application can swap models cleanly
         * @param currentFileSink receives the loaded file path so a later
         *                        "Save" can write back to it; may be {@code null}
         */
        public LoadAction(EditorContext ctx,
                          Supplier<JFrame> ownerSupplier,
                          Consumer<Graph> graphReplacer,
                          Consumer<File> currentFileSink) {
            super("From JSON", ctx);
            this.ownerSupplier = ownerSupplier;
            this.graphReplacer = graphReplacer;
            this.currentFileSink = currentFileSink;
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
            File selected = chooser.getSelectedFile();
            try {
                Graph loaded = new JsonReader().loadFromFile(selected);
                graphReplacer.accept(loaded);
                if (currentFileSink != null) {
                    currentFileSink.accept(selected);
                }
                JOptionPane.showMessageDialog(owner,
                        "Loaded.", "Load successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | JsonParseException ex) {
                LOGGER.log(Level.WARNING, "Failed to load", ex);
                JOptionPane.showMessageDialog(owner,
                        "Failed to load: " + ex.getMessage(),
                        "Load failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
