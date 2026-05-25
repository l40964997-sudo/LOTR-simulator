package nl.rug.oop.rts.controller;

import nl.rug.oop.rts.model.army.ArmyAction;
import nl.rug.oop.rts.model.graph.Edge;

/**
 * Immutable bundle of the orders a player issues for one army on one turn.
 * <p>
 * Each order contains the {@link ArmyAction} to perform plus, optionally,
 * the {@link Edge} the army should march along. When the action pins the
 * army in place (for example {@link ArmyAction#HOLD}) the edge is allowed
 * to be {@code null}.
 */
public final class PlayerOrder {

    /** The action the army will perform this turn. */
    private final ArmyAction action;

    /** The edge the army marches along, or {@code null} when staying put. */
    private final Edge edge;

    /**
     * Constructs an order.
     *
     * @param action the chosen action; must not be {@code null}
     * @param edge the chosen outgoing edge; may be {@code null} when holding
     */
    public PlayerOrder(ArmyAction action, Edge edge) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.action = action;
        this.edge = edge;
    }

    /**
     * Returns the chosen action.
     *
     * @return the action; never {@code null}
     */
    public ArmyAction getAction() {
        return action;
    }

    /**
     * Returns the chosen outgoing edge.
     *
     * @return the edge, or {@code null} if the army stays put
     */
    public Edge getEdge() {
        return edge;
    }
}
