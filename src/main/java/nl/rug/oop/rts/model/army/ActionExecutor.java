package nl.rug.oop.rts.model.army;

import nl.rug.oop.rts.model.graph.Node;

import java.util.List;
import java.util.Random;

/**
 * Applies the side effects of an {@link ArmyAction} to an army.
 * <p>
 * Separating execution from the enum keeps the action data tiny and the
 * dispatch easy to extend: a future action need only add a constant and an
 * extra case in {@link #execute(Army, ArmyAction, Node)}. The executor
 * also exposes a helper that returns whether the action permits the army
 * to move this turn, so the simulator can wire it into the move phase
 * without duplicating game rules.
 */
public final class ActionExecutor implements ActionStrategy {

    /** Morale bonus applied by {@link ArmyAction#RALLY}. */
    private static final double RALLY_BONUS = 0.10;

    /** Morale penalty applied by {@link ArmyAction#RAID} the following turn. */
    private static final double RAID_PENALTY = 0.15;

    /** Healing percentage applied by {@link ArmyAction#REGROUP}. */
    private static final double REGROUP_PERCENT = 0.20;

    /** Skirmish damage rolled against each enemy unit at the destination. */
    private static final int SKIRMISH_DAMAGE = 8;

    /** Fortify shield fraction applied by {@link ArmyAction#FORTIFY}. */
    private static final double FORTIFY_FRACTION = 0.30;

    /** Random source used for stochastic side effects (e.g. skirmish hits). */
    private final Random random;

    /**
     * Constructs an executor with a fresh random source.
     */
    public ActionExecutor() {
        this(new Random());
    }

    /**
     * Constructs an executor with the supplied random source.
     *
     * @param random the random source; must not be {@code null}
     */
    public ActionExecutor(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /**
     * Picks an action for an AI-controlled army. The current implementation
     * is a flat uniform roll across the enum constants - intentionally
     * simple, since "more interesting" action selection is a deliberate
     * future extension hook.
     *
     * @return a randomly selected action; never {@code null}
     */
    @Override
    public ArmyAction pickRandomAction() {
        ArmyAction[] all = ArmyAction.values();
        return all[random.nextInt(all.length)];
    }

    /**
     * Executes an action on an army at its current location.
     *
     * @param army the army acting; ignored when {@code null}
     * @param action the action to execute; ignored when {@code null}
     * @param origin the army's current node, used by location-aware actions
     * @return a short human-readable narrative summarising the action
     */
    @Override
    public String execute(Army army, ArmyAction action, Node origin) {
        if (army == null || action == null) {
            return "";
        }
        army.setLastAction(action.getDisplayName());
        return dispatch(army, action, origin);
    }

    /**
     * Internal dispatch helper kept separate so {@link #execute} stays under
     * the configured return-count cap.
     *
     * @param army the acting army
     * @param action the chosen action
     * @param origin the army's current node
     * @return the narrative line
     */
    private String dispatch(Army army, ArmyAction action, Node origin) {
        switch (action) {
            case FORTIFY: return doFortify(army);
            case RALLY: return doRally(army);
            case REGROUP: return doRegroup(army);
            case FORCED_MARCH: return doForcedMarch(army);
            case HOLD: return doHold(army);
            case SKIRMISH: return doSkirmish(army, origin);
            case RAID: return doRaid(army);
            default: return "";
        }
    }

    /**
     * Reports whether an action allows the army to advance this turn.
     *
     * @param action the action to query; {@code null} counts as allowed
     * @return {@code false} when the action pins the army in place
     */
    @Override
    public boolean permitsMovement(ArmyAction action) {
        return action != ArmyAction.HOLD && action != ArmyAction.FORTIFY;
    }

    /**
     * Reports whether the action triggers a second move on the same turn.
     *
     * @param action the action to query; {@code null} returns false
     * @return {@code true} only for {@link ArmyAction#FORCED_MARCH}
     */
    @Override
    public boolean doublesMovement(ArmyAction action) {
        return action == ArmyAction.FORCED_MARCH;
    }

    /* ===================== Individual action handlers ===================== */

    /**
     * Applies a fortify shield to the army for the next battle.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doFortify(Army army) {
        army.setFortifyShield(FORTIFY_FRACTION);
        return army.getName() + " fortifies its position.";
    }

    /**
     * Raises morale.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doRally(Army army) {
        army.setMorale(army.getMorale() + RALLY_BONUS);
        return army.getName() + " rallies its banners! Morale rises.";
    }

    /**
     * Heals each unit by a percentage of its current health.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doRegroup(Army army) {
        for (Unit unit : army.getUnits()) {
            int heal = Math.max(1, (int) Math.round(unit.getHealth() * REGROUP_PERCENT));
            unit.heal(heal);
        }
        return army.getName() + " regroups and the wounded recover.";
    }

    /**
     * Forced march: small morale tax, but doubled movement.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doForcedMarch(Army army) {
        army.setMorale(army.getMorale() - 0.05);
        return army.getName() + " presses on at a forced march.";
    }

    /**
     * Hold the line: tiny morale bonus, no advance.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doHold(Army army) {
        army.setMorale(army.getMorale() + 0.03);
        return army.getName() + " holds the line.";
    }

    /**
     * Skirmish: chip damage against enemy units sharing the current node.
     *
     * @param army the acting army
     * @param origin the army's current node
     * @return the narrative line
     */
    private String doSkirmish(Army army, Node origin) {
        if (origin == null) {
            return army.getName() + " probes the surroundings.";
        }
        int hits = 0;
        List<Army> here = origin.getArmies();
        for (Army other : here) {
            if (other.getTeam() == army.getTeam()) {
                continue;
            }
            for (Unit u : other.getUnits()) {
                u.takeDamage(SKIRMISH_DAMAGE);
                hits++;
            }
            other.removeDead();
        }
        return hits > 0
                ? army.getName() + " skirmishes for " + hits + " hits."
                : army.getName() + " finds no enemies to skirmish with.";
    }

    /**
     * Raid: temporary attack boost via morale, but it taxes next turn.
     *
     * @param army the acting army
     * @return the narrative line
     */
    private String doRaid(Army army) {
        army.setMorale(army.getMorale() + 0.20);
        army.setLastAction(army.getLastAction() + " (-morale after next battle)");
        // Penalty is realised when the simulator decays raid morale next turn.
        army.setFortifyShield(Math.max(0, army.getFortifyShield() - RAID_PENALTY));
        return army.getName() + " launches a reckless raid!";
    }
}
