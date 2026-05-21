package nl.rug.oop.rts.model.battle;

import nl.rug.oop.rts.model.army.Army;

import java.util.List;

/**
 * Algorithm for resolving an encounter between armies of different teams.
 * <p>
 * Plays the role of <em>Strategy</em> in the Strategy pattern: a single
 * call site (the simulator) delegates resolution to whichever
 * {@code BattleStrategy} the project has been wired up with. Swapping in
 * a more elaborate algorithm (for example one that takes terrain into
 * account) is therefore a one-line change in the simulator, with no
 * cascading edits throughout the rest of the codebase.
 */
public interface BattleStrategy {

    /**
     * Resolves a battle between the supplied armies.
     * <p>
     * Implementations must:
     * <ul>
     *   <li>only act when at least two distinct teams are represented;</li>
     *   <li>leave the winning team's armies in {@code armies} with positive
     *       unit counts;</li>
     *   <li>empty every losing army of its units. The caller is responsible
     *       for actually removing defeated armies from the graph.</li>
     * </ul>
     *
     * @param armies the armies currently sharing a location; may be modified
     *               in place
     * @param locationName the human readable location name, used in the
     *                     description
     * @return a {@link BattleResult} or {@code null} when there was no
     *         battle to resolve
     */
    BattleResult resolve(List<Army> armies, String locationName);
}
