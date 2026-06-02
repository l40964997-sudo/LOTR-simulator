package nl.rug.oop.rts.model.army;

import nl.rug.oop.rts.model.graph.Node;

/**
 * Picks and executes the {@link ArmyAction} an army carries out at the
 * start of a simulation turn.
 * <p>
 * The interface is the Strategy seam the simulator uses to drive the
 * per-turn action phase. {@link ActionExecutor} is the production
 * implementation; tests can supply scripted variants that always return the
 * same action or that record every call. By depending on this interface
 * rather than the concrete class, the simulator stays oblivious to how
 * actions are decided and applied.
 */
public interface ActionStrategy {

    /**
     * Returns the action an AI-controlled army should perform this turn.
     *
     * @return a non-null action
     */
    ArmyAction pickRandomAction();

    /**
     * Applies the chosen action's side effects to an army.
     *
     * @param army the acting army; ignored when {@code null}
     * @param action the action to apply; ignored when {@code null}
     * @param origin the army's current node, used by location-aware actions
     * @return a short narrative describing what happened; never {@code null}
     */
    String execute(Army army, ArmyAction action, Node origin);

    /**
     * Reports whether the action lets the army move this turn.
     *
     * @param action the action to query
     * @return {@code true} when the army may advance along an edge
     */
    boolean permitsMovement(ArmyAction action);

    /**
     * Reports whether the action triggers a second move on the same turn.
     *
     * @param action the action to query
     * @return {@code true} only when the action is a forced march
     */
    boolean doublesMovement(ArmyAction action);
}
