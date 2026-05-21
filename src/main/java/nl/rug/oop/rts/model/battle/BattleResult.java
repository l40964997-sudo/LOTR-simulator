package nl.rug.oop.rts.model.battle;

import nl.rug.oop.rts.model.army.Army;

import java.util.Collections;
import java.util.List;

/**
 * Immutable summary of the outcome of a {@link BattleStrategy} resolution.
 * <p>
 * The result is shown to the user through a popup and is also useful for
 * logging and testing. It deliberately keeps both winning and losing
 * armies (the latter as references even after their units are gone) so
 * that the message generator can craft a descriptive sentence.
 */
public final class BattleResult {

    /** Surviving armies (all belong to the same team). */
    private final List<Army> winners;

    /** Defeated armies (now empty). */
    private final List<Army> losers;

    /** Human readable description of the battle. */
    private final String description;

    /**
     * Constructs a battle result.
     *
     * @param winners the surviving armies; defensively copied
     * @param losers the defeated armies; defensively copied
     * @param description a short narrative shown to the user
     */
    public BattleResult(List<Army> winners, List<Army> losers, String description) {
        this.winners = winners == null ? List.of() : List.copyOf(winners);
        this.losers = losers == null ? List.of() : List.copyOf(losers);
        this.description = description == null ? "" : description;
    }

    /**
     * Returns an unmodifiable view of the winners.
     *
     * @return the winning armies
     */
    public List<Army> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    /**
     * Returns an unmodifiable view of the losers.
     *
     * @return the losing armies
     */
    public List<Army> getLosers() {
        return Collections.unmodifiableList(losers);
    }

    /**
     * Returns the descriptive narrative.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
