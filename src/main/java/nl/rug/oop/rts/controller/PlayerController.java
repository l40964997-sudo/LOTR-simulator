package nl.rug.oop.rts.controller;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.graph.Node;

/**
 * Source of {@link PlayerOrder}s for armies that the user is commanding.
 * <p>
 * Decoupling the simulator from the dialog layer behind this interface
 * means tests can plug in scripted controllers (no UI required) while the
 * production code points at a Swing dialog. Implementations are expected
 * to block until the player has finished issuing the order.
 */
public interface PlayerController {

    /**
     * Asks the user to issue an order for {@code army} currently sitting at
     * {@code currentNode}. Implementations may return {@code null} to
     * signal a default fallback should be used.
     *
     * @param army the army awaiting orders; never {@code null}
     * @param currentNode the army's current location; never {@code null}
     * @return the player's order, or {@code null} to defer to defaults
     */
    PlayerOrder requestOrder(Army army, Node currentNode);
}
