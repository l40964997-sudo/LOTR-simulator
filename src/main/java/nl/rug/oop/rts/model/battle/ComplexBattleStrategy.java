package nl.rug.oop.rts.model.battle;

import nl.rug.oop.rts.model.army.Archer;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Cavalry;
import nl.rug.oop.rts.model.army.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A multi-phase battle resolver that gives different unit roles a chance
 * to shine.
 * <p>
 * Each engagement runs in three phases:
 * <ol>
 *   <li><strong>Volley</strong>: archers (and crossbowmen) fire a pre-melee
 *       volley using their bonus damage.</li>
 *   <li><strong>Charge</strong>: cavalry slam into the enemy line at double
 *       damage.</li>
 *   <li><strong>Melee</strong>: every surviving unit fights round-by-round
 *       until one side is annihilated or a morale check causes the weaker
 *       side to rout.</li>
 * </ol>
 * The fortify shield and morale modifiers introduced on {@link Army} are
 * honoured. The strategy returns a colourful description that includes a
 * blow-by-blow summary plus the survivors per side.
 */
public class ComplexBattleStrategy implements BattleStrategy {

    /** Hard cap on melee rounds to avoid runaway loops. */
    private static final int MELEE_ROUND_CAP = 100;

    /** Morale threshold below which an army may rout. */
    private static final double ROUT_THRESHOLD = 0.45;

    /** Random source for tie breaking and morale decay. */
    private final Random random;

    /**
     * Default constructor with a fresh random source.
     */
    public ComplexBattleStrategy() {
        this(new Random());
    }

    /**
     * Constructor with an injectable random source, useful for testing.
     *
     * @param random the random source; must not be {@code null}
     */
    public ComplexBattleStrategy(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /**
     * Resolves a battle between opposing armies at the given location.
     * The combat is processed in phases: volley, charge, and melee.
     *
     * @param armies       The list of participating armies (must contain at least two distinct teams).
     * @param locationName The name of the battlefield.
     * @return The {@code BattleResult}, or {@code null} if the battle cannot occur (e.g., missing or invalid armies).
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
        StringBuilder log = new StringBuilder();
        prepareUnits(armies);
        runPhase(byTeam, UnitFilter.ARCHER_ONLY, log, "Volley");
        runPhase(byTeam, UnitFilter.CAVALRY_ONLY, log, "Charge");
        runMelee(byTeam, log);
        return collectResult(armies, byTeam, locationName, log);
    }

    /**
     * Resets per-battle state on every unit so volleys and charges are ready.
     *
     * @param armies the participating armies
     */
    private void prepareUnits(List<Army> armies) {
        for (Army army : armies) {
            for (Unit u : army.getUnits()) {
                u.resetForBattle();
            }
        }
    }

    /**
     * Groups participants by team identifier.
     *
     * @param armies the armies present
     * @return a map from team id to its armies
     */
    private Map<Integer, List<Army>> groupByTeam(List<Army> armies) {
        Map<Integer, List<Army>> byTeam = new HashMap<>();
        for (Army army : armies) {
            byTeam.computeIfAbsent(army.getTeam(), key -> new ArrayList<>()).add(army);
        }
        return byTeam;
    }

    /**
     * Runs a single asymmetric phase (volley, charge).
     *
     * @param byTeam team to army mapping
     * @param filter chooses which units actually fire this phase
     * @param log narrative log
     * @param phaseName the name shown in the log
     */
    private void runPhase(Map<Integer, List<Army>> byTeam, UnitFilter filter,
                          StringBuilder log, String phaseName) {
        if (byTeam.size() < 2) {
            return;
        }
        Map<Integer, Integer> teamDamage = computeFilteredDamage(byTeam, filter);
        boolean fired = applyDamageRound(byTeam, teamDamage, log, phaseName);
        if (fired) {
            purgeDead(byTeam);
        }
    }

    /**
     * Runs the melee round loop until one team is left, the cap is hit, or
     * the morale check breaks the weaker side.
     *
     * @param byTeam team to army mapping
     * @param log narrative log
     */
    private void runMelee(Map<Integer, List<Army>> byTeam, StringBuilder log) {
        int rounds = 0;
        while (byTeam.size() > 1 && rounds < MELEE_ROUND_CAP) {
            rounds++;
            Map<Integer, Integer> teamDamage = computeFilteredDamage(byTeam, UnitFilter.ALL);
            applyDamageRound(byTeam, teamDamage, log, "Melee #" + rounds);
            purgeDead(byTeam);
            checkRout(byTeam, log);
        }
    }

    /**
     * Performs a morale check; the weaker army routs when its morale dips
     * below the threshold.
     *
     * @param byTeam team to army mapping; mutated when an army routs
     * @param log narrative log
     */
    private void checkRout(Map<Integer, List<Army>> byTeam, StringBuilder log) {
        for (Map.Entry<Integer, List<Army>> entry : new ArrayList<>(byTeam.entrySet())) {
            List<Army> teamArmies = entry.getValue();
            for (Army army : new ArrayList<>(teamArmies)) {
                if (army.getMorale() < ROUT_THRESHOLD && random.nextDouble() < 0.3) {
                    log.append("  ").append(army.getName()).append(" routs!\n");
                    clearArmy(army);
                    teamArmies.remove(army);
                }
            }
            if (teamArmies.isEmpty()) {
                byTeam.remove(entry.getKey());
            }
        }
    }

    /**
     * Sums damage produced by units passing the given filter, per team.
     *
     * @param byTeam team to army mapping
     * @param filter the unit filter
     * @return per-team damage totals
     */
    private Map<Integer, Integer> computeFilteredDamage(Map<Integer, List<Army>> byTeam,
                                                        UnitFilter filter) {
        Map<Integer, Integer> teamDamage = new HashMap<>();
        for (Map.Entry<Integer, List<Army>> entry : byTeam.entrySet()) {
            int damage = 0;
            for (Army army : entry.getValue()) {
                damage += sumFilteredDamage(army, filter);
            }
            teamDamage.put(entry.getKey(), damage);
        }
        return teamDamage;
    }

    /**
     * Sums damage from one army's units after applying the unit filter.
     *
     * @param army the army
     * @param filter the unit filter
     * @return the army's damage contribution
     */
    private int sumFilteredDamage(Army army, UnitFilter filter) {
        int damage = 0;
        for (Unit unit : army.getUnits()) {
            if (unit.isAlive() && filter.accept(unit)) {
                damage += unit.computeDamage();
            }
        }
        return (int) Math.round(damage * army.getMorale());
    }

    /**
     * Applies one round of damage from every attacker to a random enemy team.
     *
     * @param byTeam team to army mapping
     * @param teamDamage per-team damage totals
     * @param log narrative log
     * @param phaseName the phase name for log lines
     * @return {@code true} if any damage was actually applied
     */
    private boolean applyDamageRound(Map<Integer, List<Army>> byTeam,
                                     Map<Integer, Integer> teamDamage,
                                     StringBuilder log, String phaseName) {
        boolean fired = false;
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
            int absorbed = spreadDamage(byTeam.get(targetTeam), damage);
            log.append("  ").append(phaseName).append(": Team ").append(attacker.getKey())
                    .append(" deals ").append(absorbed).append(" damage.\n");
            fired = true;
        }
        return fired;
    }

