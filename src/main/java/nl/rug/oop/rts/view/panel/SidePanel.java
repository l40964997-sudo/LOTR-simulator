package nl.rug.oop.rts.view.panel;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.ArmyFactory;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.event.EventFactory;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.MapElement;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.util.TextureLoader;
import nl.rug.oop.rts.view.dialog.ArmyBuilderDialog;
import nl.rug.oop.rts.view.dialog.UnitInfoDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The right-hand options menu shown alongside the graph editor.
 *
 * <p>The panel observes the model and rebuilds its contents whenever the
 * selection changes: a placeholder when nothing is selected, a node menu,
 * or an edge menu. Rebuilding the whole subtree on each change is simpler
 * than diff-based updates and the panel is small enough that the cost is
 * negligible.</p>
 */
public class SidePanel extends JPanel {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Header colour for section titles. */
    private static final Color HEADER_COLOR = new Color(0xF7B538);

    /** Label for the add button. */
    private static final String ADD_LABEL = "+";

    /** Label for the remove button (minus sign). */
    private static final String REMOVE_LABEL = "\u2212";

    /** Label for the details button. */
    private static final String DETAILS_LABEL = "Details";

    /** Label for the custom-build button. */
    private static final String BUILD_LABEL = "Build...";

    /** Label for the player-control toggle. */
    private static final String CONTROL_LABEL = "Take Control";

    /** Label shown on the control toggle when an army is player controlled. */
    private static final String RELEASE_LABEL = "Release";

    /** Pixel size for the faction emblems shown in the section banner. */
    private static final int BANNER_SPRITE = 36;

    /** Pixel size for the fortress sprite shown next to a held location. */
    private static final int BANNER_FORTRESS = 56;

    /** The editor context backing this panel. */
    private final EditorContext context;

