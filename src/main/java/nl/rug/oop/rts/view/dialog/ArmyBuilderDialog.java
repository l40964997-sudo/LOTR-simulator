package nl.rug.oop.rts.view.dialog;

import lombok.Getter;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.army.Unit;
import nl.rug.oop.rts.model.army.UnitFactory;
import nl.rug.oop.rts.model.army.UnitFactory.UnitTemplate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modal dialog for crafting a brand new {@link Army} with full control over
 * its composition.
 * <p>
 * The dialog walks the user through three decisions: the army's faction,
 * its display name, and how many of each unit type to recruit. A "take
 * control" checkbox at the bottom flags the resulting army as player
 * controlled so the simulator will pause for player orders on every step.
 * The dialog never directly mutates the model - it just returns the built
 * army to the caller, which is responsible for inserting it via the usual
 * undo-aware command.
 */
@Getter
public class ArmyBuilderDialog extends JDialog {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Maximum number of units allowed per type. */
    private static final int MAX_PER_UNIT = 50;

    /** The unit factory used to instantiate every unit. */
    private final UnitFactory unitFactory = new UnitFactory();

    /** Name input field. */
    private final JTextField nameField = new JTextField(16);

    /** Faction picker. */
    private final JComboBox<Faction> factionCombo = new JComboBox<>(Faction.values());

    /** The roster section that swaps when the faction changes. */
    private final JPanel rosterPanel = new JPanel(new GridBagLayout());

    /** The current unit-name to spinner mapping for the active faction. */
    private final Map<String, JSpinner> spinnerByName = new LinkedHashMap<>();

    /** "Take Control" checkbox. */
    private final JCheckBox controlCheckbox = new JCheckBox("Take direct control of this army");

    /** Set when the user accepts the dialog with at least one unit. */
    private Army result;

    /**
     * Constructs the dialog and lays out its widgets.
     *
     * @param owner the parent frame
     */
    public ArmyBuilderDialog(Frame owner) {
        super(owner, "Build a New Army", true);
        setLayout(new BorderLayout(8, 8));
        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(rosterPanel), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        factionCombo.addActionListener(e -> rebuildRoster());
        rebuildRoster();
        setPreferredSize(new Dimension(440, 480));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Builds the upper section: name + faction selection plus a flavour line.
     *
     * @return the header component
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        header.add(new JLabel("Army Name:"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        header.add(nameField, c);
        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        header.add(new JLabel("Faction:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        header.add(factionCombo, c);
        return header;
    }

    /**
     * Builds the lower section with the OK / Cancel buttons and the
     * "take control" checkbox.
     *
     * @return the footer component
     */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlRow.add(controlCheckbox);
        footer.add(controlRow, BorderLayout.NORTH);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Recruit");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> finishOk());
        cancel.addActionListener(e -> dispose());
        buttonRow.add(ok);
        buttonRow.add(cancel);
        footer.add(buttonRow, BorderLayout.CENTER);
        return footer;
    }

    /**
     * Rebuilds the per-unit spinner roster when the faction changes.
     */
    private void rebuildRoster() {
        rosterPanel.removeAll();
        spinnerByName.clear();
        Faction faction = (Faction) factionCombo.getSelectedItem();
        if (faction == null) {
            rosterPanel.revalidate();
            rosterPanel.repaint();
            return;
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 6, 2, 6);
        c.anchor = GridBagConstraints.LINE_START;
        int row = 0;
        rosterPanel.add(makeRosterHeader(), constrain(c, row++, 0, 3));
        for (String unitName : faction.getUnitNames()) {
            addRosterRow(unitName, row++, c);
        }
        rosterPanel.revalidate();
        rosterPanel.repaint();
    }

    /**
     * Adds one row to the roster panel for the given unit name.
     *
     * @param unitName the unit name
     * @param row the row index
     * @param c the grid bag constraints to reuse
     */
    private void addRosterRow(String unitName, int row, GridBagConstraints c) {
        JLabel label = new JLabel(unitName);
        UnitTemplate template = UnitFactory.templateFor(unitName);
        JLabel stats = new JLabel(String.format(
                "STR~%d HP~%d DEF=%d EVA=%.0f%%",
                template.averageStrength(), template.averageHealth(),
                template.defense(), template.evasion() * 100));
        stats.setHorizontalAlignment(SwingConstants.LEFT);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(suggestDefault(unitName), 0, MAX_PER_UNIT, 1));
        spinnerByName.put(unitName, spinner);
        rosterPanel.add(label, constrain(c, row, 0, 1));
        rosterPanel.add(stats, constrain(c, row, 1, 1));
        rosterPanel.add(spinner, constrain(c, row, 2, 1));
    }

    /**
     * Builds the small header row that labels the roster columns.
     *
     * @return the header component
     */
    private Component makeRosterHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel label = new JLabel("Choose unit composition:");
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(label);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    /**
     * Suggests a starting unit count, biased toward heroes (1) versus rank
     * and file (8).
     *
     * @param unitName the unit name
     * @return the default value for the spinner
     */
    private int suggestDefault(String unitName) {
        String lower = unitName.toLowerCase();
        if (lower.contains("saruman")) {
            return 1;
        }
        if (lower.contains("troll") || lower.contains("berserker")) {
            return 2;
        }
        return 6;
    }

    /**
     * Helper that fills a {@link GridBagConstraints} for a roster row.
     *
     * @param c the constraints to mutate and return
     * @param row the row index
     * @param col the column index
     * @param width the column span
     * @return the same constraints, for fluent use
     */
    private GridBagConstraints constrain(GridBagConstraints c, int row, int col, int width) {
        c.gridx = col;
        c.gridy = row;
        c.gridwidth = width;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = col == 1 ? 1 : 0;
        return c;
    }

    /**
     * Validates the form and constructs the army.
     */
    private void finishOk() {
        Faction faction = (Faction) factionCombo.getSelectedItem();
        if (faction == null) {
            return;
        }
        List<Unit> units = new ArrayList<>();
        for (Map.Entry<String, JSpinner> entry : spinnerByName.entrySet()) {
            int count = (Integer) entry.getValue().getValue();
            for (int i = 0; i < count; i++) {
                units.add(unitFactory.createNamedUnit(faction, entry.getKey()));
            }
        }
        if (units.isEmpty()) {
            return;
        }
        String name = nameField.getText();
        Army army = new Army(name, faction, units);
        army.setPlayerControlled(controlCheckbox.isSelected());
        this.result = army;
        dispose();
    }

    /**
     * Convenience helper that pops the dialog and returns the built army.
     *
     * @param owner the parent frame
     * @return the new army, or {@code null} if the user cancelled
     */
    public static Army showDialog(JFrame owner) {
        ArmyBuilderDialog dialog = new ArmyBuilderDialog(owner);
        dialog.setVisible(true);
        return dialog.getResult();
    }
}
