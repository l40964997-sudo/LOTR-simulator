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
        SoundManager.getInstance().play(SoundManager.Effect.AMBIENT);
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
        bar.add(new JButton(new JsonIoActions.SaveAction(context, this::self)));
        bar.add(new JButton(new JsonIoActions.LoadAction(context, this::self, this::replaceGraph)));
        bar.addSeparator();
        bar.add(buildSoundToggle());
        bar.add(buildMusicToggle());
        bar.add(buildNextTrackButton());
        bar.add(buildShuffleToggle());
        return bar;
    }

    /**
     * Builds the sound-on/off toggle for the toolbar.
     *
     * @return the configured toggle button
     */
    private JToggleButton buildSoundToggle() {
        JToggleButton toggle = new JToggleButton("Sound On", SoundManager.getInstance().isEnabled());
        toggle.addActionListener(e -> {
            boolean on = toggle.isSelected();
            SoundManager.getInstance().setEnabled(on);
            toggle.setText(on ? "Sound On" : "Sound Off");
            if (on) {
                // Audible confirmation that the audio pipeline works.
                SoundManager.getInstance().play(SoundManager.Effect.BATTLE_HORN);
            }
        });
        return toggle;
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
                JOptionPane.showMessageDialog(owner, text,
                        "Step report", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
