package nl.rug.oop.rts.view.dialog;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.army.Unit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Modal dialog showing detailed information for every unit in an army.
 *
 * <p>Satisfies the assignment requirement that there should be a menu
 * providing detailed information about each unit's stats. The dialog also
 * shows aggregate stats (total strength, total health, combat power) and a
 * short flavour text per faction, giving the user a sense of why one army
 * might be expected to beat another.</p>
 */
public class UnitInfoDialog extends JDialog {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Placeholder shown in empty table cells. */
    private static final String EMPTY_CELL = "-";

    /** Per-faction flavour text used to populate the dialog. */
    private static final Map<Faction, String> FLAVOUR = buildFlavourMap();

    /**
     * Constructs the dialog.
     *
     * @param owner the parent frame
     * @param army the army to inspect; must not be {@code null}
     */
    public UnitInfoDialog(Frame owner, Army army) {
        super(owner, "Army Details", true);
        if (army == null) {
            throw new IllegalArgumentException("army must not be null");
        }
        setTitle("Army Details: " + army.getName());
        setLayout(new BorderLayout(8, 8));
        add(buildHeader(army), BorderLayout.NORTH);
        add(buildTable(army), BorderLayout.CENTER);
        add(buildFooter(owner, army), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Builds the per-faction flavour text map.
     *
     * @return an immutable-ish map of faction to flavour text
     */
    private static Map<Faction, String> buildFlavourMap() {
        Map<Faction, String> map = new HashMap<>();
        map.put(Faction.MEN,
                "The Men of the West stand at the last bastion against the Shadow.");
        map.put(Faction.ELVES,
                "Immortal archers and lancers of Lothlorien, Mirkwood and Rivendell.");
        map.put(Faction.DWARVES,
                "Stout warriors of Erebor and the Iron Hills, masters of axe and forge.");
        map.put(Faction.MORDOR,
                "Orcs of Mordor and Haradrim of the south, sworn to the will of Sauron.");
        map.put(Faction.ISENGARD,
                "Saruman's Uruk-hai and Warg Riders, bred for war beneath Orthanc.");
        return map;
    }

    /**
     * Builds the header panel showing the army name, team and aggregate
     * statistics.
     *
     * @param army the army being inspected
     * @return the header component
     */
    private JPanel buildHeader(Army army) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel title = new JLabel(army.getName() + " - " + army.getFaction().getDisplayName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        header.add(title);
        String teamName = army.getTeam() == Faction.TEAM_FREE_PEOPLES ? "Free Peoples" : "Shadow";
        header.add(new JLabel("Team: " + teamName));
        header.add(Box.createVerticalStrut(4));
        header.add(new JLabel(formatStats(army)));
        header.add(Box.createVerticalStrut(4));
        header.add(new JLabel("<html><i>" + FLAVOUR.getOrDefault(army.getFaction(), "") + "</i></html>"));
        Color base = army.getFaction().getColor();
        header.setOpaque(true);
        header.setBackground(new Color(base.getRed(), base.getGreen(), base.getBlue(), 24));
        return header;
    }

    /**
     * Formats the aggregate statistics line for an army.
     *
     * @param army the army
     * @return a formatted statistics string
     */
    private String formatStats(Army army) {
        return String.format("Units: %d | Total Strength: %d | Total Health: %d | Combat Power: %.1f",
                army.size(), army.totalStrength(), army.totalHealth(), army.combatPower());
    }

    /**
     * Builds the scrollable per-unit table.
     *
     * @param army the army whose units are listed
     * @return the table inside a scroll pane
     */
    private JScrollPane buildTable(Army army) {
        DefaultTableModel model = new NonEditableTableModel();
        int index = 1;
        for (Unit unit : army.getUnits()) {
            model.addRow(new Object[] {index, unit.getName(), unit.getStrength(),
                unit.getHealth(), unit.isAlive() ? "Alive" : "Defeated"});
            index++;
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[] {EMPTY_CELL, "(no units)", EMPTY_CELL, EMPTY_CELL, EMPTY_CELL});
        }
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(460, 280));
        return scroll;
    }

    /**
     * Builds the footer with the faction colour stripe and a close button.
     *
     * @param owner the parent frame (unused beyond layout parenting)
     * @param army the army, used for the colour stripe
     * @return the footer component
     */
    private JPanel buildFooter(Frame owner, Army army) {
        JPanel stripe = new JPanel();
        stripe.setBackground(army.getFaction().getColor());
        stripe.setPreferredSize(new Dimension(0, 8));
        stripe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttonRow = new JPanel();
        buttonRow.add(close);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(stripe, BorderLayout.NORTH);
        footer.add(buttonRow, BorderLayout.CENTER);
        return footer;
    }

    /**
     * Table model whose cells are never editable.
     */
    private static final class NonEditableTableModel extends DefaultTableModel {

        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the model with the fixed column headers.
         */
        private NonEditableTableModel() {
            super(new Object[] {"#", "Name", "Strength", "Health", "Status"}, 0);
        }

        /**
         * Marks every cell as read-only.
         *
         * @param row the row index
         * @param column the column index
         * @return always {@code false}
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