    /**
     * Constructs the side panel and subscribes it to the model.
     *
     * @param context the editor context; must not be {@code null}
     */
    public SidePanel(EditorContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(260, 600));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(new Color(0x1F2933));
        rebuild();
        context.getGraph().addListener(event -> SwingUtilities.invokeLater(this::rebuild));
        context.addListener(event -> SwingUtilities.invokeLater(this::rebuild));
    }

    /**
     * Rebuilds the contents based on the current selection.
     */
    private void rebuild() {
        removeAll();
        Node selectedNode = context.getGraph().getSelectedNode();
        Edge selectedEdge = context.getGraph().getSelectedEdge();
        if (selectedNode != null) {
            add(buildNodeMenu(selectedNode), BorderLayout.CENTER);
        } else if (selectedEdge != null) {
            add(buildEdgeMenu(selectedEdge), BorderLayout.CENTER);
        } else {
            add(buildEmpty(), BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    /**
     * Builds the placeholder shown when nothing is selected.
     *
     * @return the placeholder component
     */
    private Component buildEmpty() {
        JPanel panel = makeSectionPanel();
        JLabel label = new JLabel("Nothing selected.");
        label.setForeground(Color.LIGHT_GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        JLabel hint = new JLabel("<html><i>Click a location or route<br>to see its details.</i></html>");
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(6));
        panel.add(hint);
        return panel;
    }

    /**
     * Builds the menu shown when a node is selected.
     *
     * @param node the selected node
     * @return the node menu component
     */
    private Component buildNodeMenu(Node node) {
        JPanel panel = makeSectionPanel();
        panel.add(makeHeader("Location"));
        panel.add(makeFactionBanner(node, true));
        panel.add(makeNameEditor(node));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeEventsSection(node));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeArmiesSection(node));
        panel.add(Box.createVerticalGlue());
        return new JScrollPane(panel);
    }

    /**
     * Builds the menu shown when an edge is selected.
     *
     * @param edge the selected edge
     * @return the edge menu component
     */
    private Component buildEdgeMenu(Edge edge) {
        JPanel panel = makeSectionPanel();
        panel.add(makeHeader("Route"));
        panel.add(makeFactionBanner(edge, false));
        panel.add(makeNameEditor(edge));
        panel.add(Box.createVerticalStrut(4));
        panel.add(makeEndpointLabel("From: " + edge.getNodeA().getName()));
        panel.add(makeEndpointLabel("To: " + edge.getNodeB().getName()));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeEventsSection(edge));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeArmiesSection(edge));
        panel.add(Box.createVerticalGlue());
        return new JScrollPane(panel);
    }

    /**
     * Creates a left-aligned light grey endpoint label.
     *
     * @param text the label text
     * @return the configured label
     */
    private JLabel makeEndpointLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Creates an empty, transparent, vertically stacked panel.
     *
     * @return the panel
     */
    private JPanel makeSectionPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * Creates a section header label.
     *
     * @param text the header text
     * @return the configured label
     */
    private JLabel makeHeader(String text) {
        JLabel header = new JLabel(text);
        header.setForeground(HEADER_COLOR);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    /**
     * Builds a text field that renames the element on focus loss.
     *
     * @param element the element to rename
     * @return the configured text field
     */
    private Component makeNameEditor(MapElement element) {
        JTextField field = new JTextField(element.getName());
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.addFocusListener(new RenameOnFocusLoss(element, field));
        return field;
    }

    /**
     * Builds a banner showing the larger faction sprites for every army at
     * the element, plus a fortress sprite when a single faction holds the
     * location. Returns a small placeholder strip when nothing is present so
     * the surrounding layout stays consistent.
     *
     * @param element the selected node or edge
     * @param includeFortress whether to draw the fortress sprite for held nodes
     * @return the banner component, left-aligned
     */
    private Component makeFactionBanner(MapElement element, boolean includeFortress) {
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        banner.setOpaque(false);
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, BANNER_FORTRESS + 12));
        if (element.getArmies().isEmpty()) {
            JLabel empty = new JLabel("(no armies here)");
            empty.setForeground(Color.GRAY);
            banner.add(empty);
            return banner;
        }
        if (includeFortress) {
            Faction holder = soleHolder(element);
            if (holder != null) {
                banner.add(makeSprite("fortress" + holder.textureKey(), BANNER_FORTRESS));
            }
        }
        for (Faction faction : presentFactions(element)) {
            banner.add(makeSprite("faction" + faction.textureKey(), BANNER_SPRITE));
        }
        return banner;
    }

    /**
     * Returns the distinct factions present at the element, in insertion
     * order so the banner is stable across rebuilds.
     *
     * @param element the selected element
     * @return an ordered set of factions present
     */
    private Set<Faction> presentFactions(MapElement element) {
        Set<Faction> seen = new LinkedHashSet<>();
        for (Army army : element.getArmies()) {
            seen.add(army.getFaction());
        }
        return seen;
    }

    /**
     * Returns the single faction holding the element, or {@code null} when
     * the location is empty or contested by multiple factions/teams.
     *
     * @param element the selected element
     * @return the lone holder, or {@code null}
     */
    private Faction soleHolder(MapElement element) {
        Set<Faction> factions = presentFactions(element);
        return factions.size() == 1 ? factions.iterator().next() : null;
    }

    /**
     * Loads a texture by key and wraps it in a {@code JLabel} sized for the
     * banner. The texture loader handles caching, so requesting the same
     * sprite repeatedly is cheap.
     *
     * @param key the texture key (e.g. {@code "factionMen"})
     * @param size the side length in pixels
     * @return a label rendering the sprite
     */
    private JLabel makeSprite(String key, int size) {
        Image image = TextureLoader.getInstance().getTexture(key, size, size);
        // Force synchronous load: getScaledInstance returns asynchronously.
        ImageIcon icon = new ImageIcon(image);
        JLabel label = new JLabel(icon);
        label.setPreferredSize(new Dimension(size, size));
        return label;
    }

    /**
     * Builds the events section for a node or edge.
     *
     * @param element the element whose events are shown
     * @return the section component
     */
    private Component makeEventsSection(MapElement element) {
        JPanel panel = makeSectionPanel();
        panel.add(makeSubHeader("Events"));
        JList<String> list = makeList(eventModel(element), 4, 90);
        panel.add(new JScrollPane(list));
        JButton add = new JButton(ADD_LABEL);
        JButton remove = new JButton(REMOVE_LABEL);
        add.addActionListener(action -> promptAddEvent(element));
        remove.addActionListener(action -> removeSelectedEvent(element, list.getSelectedIndex()));
        panel.add(makeButtonRow(add, remove));
        return panel;
    }

    /**
     * Builds the armies section for a node or edge.
     *
     * @param element the element whose armies are shown
     * @return the section component
     */
    private Component makeArmiesSection(MapElement element) {
        JPanel panel = makeSectionPanel();
        panel.add(makeSubHeader("Armies"));
        JList<String> list = makeList(armyModel(element), 5, 110);
        panel.add(new JScrollPane(list));
        JButton add = new JButton(ADD_LABEL);
        JButton build = new JButton(BUILD_LABEL);
        JButton remove = new JButton(REMOVE_LABEL);
        JButton details = new JButton(DETAILS_LABEL);
        JButton control = new JButton(CONTROL_LABEL);
        add.addActionListener(action -> promptAddArmy(element));
        build.addActionListener(action -> promptBuildArmy(element));
        remove.addActionListener(action -> removeSelectedArmy(element, list.getSelectedIndex()));
        details.addActionListener(action -> showArmyDetails(element, list.getSelectedIndex()));
        control.addActionListener(action -> toggleControl(element, list.getSelectedIndex()));
        panel.add(makeButtonRow(add, build, remove));
        panel.add(makeButtonRow(details, control));
        return panel;
    }

    /**
     * Builds the list model of event names for an element. The label
     * combines the name and a short hint of the effect so the user can
     * tell events apart at a glance.
     *
     * @param element the element
     * @return a populated list model
     */
    private DefaultListModel<String> eventModel(MapElement element) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (GameEvent event : element.getEvents()) {
            String desc = event.getDescription();
            if (desc.length() > 40) {
                desc = desc.substring(0, 38) + "...";
            }
            model.addElement(event.getName() + " - " + desc);
        }
        return model;
    }

    /**
     * Builds the list model of army summaries for an element.
     *
     * @param element the element
     * @return a populated list model
     */
    private DefaultListModel<String> armyModel(MapElement element) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Army army : element.getArmies()) {
            String tag = army.isPlayerControlled() ? " [you]" : "";
            String action = army.getLastAction().isEmpty() ? "" : " - " + army.getLastAction();
            model.addElement(army.getName() + " (" + army.size() + ")" + tag + action);
        }
        return model;
    }

    /**
     * Creates a configured list with a fixed visible row count.
     *
     * @param model the list model
     * @param rows the number of visible rows
     * @param maxHeight the maximum height of the enclosing scroll area
     * @return the configured list
     */
    private JList<String> makeList(DefaultListModel<String> model, int rows, int maxHeight) {
        JList<String> list = new JList<>(model);
        list.setVisibleRowCount(rows);
        list.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight));
        return list;
    }

    /**
     * Creates a left-aligned transparent row holding the given buttons.
     *
     * @param buttons the buttons to add
     * @return the button row panel
     */
    private JPanel makeButtonRow(JButton... buttons) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    /**
     * Creates a white left-aligned sub-header label.
     *
     * @param text the header text
     * @return the configured label
     */
    private JLabel makeSubHeader(String text) {
        JLabel header = new JLabel(text);
        header.setForeground(Color.WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    /**
     * Prompts the user to choose an event and adds it to the element.
     * Renders each option as {@code "Name - description"} so the user
     * sees exactly what each event does before picking.
     *
     * @param element the target element
     */
    private void promptAddEvent(MapElement element) {
        java.util.List<GameEvent> options = EventFactory.availableEvents();
        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            GameEvent e = options.get(i);
            labels[i] = e.getName() + " - " + e.getDescription();
        }
        String pick = (String) JOptionPane.showInputDialog(this,
                "Select an event to attach:", "Add Event",
                JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
        if (pick == null) {
            return;
        }
        GameEvent chosen = options.get(java.util.Arrays.asList(labels).indexOf(pick));
        context.addEvent(element, chosen);
    }

    /**
     * Removes the selected event from the element, if any.
     *
     * @param element the target element
     * @param index the selected list index
     */
    private void removeSelectedEvent(MapElement element, int index) {
        if (index >= 0 && index < element.getEvents().size()) {
            context.removeEvent(element, element.getEvents().get(index));
        }
    }

    /**
     * Prompts the user to choose a faction and adds a random army.
     *
     * @param element the target element
     */
    private void promptAddArmy(MapElement element) {
        Faction[] factions = Faction.values();
        Faction pick = (Faction) JOptionPane.showInputDialog(this, "Select faction:", "Add Army",
                JOptionPane.QUESTION_MESSAGE, null, factions, factions[0]);
        if (pick == null) {
            return;
        }
        context.addArmy(element, new ArmyFactory().createRandomArmy(pick));
    }

    /**
     * Pops the army builder dialog so the user can hand-craft an army.
     *
     * @param element the target element
     */
    private void promptBuildArmy(MapElement element) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Army army = ArmyBuilderDialog.showDialog(frame);
        if (army == null) {
            return;
        }
        context.addArmy(element, army);
    }

    /**
     * Toggles the player-controlled flag of the selected army.
     *
     * @param element the target element
     * @param index the selected list index
     */
    private void toggleControl(MapElement element, int index) {
        if (index < 0 || index >= element.getArmies().size()) {
            return;
        }
        Army army = element.getArmies().get(index);
        army.setPlayerControlled(!army.isPlayerControlled());
        context.getGraph().fireArmyChanged();
    }

    /**
     * Removes the selected army from the element, if any.
     *
     * @param element the target element
     * @param index the selected list index
     */
    private void removeSelectedArmy(MapElement element, int index) {
        if (index >= 0 && index < element.getArmies().size()) {
            context.removeArmy(element, element.getArmies().get(index));
        }
    }

    /**
     * Opens the unit detail dialog for the selected army, if any.
     *
     * @param element the target element
     * @param index the selected list index
     */
    private void showArmyDetails(MapElement element, int index) {
        if (index < 0 || index >= element.getArmies().size()) {
            return;
        }
        Army army = element.getArmies().get(index);
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        new UnitInfoDialog(frame, army).setVisible(true);
    }

    /**
     * Focus listener that records a rename command when editing finishes
     * and the text has actually changed.
     */
    private final class RenameOnFocusLoss extends FocusAdapter {

        /** The element being renamed. */
        private final MapElement element;

        /** The text field providing the new name. */
        private final JTextField field;

        /** The value when editing began, to detect real changes. */
        private String startingValue;

        /**
         * Constructs the listener.
         *
         * @param element the element to rename
         * @param field the backing text field
         */
        private RenameOnFocusLoss(MapElement element, JTextField field) {
            this.element = element;
            this.field = field;
            this.startingValue = element.getName();
        }

        /**
         * Records the value present when editing starts.
         *
         * @param event the focus event
         */
        @Override
        public void focusGained(FocusEvent event) {
            startingValue = field.getText();
        }

        /**
         * Issues a rename command if the text changed to a non-blank value.
         *
         * @param event the focus event
         */
        @Override
        public void focusLost(FocusEvent event) {
            String current = field.getText();
            if (current != null && !current.equals(startingValue) && !current.isBlank()) {
                context.rename(element, current);
            }
        }
    }
}
