package nl.rug.oop.rts.view.panel;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.controller.PlayerController;
import nl.rug.oop.rts.controller.PlayerOrder;
import nl.rug.oop.rts.controller.action.EditorActions;
import nl.rug.oop.rts.controller.action.JsonIoActions;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.command.CommandHistory;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.model.simulation.Simulator;
import nl.rug.oop.rts.util.SoundManager;
import nl.rug.oop.rts.view.dialog.PlayerOrderDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * The main application window.
 * <p>
 * The frame hosts a {@link JToolBar} with the editor actions and a
 * {@link JSplitPane} divided between the {@link GraphPanel} (left) and the
 * {@link SidePanel} (right). It is centred on screen and sized to a
 * reasonable default. The frame holds references to the model and
 * controller layers but never reaches into them beyond what its toolbar
 * needs - the actions own that responsibility.
 */
public class MainFrame extends JFrame {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Mutable so the load action can swap the underlying graph. */
    private EditorContext context;

    /** The split pane holding the graph and the side panel. */
    private final JSplitPane split;

    /** Reference to the graph panel so we can replace it on load. */
    private GraphPanel graphPanel;

    /** Reference to the side panel so we can replace it on load. */
    private SidePanel sidePanel;

    /** File the editor is currently editing; {@code null} until first save/load. */
    private File currentFile;

