package nl.rug.oop.rts.view.dialog;

import lombok.Getter;
import nl.rug.oop.rts.controller.PlayerOrder;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.ArmyAction;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Node;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;

/**
 * Modal dialog used to ask the player how a player-controlled army should
 * act this turn.
 * <p>
 * The dialog shows the army's current situation (faction, size, morale),
 * an enum-driven dropdown of every {@link ArmyAction}, and a dropdown of
 * the outgoing edges. The "Edge" dropdown is automatically disabled when
 * the chosen action keeps the army in place. The user confirms with
 * "Order March" or cancels - cancellation returns a sensible HOLD default
 * so the simulator can always make progress.
 */
@Getter
public class PlayerOrderDialog extends JDialog {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Action picker. */
    private final JComboBox<ArmyAction> actionCombo = new JComboBox<>(ArmyAction.values());

    /** Edge picker (rendered with a friendly textual format). */
    private final JComboBox<Edge> edgeCombo;

    /** Description label that mirrors the active action. */
    private final JLabel descriptionLabel = new JLabel();

    /** Captures the user's final choice. */
    private PlayerOrder result;

    /**
     * Constructs the dialog.
     *
     * @param owner the parent frame
     * @param army the army awaiting orders
     * @param currentNode the army's current node
     */
    public PlayerOrderDialog(Frame owner, Army army, Node currentNode) {
        super(owner, "Orders: " + army.getName(), true);
        if (currentNode == null) {
            throw new IllegalArgumentException("currentNode must not be null");
        }
        this.edgeCombo = buildEdgeCombo(currentNode);
        setLayout(new BorderLayout(8, 8));
        add(buildHeader(army, currentNode), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        actionCombo.addActionListener(e -> syncDescription());
        syncDescription();
        setPreferredSize(new Dimension(420, 280));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Builds the descriptive header.
     *
     * @param army the army
     * @param currentNode the army's location
     * @return the header component
     */
    private JPanel buildHeader(Army army, Node currentNode) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        JLabel title = new JLabel(army.getName() + " - " + army.getFaction().getDisplayName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        header.add(title);
        header.add(new JLabel(String.format("At: %s | Units: %d | Morale: %.0f%%",
                currentNode.getName(), army.size(), army.getMorale() * 100)));
        header.add(Box.createVerticalStrut(4));
        return header;
    }

    /**
     * Builds the body with the action and edge pickers.
     *
     * @return the body component
     */
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        body.add(makeRow("Action:", actionCombo));
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC));
        descriptionLabel.setForeground(java.awt.Color.LIGHT_GRAY);
        body.add(descriptionLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(makeRow("March via:", edgeCombo));
        return body;
    }

    /**
     * Builds the footer with the confirm and cancel buttons.
     *
     * @return the footer component
     */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Hold");
        JButton confirm = new JButton("Order March");
        cancel.addActionListener(e -> {
            this.result = new PlayerOrder(ArmyAction.HOLD, null);
            dispose();
        });
        confirm.addActionListener(e -> finishOk());
        footer.add(cancel);
        footer.add(confirm);
        return footer;
    }

    /**
     * Builds a labelled row hosting one of the pickers.
     *
     * @param label the label text
     * @param component the picker
     * @return the row component
     */
    private JPanel makeRow(String label, Component component) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    /**
     * Builds the edge combo populated with the outgoing edges of a node.
     *
     * @param origin the army's current node
     * @return the combo box
     */
    private JComboBox<Edge> buildEdgeCombo(Node origin) {
        List<Edge> outgoing = origin.getEdges();
        JComboBox<Edge> combo = new JComboBox<>(outgoing.toArray(new Edge[0]));
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("(none)");
            if (value != null) {
                label.setText(value.getName() + " -> " + value.opposite(origin).getName());
            }
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        return combo;
    }

    /**
     * Refreshes the description and edge enablement when the action changes.
     */
    private void syncDescription() {
        ArmyAction action = (ArmyAction) actionCombo.getSelectedItem();
        if (action == null) {
            return;
        }
        descriptionLabel.setText("  " + action.getDescription());
        boolean canMove = action != ArmyAction.HOLD && action != ArmyAction.FORTIFY;
        edgeCombo.setEnabled(canMove && edgeCombo.getItemCount() > 0);
    }

    /**
     * Validates and records the player's choice.
     */
    private void finishOk() {
        ArmyAction action = (ArmyAction) actionCombo.getSelectedItem();
        Edge edge = edgeCombo.isEnabled() ? (Edge) edgeCombo.getSelectedItem() : null;
        this.result = new PlayerOrder(action == null ? ArmyAction.HOLD : action, edge);
        dispose();
    }
}
