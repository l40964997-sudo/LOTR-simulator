package nl.rug.oop.rts.model.battle;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;

import java.util.*;

/**
 * Default {@link BattleStrategy} that pits all armies against each other in
 * a sequence of synchronous rounds.
 *
 * <p>Algorithm summary: the participants are split by team; if only one
 * team has armies the encounter is not a battle and {@code null} is
 * returned. Otherwise a round loop runs in which each side's total damage
 * is computed by summing {@link Unit#computeDamage()} over its living
 * units, the damage is spread across the opposing side, and dead units are
 * removed at the end of the round. The remaining team wins and every
 * defeated army has its unit list cleared.</p>
 *
 * <p>The design lets the simulator stay ignorant of combat mechanics: a
 * future assignment that needs a different algorithm only has to supply a
 * new strategy implementation.</p>
 */
public class StandardBattleStrategy implements BattleStrategy {

    /** Hard cap on rounds to avoid runaway loops in pathological cases. */
    private static final int ROUND_CAP = 200;

    /** Random source for distributing damage. */
    private final Random random;

    /**
     * Default constructor with a fresh random source.
     */
    public StandardBattleStrategy() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source, useful for testing.
     *
     * @param random the random source; must not be {@code null}
     */
    public StandardBattleStrategy(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /**
     * Resolves a battle between the supplied armies at a location.
     *
     * @param armies the armies present; a battle needs at least two
     * @param locationName the human readable place name, for the report
     * @return the result, or {@code null} if there was nothing to resolve
     */
    @Override
    public BattleResult resolve(List<Army> armies, String locationName) {
        if (armies == null || armies.size() < 2) {
            return null;
        }
        Map<Integer, List<Army>> byTeam = groupByTeam(armies);
        if (byTeam.size() < 2) {
            return null;
        }
        runRounds(byTeam);
        return collectResult(armies, byTeam, locationName);
    }

    /**
     * Splits the armies into buckets keyed by team identifier.
     *
     * @param armies the armies to group
     * @return a map from team id to the armies on that team
     */
    private Map<Integer, List<Army>> groupByTeam(List<Army> armies) {
        Map<Integer, List<Army>> byTeam = new HashMap<>();
        for (Army army : armies) {
            byTeam.computeIfAbsent(army.getTeam(), key -> new ArrayList<>()).add(army);
        }
        return byTeam;
    }

    /**
     * Runs the round loop until one team remains or the cap is hit.
     *
     * @param byTeam the live mapping of team id to armies; mutated in place
     */
    private void runRounds(Map<Integer, List<Army>> byTeam) {
        int rounds = 0;
        while (byTeam.size() > 1 && rounds < ROUND_CAP) {
            rounds++;
            Map<Integer, Integer> teamDamage = computeTeamDamage(byTeam);
            applyTeamDamage(byTeam, teamDamage);
            purgeDead(byTeam);
        }
    }

    /**
     * Computes each team's total damage output for one round.
     *
     * @param byTeam the current team mapping
     * @return a map from team id to that team's damage for the round
     */
    private Map<Integer, Integer> computeTeamDamage(Map<Integer, List<Army>> byTeam) {
        Map<Integer, Integer> teamDamage = new HashMap<>();
        for (Map.Entry<Integer, List<Army>> entry : byTeam.entrySet()) {
            int damage = 0;
            for (Army army : entry.getValue()) {
                for (Unit unit : army.getUnits()) {
                    if (unit.isAlive()) {
                        damage += unit.computeDamage();
                    }
                }
            }
            teamDamage.put(entry.getKey(), damage);
        }
        return teamDamage;
    }

    /**
     * Applies each team's damage to a randomly chosen enemy team.
     *
     * @param byTeam the current team mapping
     * @param teamDamage the per-team damage computed for this round
     */
    private void applyTeamDamage(Map<Integer, List<Army>> byTeam, Map<Integer, Integer> teamDamage) {
        for (Map.Entry<Integer, Integer> attacker : teamDamage.entrySet()) {
            int damage = attacker.getValue();
            if (damage <= 0) {
                continue;
            }
            List<Integer> targets = new ArrayList<>(byTeam.keySet());
            targets.remove(attacker.getKey());
            if (targets.isEmpty()) {
                continue;
            }
            int targetTeam = targets.get(random.nextInt(targets.size()));
            spreadDamage(byTeam.get(targetTeam), damage);
        }
    }

    /**
     * Removes dead units and empty armies, then prunes empty teams.
     *
     * @param byTeam the current team mapping; mutated in place
     */
    private void purgeDead(Map<Integer, List<Army>> byTeam) {
        for (List<Army> teamArmies : byTeam.values()) {
            for (Army army : teamArmies) {
                army.removeDead();
            }
            teamArmies.removeIf(Army::isDefeated);
        }
        byTeam.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Builds the battle result from the survivors.
     *
     * @param armies the original participants
     * @param byTeam the surviving team mapping
     * @param locationName the place name for the report
     * @return the assembled result
     */
    private BattleResult collectResult(List<Army> armies, Map<Integer, List<Army>> byTeam,
                                       String locationName) {
        List<Army> winners = new ArrayList<>();
        for (List<Army> teamArmies : byTeam.values()) {
            winners.addAll(teamArmies);
        }
        List<Army> losers = new ArrayList<>();
        for (Army army : armies) {
            if (!winners.contains(army)) {
                losers.add(army);
                clearArmy(army);
            }
        }
        String description = buildDescription(winners, losers, locationName);
        return new BattleResult(winners, losers, description);
    }

    /**
     * Removes every unit from a defeated army.
     *
     * @param army the army to empty
     */
    private void clearArmy(Army army) {
        for (Unit unit : new ArrayList<>(army.getUnits())) {
            army.removeUnit(unit);
        }
    }

    /**
     * Distributes total damage across the units of an opposing team in a
     * round-robin fashion so that no single unit absorbs everything.
     *
     * @param teamArmies the armies on the target team
     * @param totalDamage the damage to distribute
     */
    private void spreadDamage(List<Army> teamArmies, int totalDamage) {
        List<Unit> alive = new ArrayList<>();
        for (Army army : teamArmies) {
            for (Unit unit : army.getUnits()) {
                if (unit.isAlive()) {
                    alive.add(unit);
                }
            }
        }
        if (alive.isEmpty() || totalDamage <= 0) {
            return;
        }
        int perUnit = Math.max(1, totalDamage / alive.size());
        int remainder = Math.max(0, totalDamage - perUnit * alive.size());
        for (Unit unit : alive) {
            unit.takeDamage(perUnit);
        }
        spreadRemainder(alive, remainder);
    }

    /**
     * Applies any leftover damage one point at a time to random units.
     *
     * @param alive the living units that may absorb the remainder
     * @param remainderStart the leftover damage to distribute
     */
    private void spreadRemainder(List<Unit> alive, int remainderStart) {
        int remainder = remainderStart;
        while (remainder > 0 && !alive.isEmpty()) {
            int idx = random.nextInt(alive.size());
            alive.get(idx).takeDamage(1);
            remainder--;
        }
    }

    /**
     * Builds a short human readable battle description.
     *
     * @param winners the surviving armies
     * @param losers the defeated armies
     * @param locationName the place name
     * @return the formatted description
     */
    private String buildDescription(List<Army> winners, List<Army> losers, String locationName) {
        StringBuilder sb = new StringBuilder();
        String place = locationName == null ? "unknown location" : locationName;
        sb.append("Battle at ").append(place).append("!\n");
        appendArmyList(sb, "Victors: ", winners, true);
        appendArmyList(sb, "Defeated: ", losers, false);
        return sb.toString();
    }

    /**
     * Appends a labelled, comma separated list of armies to a builder.
     *
     * @param sb the builder to append to
     * @param label the section label
     * @param list the armies to list
     * @param withCounts whether to include surviving unit counts
     */
    private void appendArmyList(StringBuilder sb, String label, List<Army> list, boolean withCounts) {
        if (list.isEmpty()) {
            return;
        }
        sb.append(label);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Army army = list.get(i);
            sb.append(army.getName());
            if (withCounts) {
                sb.append(" (").append(army.size()).append(" units left)");
            }
        }
        sb.append('\n');
    }
}