    /**
     * Constructs the main frame.
     *
     * @param initialContext the initial editor context
     */
    public MainFrame(EditorContext initialContext) {
        super("LoTR Battle Simulator");
        if (initialContext == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = initialContext;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension preferred = new Dimension(1200, 720);
        setPreferredSize(preferred);

        graphPanel = new GraphPanel(context);
        sidePanel = new SidePanel(context);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPanel, sidePanel);
        split.setResizeWeight(0.78);
        split.setOneTouchExpandable(true);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(buildToolBar(), BorderLayout.NORTH);

        wireSimulatorReporter();
        installPlayerController();
        SoundManager.getInstance().startPlaylist();

        pack();
        // Centre on the primary display.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2,
                Math.max(0, (screen.height - getHeight()) / 2));
    }

    /**
     * Builds the top toolbar.
     *
     * @return the assembled toolbar
     */
    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(new JButton(new EditorActions.AddNodeAction(context)));
        bar.add(new JButton(new EditorActions.RemoveNodeAction(context)));
        bar.add(new JButton(new EditorActions.StartAddEdgeAction(context)));
        bar.add(new JButton(new EditorActions.RemoveEdgeAction(context)));
        bar.addSeparator();
        bar.add(new JButton(new EditorActions.SimulateStepAction(context)));
        bar.addSeparator();
        bar.add(new JButton(new EditorActions.UndoAction(context)));
        bar.add(new JButton(new EditorActions.RedoAction(context)));
        bar.addSeparator();
        bar.add(new JButton(new JsonIoActions.SaveCurrentAction(
                context, this::self, this::getCurrentFile, this::setCurrentFile)));
        bar.add(new JButton(new JsonIoActions.SaveAction(
                context, this::self, this::setCurrentFile)));
        bar.add(new JButton(new JsonIoActions.LoadAction(
                context, this::self, this::replaceGraph, this::setCurrentFile)));
        bar.addSeparator();
        bar.add(buildMusicToggle());
        bar.add(buildNextTrackButton());
        bar.add(buildShuffleToggle());
        return bar;
    }

    /**
     * Builds the music-on/off toggle for the toolbar.
     *
     * @return the configured toggle button
     */
    private JToggleButton buildMusicToggle() {
        JToggleButton toggle = new JToggleButton("Music On", true);
        toggle.addActionListener(e -> {
            if (toggle.isSelected()) {
                SoundManager.getInstance().startPlaylist();
                toggle.setText("Music On");
            } else {
                SoundManager.getInstance().stopMusic();
                toggle.setText("Music Off");
            }
        });
        return toggle;
    }

    /**
     * Builds the "Next Track" button used to skip ahead in the playlist.
     *
     * @return the configured button
     */
    private JButton buildNextTrackButton() {
        JButton next = new JButton("Next Track");
        next.addActionListener(e -> SoundManager.getInstance().nextTrack());
        return next;
    }

    /**
     * Builds the shuffle toggle for the music playlist.
     *
     * @return the configured toggle button
     */
    private JToggleButton buildShuffleToggle() {
        JToggleButton toggle = new JToggleButton("Shuffle Off", false);
        toggle.addActionListener(e -> {
            boolean on = toggle.isSelected();
            SoundManager.getInstance().setShuffle(on);
            toggle.setText(on ? "Shuffle On" : "Shuffle Off");
        });
        return toggle;
    }

    /**
     * Supplies this frame as the dialog owner for the IO actions.
     *
     * @return this frame
     */
    private JFrame self() {
        return this;
    }

    /**
     * Returns the file currently being edited, or {@code null} when the
     * editor is still working on an unsaved fresh session.
     *
     * @return the tracked file, or {@code null}
     */
    private File getCurrentFile() {
        return currentFile;
    }

    /**
     * Records the file the editor is now tracking. Called after a successful
     * save or load so the in-place "Save" button knows where to write next.
     *
     * @param file the file to remember
     */
    private void setCurrentFile(File file) {
        this.currentFile = file;
    }

    /**
     * Swaps the entire editor context to host a newly loaded graph.
     *
     * @param loaded the freshly deserialised graph
     */
    private void replaceGraph(Graph loaded) {
        if (loaded == null) {
            return;
        }
        Simulator simulator = new Simulator(loaded);
        CommandHistory history = new CommandHistory();
        EditorContext newContext = new EditorContext(loaded, history, simulator);
        this.context = newContext;
        // Replace panels in place.
        graphPanel = new GraphPanel(newContext);
        sidePanel = new SidePanel(newContext);
        split.setLeftComponent(graphPanel);
        split.setRightComponent(sidePanel);
        getContentPane().remove(((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.NORTH));
        getContentPane().add(buildToolBar(), BorderLayout.NORTH);
        wireSimulatorReporter();
        revalidate();
        repaint();
    }

    /**
     * Installs the player controller; it pops {@link PlayerOrderDialog}
     * whenever the simulator needs orders for a player army.
     */
    private void installPlayerController() {
        PlayerController controller = new PlayerController() {
            @Override
            public PlayerOrder requestOrder(Army army, Node currentNode) {
                PlayerOrderDialog dialog = new PlayerOrderDialog(MainFrame.this, army, currentNode);
                dialog.setVisible(true);
                return dialog.getResult();
            }
        };
        context.installPlayerController(controller);
    }

    /**
     * Wires the current simulator's reporter to a popup-based sink.
     */
    private void wireSimulatorReporter() {
        // Each step may produce multiple report strings; we batch them with
        // a StringBuilder so the user gets one popup per step rather than
        // many. The reporter is reset for each step via a tiny stateful
        // wrapper.
        context.getSimulator().setReporter(new BatchedReporter(this));
    }

    /**
     * Small {@link java.util.function.Consumer} that batches consecutive
     * report strings into a single dialog per simulation step.
     */
    private static final class BatchedReporter implements java.util.function.Consumer<String> {
        /** Weak reference to the owning frame. */
        private final java.lang.ref.WeakReference<MainFrame> ownerRef;

        /** Accumulates report text for one batched dialog. */
        private final StringBuilder buffer = new StringBuilder();

        /** Whether a flush has already been scheduled. */
        private boolean scheduled;

        /**
         * Constructs the reporter bound to its owning frame.
         *
         * @param owner the frame used as the dialog parent
         */
        BatchedReporter(MainFrame owner) {
            this.ownerRef = new java.lang.ref.WeakReference<>(owner);
        }

        @Override
        public void accept(String s) {
            if (s == null || s.isBlank()) {
                return;
            }
            if (buffer.length() > 0) {
                buffer.append('\n');
            }
            buffer.append(s);
            if (!scheduled) {
                scheduled = true;
                SwingUtilities.invokeLater(this::flush);
            }
        }

        /**
         * Shows the accumulated report text in a single dialog.
         */
        private void flush() {
            scheduled = false;
            MainFrame owner = ownerRef.get();
            if (owner == null) {
                buffer.setLength(0);
                return;
            }
            String text = buffer.toString();
            buffer.setLength(0);
            if (!text.isBlank()) {
                showStepReport(owner, text);
            }
        }

        /**
         * Shows a step report in a dark-themed, centered, larger-font dialog.
         *
         * @param owner the parent frame
         * @param text the narrative text to display
         */
        private static void showStepReport(JFrame owner, String text) {
            JDialog dialog = new JDialog(owner, "", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel content = new JPanel(new BorderLayout(0, 12));
            content.setBackground(Color.BLACK);
            content.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
            content.add(buildScrollableLabel(text), BorderLayout.CENTER);

            JButton ok = buildOkButton(dialog);
            JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            buttonRow.setBackground(Color.BLACK);
            buttonRow.add(ok);
            content.add(buttonRow, BorderLayout.SOUTH);

            dialog.setContentPane(content);
            dialog.pack();
            Dimension size = dialog.getSize();
            dialog.setSize(Math.min(size.width, 640), Math.min(size.height, 480));
            dialog.setLocationRelativeTo(owner);
            dialog.getRootPane().setDefaultButton(ok);
            dialog.setVisible(true);
        }

        /**
         * Builds the centered, white-on-black narrative label wrapped in a
         * borderless scroll pane so long reports remain scrollable.
         *
         * @param text the narrative text
         * @return the scroll pane containing the styled label
         */
        private static JScrollPane buildScrollableLabel(String text) {
            String body = escapeHtml(text).replace("\n", "<br>");
            JLabel label = new JLabel(
                    "<html><div style='text-align:center;'>" + body + "</div></html>");
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 18f));

            JScrollPane scroll = new JScrollPane(label);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setBackground(Color.BLACK);
            scroll.getViewport().setBackground(Color.BLACK);
            return scroll;
        }

        /**
         * Builds the OK button styled to match the dark dialog theme.
         *
         * @param dialog the parent dialog disposed on click
         * @return the styled button
         */
        private static JButton buildOkButton(JDialog dialog) {
            JButton ok = new JButton("OK");
            ok.setFocusPainted(false);
            ok.setBackground(Color.BLACK);
            ok.setForeground(Color.WHITE);
            ok.setFont(ok.getFont().deriveFont(Font.PLAIN, 15f));
            ok.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 1),
                    BorderFactory.createEmptyBorder(6, 22, 6, 22)));
            ok.setContentAreaFilled(false);
            ok.setOpaque(true);
            ok.addActionListener(e -> dialog.dispose());
            return ok;
        }

        /**
         * Minimal HTML escape so user-supplied narrative text renders safely
         * inside the centered JLabel.
         *
         * @param s raw text
         * @return HTML-safe text
         */
        private static String escapeHtml(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }
    }
}
