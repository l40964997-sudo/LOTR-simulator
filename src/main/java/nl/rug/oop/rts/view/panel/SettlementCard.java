package nl.rug.oop.rts.view.panel;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.model.graph.Wealth;
import nl.rug.oop.rts.util.TextureLoader;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Renders the settlement detail card shown in the side panel when a node
 * is selected.
 * <p>
 * The card combines the larger faction portrait (loaded from the
 * {@code images/factions} folder via {@link TextureLoader}) with editable
 * widgets for the node's home faction, population, literacy rate, wealth
 * tier and spoken language, plus a read-only garrison line summarising
 * the resident armies. Edits commit straight back to the {@link Node}
 * and fire a structural change so the map and banner repaint.
 * <p>
 * Splitting this card off from {@link SidePanel} keeps each file focused
 * on one job and well within the project's file length limit.
 */
public class SettlementCard {

    /** Pixel size for the large faction portrait. */
    private static final int FACTION_PORTRAIT = 96;

    /** Fixed width of the field captions, in pixels. */
    private static final int LABEL_WIDTH = 80;

    /** Maximum population a spinner will allow (10 million is plenty). */
    private static final int MAX_POPULATION = 10_000_000;

    /** Step size for the population spinner. */
    private static final int POPULATION_STEP = 500;

    /** Editor context used to fire repaint events on mutations. */
    private final EditorContext context;

    /**
     * Constructs the card factory.
     *
     * @param context the editor context; must not be {@code null}
     */
    public SettlementCard(EditorContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context;
    }

    /**
     * Builds the settlement detail card for the given node.
     *
     * @param node the selected node
     * @return the card component, ready to be added to a parent layout
     */
    public Component build(Node node) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(makeSprite("faction" + node.getFaction().textureKey(), FACTION_PORTRAIT));
        row.add(Box.createHorizontalStrut(8));
        row.add(makeFields(node));
        return row;
    }

    /**
     * Builds the vertical column of editable field rows.
     *
     * @param node the selected node
     * @return the configured field column
     */
    private JPanel makeFields(Node node) {
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(makeFactionRow(node));
        fields.add(makePopulationRow(node));
        fields.add(makeLiteracyRow(node));
        fields.add(makeWealthRow(node));
        fields.add(makeLanguageRow(node));
        fields.add(makeArmySummaryRow(node));
        return fields;
    }

    /**
     * Row containing the home-faction picker.
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makeFactionRow(Node node) {
        JComboBox<Faction> picker = new JComboBox<>(Faction.values());
        picker.setSelectedItem(node.getFaction());
        picker.addActionListener(action -> {
            Faction picked = (Faction) picker.getSelectedItem();
            if (picked != null && picked != node.getFaction()) {
                node.setFaction(picked);
                context.getGraph().fireStructureChanged();
            }
        });
        return makeLabelledRow("Faction:", picker);
    }

    /**
     * Row containing the population spinner.
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makePopulationRow(Node node) {
        SpinnerNumberModel model = new SpinnerNumberModel(node.getPopulation(),
                0, MAX_POPULATION, POPULATION_STEP);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(change -> {
            node.setPopulation((Integer) spinner.getValue());
            context.getGraph().fireStructureChanged();
        });
        return makeLabelledRow("Population:", spinner);
    }

    /**
     * Row containing the literacy spinner (0-100%).
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makeLiteracyRow(Node node) {
        SpinnerNumberModel model = new SpinnerNumberModel(node.getLiteracy(),
                0, 100, 5);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(change -> {
            node.setLiteracy((Integer) spinner.getValue());
            context.getGraph().fireStructureChanged();
        });
        return makeLabelledRow("Literacy %:", spinner);
    }

    /**
     * Row containing the wealth tier picker.
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makeWealthRow(Node node) {
        JComboBox<Wealth> picker = new JComboBox<>(Wealth.values());
        picker.setSelectedItem(node.getWealth());
        picker.addActionListener(action -> {
            Wealth picked = (Wealth) picker.getSelectedItem();
            if (picked != null && picked != node.getWealth()) {
                node.setWealth(picked);
                context.getGraph().fireStructureChanged();
            }
        });
        return makeLabelledRow("Wealth:", picker);
    }

    /**
     * Row containing the spoken-language text field. Commits on focus loss
     * so casual typing doesn't fire a dozen model events.
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makeLanguageRow(Node node) {
        JTextField field = new JTextField(node.getLanguage());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                if (!field.getText().equals(node.getLanguage())) {
                    node.setLanguage(field.getText());
                    context.getGraph().fireStructureChanged();
                }
            }
        });
        return makeLabelledRow("Language:", field);
    }

    /**
     * Read-only summary row showing the number of resident armies and units.
     *
     * @param node the selected node
     * @return the row component
     */
    private Component makeArmySummaryRow(Node node) {
        int armies = node.getArmies().size();
        int units = 0;
        for (Army army : node.getArmies()) {
            units += army.size();
        }
        JLabel value = new JLabel(armies == 0
                ? "none"
                : armies + " army/armies (" + units + " units)");
        value.setForeground(Color.LIGHT_GRAY);
        return makeLabelledRow("Garrison:", value);
    }

    /**
     * Builds a left-aligned row with a fixed-width caption followed by the
     * given editor widget. Spinner editors are styled to use grouping
     * separators so large populations stay readable.
     *
     * @param caption the label text
     * @param editor the right-hand widget
     * @return the row component
     */
    private Component makeLabelledRow(String caption, JComponent editor) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel(caption);
        label.setForeground(Color.LIGHT_GRAY);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 22));
        row.add(label);
        row.add(Box.createHorizontalStrut(4));
        editor.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (editor instanceof JSpinner) {
            JComponent comp = ((JSpinner) editor).getEditor();
            if (comp instanceof JSpinner.NumberEditor) {
                ((JSpinner.NumberEditor) comp).getFormat().setGroupingUsed(true);
            }
        }
        editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.add(editor);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }

    /**
     * Loads a faction portrait via the texture loader and wraps it in a
     * synchronously-decoded label.
     *
     * @param key the texture key (e.g. {@code "factionMen"})
     * @param size the side length in pixels
     * @return a label rendering the sprite
     */
    private JLabel makeSprite(String key, int size) {
        Image image = TextureLoader.getInstance().getTexture(key, size, size);
        ImageIcon icon = new ImageIcon(image);
        JLabel label = new JLabel(icon);
        label.setPreferredSize(new Dimension(size, size));
        return label;
    }
}