    /**
     * Removes dead units and empty armies, then prunes empty teams.
     *
     * @param byTeam team mapping, mutated in place
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
     * Builds the battle result from the survivors and the running log.
     *
     * @param armies the original participants
     * @param byTeam the surviving team mapping
     * @param locationName the place name
     * @param log the running narrative log
     * @return the assembled result
     */
    private BattleResult collectResult(List<Army> armies, Map<Integer, List<Army>> byTeam,
                                       String locationName, StringBuilder log) {
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
        clearFortifyShields(winners);
        clearFortifyShields(losers);
        String description = buildDescription(winners, losers, locationName, log);
        return new BattleResult(winners, losers, description);
    }

    /**
     * Resets each survivor's fortify shield so it does not carry over.
     *
     * @param armies the armies to reset
     */
    private void clearFortifyShields(List<Army> armies) {
        for (Army army : armies) {
            army.setFortifyShield(0.0);
        }
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
     * Distributes total damage across the units of an opposing team,
     * applying the fortify shield of each defending army first.
     *
     * @param teamArmies the armies on the target team
     * @param totalDamage the damage to distribute
     * @return the total damage that was actually applied after absorption
     */
    private int spreadDamage(List<Army> teamArmies, int totalDamage) {
        int remaining = totalDamage;
        int applied = 0;
        for (Army army : teamArmies) {
            int share = Math.max(0, (int) Math.round(remaining * 1.0 / Math.max(1, teamArmies.size())));
            int shielded = (int) Math.round(share * (1.0 - army.getFortifyShield()));
            applied += distributeAcrossUnits(army, shielded);
        }
        return applied;
    }

    /**
     * Round-robins damage across an army's living units.
     *
     * @param army the receiving army
     * @param damage the absorbed damage
     * @return the damage actually applied
     */
    private int distributeAcrossUnits(Army army, int damage) {
        if (damage <= 0) {
            return 0;
        }
        List<Unit> alive = new ArrayList<>();
        for (Unit u : army.getUnits()) {
            if (u.isAlive()) {
                alive.add(u);
            }
        }
        if (alive.isEmpty()) {
            return 0;
        }
        int per = Math.max(1, damage / alive.size());
        int remainder = Math.max(0, damage - per * alive.size());
        for (Unit u : alive) {
            u.takeDamage(per);
        }
        while (remainder > 0 && !alive.isEmpty()) {
            alive.get(random.nextInt(alive.size())).takeDamage(1);
            remainder--;
        }
        return damage;
    }

    /**
     * Builds the multi-line narrative shown to the user.
     *
     * @param winners surviving armies
     * @param losers defeated armies
     * @param locationName the place name
     * @param log the running per-round log
     * @return the rendered description
     */
    private String buildDescription(List<Army> winners, List<Army> losers,
                                    String locationName, StringBuilder log) {
        StringBuilder sb = new StringBuilder();
        String place = locationName == null ? "unknown location" : locationName;
        sb.append("Battle at ").append(place).append("!\n");
        if (log.length() > 0) {
            sb.append(log);
        }
        appendArmyList(sb, "Victors: ", winners, true);
        appendArmyList(sb, "Defeated: ", losers, false);
        return sb.toString();
    }

    /**
     * Appends a labelled, comma separated army list to a builder.
     *
     * @param sb the builder
     * @param label the section label
     * @param list the armies
     * @param withCounts whether to append unit counts
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

    /** Encapsulates a per-unit selection rule for the various phases. */
    private enum UnitFilter {
        /** Match every living unit. */
        ALL {
            @Override
            boolean accept(Unit u) {
                return true;
            }
        },
        /** Match only archers (and their subclasses such as crossbowmen). */
        ARCHER_ONLY {
            @Override
            boolean accept(Unit u) {
                return u instanceof Archer;
            }
        },
        /** Match only cavalry. */
        CAVALRY_ONLY {
            @Override
            boolean accept(Unit u) {
                return u instanceof Cavalry;
            }
        };

        /**
         * Reports whether a unit participates in the current phase.
         *
         * @param unit the unit to test
         * @return {@code true} when the unit acts this phase
         */
        abstract boolean accept(Unit unit);
    }
}
