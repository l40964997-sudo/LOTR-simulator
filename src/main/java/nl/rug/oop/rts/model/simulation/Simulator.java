package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.controller.PlayerController;
import nl.rug.oop.rts.controller.PlayerOrder;
import nl.rug.oop.rts.model.army.ActionExecutor;
import nl.rug.oop.rts.model.army.ActionStrategy;
import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.ArmyAction;
import nl.rug.oop.rts.model.battle.BattleResult;
import nl.rug.oop.rts.model.battle.BattleStrategy;
import nl.rug.oop.rts.model.battle.ComplexBattleStrategy;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;
import nl.rug.oop.rts.model.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Executes a single simulation step against a {@link Graph}.
 * <p>
 * A step now has eight phases, in order:
 * <ol>
 *   <li>resolve any battles already on the starting nodes;</li>
 *   <li>collect a per-army order (random AI roll or player-issued) and
 *       apply each action's immediate side effects;</li>
 *   <li>move every movement-permitting army along its chosen edge;</li>
 *   <li>resolve any battles that ensue on the edges;</li>
 *   <li>trigger one random event per army on its edge;</li>
 *   <li>move surviving armies off their edge onto the opposite node;</li>
 *   <li>resolve any battles on those destination nodes;</li>
 *   <li>trigger one random event per army on the destination node.</li>
 * </ol>
 * Pathing, battle and action behaviour are all pluggable; the default
 * wiring uses {@link SmartPathing}, {@link ComplexBattleStrategy} and a
 * fresh {@link ActionExecutor}.
 */
public class Simulator {

    /** Probability that an army encounters NO event when it has the chance. */
    private static final double NO_EVENT_PROBABILITY = 0.5;

    /** The graph being simulated. */
    private final Graph graph;
    /** Strategy used to resolve battles. */
    private final BattleStrategy battleStrategy;
    /** Strategy used to pick an outgoing edge. */
    private final PathingStrategy pathingStrategy;
    /** Action roller used for AI-controlled armies. */
    private final ActionStrategy actionStrategy;
    /** Random source for event triggering. */
    private final Random random;
    /** Reporter for narrative events and battles. The view subscribes to it. */
    private Consumer<String> reporter;
    /** Player controller consulted for player-flagged armies; may be null. */
    private PlayerController playerController;
    /** Edges chosen by the player this turn, consulted in the move phase. */
    private final Map<Army, Edge> playerEdges = new IdentityHashMap<>();

    /**
     * Convenience constructor wiring up the default strategies.
     *
     * @param graph the graph to simulate; must not be {@code null}
     */
    public Simulator(Graph graph) {
        this(graph, new ComplexBattleStrategy(), new SmartPathing(),
                new ActionExecutor(), new Random());
    }

    /**
     * Fully parametrised constructor used by tests and callers that want to
     * swap any of the strategy axes.
     *
     * @param graph           the graph to simulate
     * @param battleStrategy  the battle resolver
     * @param pathingStrategy the pathing algorithm
     * @param actionStrategy  the action picker / executor
     * @param random          the random source used for event chance
     */
    public Simulator(Graph graph, BattleStrategy battleStrategy,
                     PathingStrategy pathingStrategy, ActionStrategy actionStrategy,
                     Random random) {
        if (graph == null || battleStrategy == null || pathingStrategy == null
                || actionStrategy == null || random == null) {
            throw new IllegalArgumentException("Constructor arguments must be non-null");
        }
        this.graph = graph;
        this.battleStrategy = battleStrategy;
        this.pathingStrategy = pathingStrategy;
        this.actionStrategy = actionStrategy;
        this.random = random;
    }

    /**
     * Sets the reporter used to surface battle and event narratives.
     *
     * @param reporter a sink for narrative strings; {@code null} silences it
     */
    public void setReporter(Consumer<String> reporter) {
        this.reporter = reporter;
    }

    /**
     * Installs the player controller queried for player-flagged armies.
     *
     * @param playerController the controller; {@code null} disables it
     */
    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    /**
     * Executes one simulation step across every phase.
     */
    public void simulateStep() {
        resolveBattlesEverywhere(graph.getNodes());
        Map<Army, Node> originNodes = snapshotArmyOrigins();
        Map<Army, ArmyAction> orders = collectOrders(originNodes);
        executeActions(orders, originNodes);
        Map<Army, Edge> chosenEdges = moveArmiesToEdges(originNodes, orders);
        resolveBattlesEverywhere(graph.getEdges());
        triggerArrivedEvents(graph.getEdges(), chosenEdges.keySet());
        Map<Army, Node> destinations = moveArmiesToNodes(originNodes, chosenEdges);
        handleForcedMarch(orders, destinations);
        resolveBattlesEverywhere(graph.getNodes());
        triggerArrivedEvents(graph.getNodes(), destinations.keySet());
        purgeDefeatedArmies();
        graph.fireSimulationStepped();
    }

    /**
     * Records which node each army currently occupies before any movement.
     *
     * @return an identity map from army to its origin node
     */
    private Map<Army, Node> snapshotArmyOrigins() {
        Map<Army, Node> originNodes = new IdentityHashMap<>();
        for (Node node : graph.getNodes()) {
            for (Army army : node.getArmies()) {
                originNodes.put(army, node);
            }
        }
        return originNodes;
    }

    /**
     * Collects one action order per army; player armies block on the
     * controller and the resulting edge is stashed for the move phase.
     *
     * @param originNodes the army to origin-node mapping
     * @return the chosen action per army
     */
    private Map<Army, ArmyAction> collectOrders(Map<Army, Node> originNodes) {
        Map<Army, ArmyAction> orders = new IdentityHashMap<>();
        playerEdges.clear();
        for (Map.Entry<Army, Node> entry : originNodes.entrySet()) {
            Army army = entry.getKey();
            Node origin = entry.getValue();
            if (army.isPlayerControlled() && playerController != null) {
                PlayerOrder order = playerController.requestOrder(army, origin);
                if (order != null) {
                    orders.put(army, order.getAction());
                    if (order.getEdge() != null) {
                        playerEdges.put(army, order.getEdge());
                    }
                    continue;
                }
            }
            orders.put(army, actionStrategy.pickRandomAction());
        }
        return orders;
    }

    /**
     * Applies the side effects of each army's action.
     *
     * @param orders the per-army action map
     * @param originNodes the army origin node map
     */
    private void executeActions(Map<Army, ArmyAction> orders, Map<Army, Node> originNodes) {
        for (Map.Entry<Army, ArmyAction> entry : orders.entrySet()) {
            Army army = entry.getKey();
            ArmyAction action = entry.getValue();
            String line = actionStrategy.execute(army, action, originNodes.get(army));
            report(line);
        }
        graph.fireArmyChanged();
    }

    /**
     * Moves each army from its origin node onto a chosen incident edge.
     *
     * @param originNodes the army to origin-node mapping
     * @param orders the per-army action map (skips static actions)
     * @return an identity map from army to the edge it moved onto
     */
    private Map<Army, Edge> moveArmiesToEdges(Map<Army, Node> originNodes,
                                              Map<Army, ArmyAction> orders) {
        Map<Army, Edge> chosenEdges = new IdentityHashMap<>();
        for (Map.Entry<Army, Node> entry : originNodes.entrySet()) {
            Army army = entry.getKey();
            ArmyAction action = orders.get(army);
            if (!actionStrategy.permitsMovement(action)) {
                continue;
            }
            Edge picked = pickEdgeFor(army, entry.getValue());
            if (picked == null) {
                continue;
            }
            entry.getValue().removeArmy(army);
            picked.addArmy(army);
            chosenEdges.put(army, picked);
        }
        return chosenEdges;
    }

    /**
     * Picks an outgoing edge for an army, honouring the player's explicit
     * choice when one is recorded for this turn.
     *
     * @param army the army being routed
     * @param from the army's current node
     * @return the chosen edge, or {@code null} when none is available
     */
    private Edge pickEdgeFor(Army army, Node from) {
        Edge playerPick = playerEdges.get(army);
        if (playerPick != null && from.getEdges().contains(playerPick)) {
            return playerPick;
        }
        return pathingStrategy.selectNextEdge(army, from, from.getEdges());
    }

    /**
     * Moves surviving armies off their edge onto the opposite node.
     *
     * @param originNodes the army to origin-node mapping
     * @param chosenEdges the army to edge mapping from the outbound move
     * @return an identity map from army to its destination node
     */
    private Map<Army, Node> moveArmiesToNodes(Map<Army, Node> originNodes,
                                              Map<Army, Edge> chosenEdges) {
        Map<Army, Node> destinations = new IdentityHashMap<>();
        for (Map.Entry<Army, Edge> entry : chosenEdges.entrySet()) {
            Army army = entry.getKey();
            Edge edge = entry.getValue();
            if (!edge.getArmies().contains(army)) {
                continue;
            }
            Node target = edge.opposite(originNodes.get(army));
            edge.removeArmy(army);
            target.addArmy(army);
            destinations.put(army, target);
        }
        return destinations;
    }

    /**
     * For every army whose order was a forced march, take a second step.
     *
     * @param orders the per-army action map
     * @param destinations the destination map produced by the first move
     */
    private void handleForcedMarch(Map<Army, ArmyAction> orders, Map<Army, Node> destinations) {
        Map<Army, Node> origins = new IdentityHashMap<>();
        for (Map.Entry<Army, Node> entry : destinations.entrySet()) {
            if (actionStrategy.doublesMovement(orders.get(entry.getKey()))) {
                origins.put(entry.getKey(), entry.getValue());
            }
        }
        if (origins.isEmpty()) {
            return;
        }
        Map<Army, Edge> second = moveArmiesToEdges(origins, orders);
        Map<Army, Node> finalDest = moveArmiesToNodes(origins, second);
        destinations.putAll(finalDest);
    }

    /**
     * Triggers events on the given locations for the armies that arrived.
     *
     * @param locations the locations to inspect
     * @param arrived the armies that moved this phase
     */
    private void triggerArrivedEvents(List<? extends MapElement> locations, Set<Army> arrived) {
        Map<MapElement, List<Army>> active = collectActiveLocations(locations);
        triggerEvents(active, arrived);
    }

    /**
     * Resolves potential battles on every supplied location.
     *
     * @param locations the locations to check for battles
     */
    private void resolveBattlesEverywhere(List<? extends MapElement> locations) {
        for (MapElement location : new ArrayList<>(locations)) {
            List<Army> here = new ArrayList<>(location.getArmies());
            BattleResult result = battleStrategy.resolve(here, location.getName());
            if (result == null) {
                continue;
            }
            for (Army loser : result.getLosers()) {
                location.removeArmy(loser);
            }
            report(result.getDescription());
        }
        graph.fireArmyChanged();
    }

    /**
     * Iterates locations and groups them by element, retaining only those
     * that actually have armies on them.
     *
     * @param locations the locations to inspect
     * @return a map from active location to a snapshot of its armies
     */
    private Map<MapElement, List<Army>> collectActiveLocations(List<? extends MapElement> locations) {
        Map<MapElement, List<Army>> out = new HashMap<>();
        for (MapElement element : locations) {
            if (!element.getArmies().isEmpty()) {
                out.put(element, new ArrayList<>(element.getArmies()));
            }
        }
        return out;
    }

    /**
     * Triggers one random event per army on each active location.
     *
     * @param locations map of locations to the armies that just arrived
     * @param eligible only these armies may roll for an event
     */
    private void triggerEvents(Map<MapElement, List<Army>> locations, Iterable<Army> eligible) {
        if (locations.isEmpty()) {
            return;
        }
        Map<Army, Boolean> eligibleSet = new IdentityHashMap<>();
        for (Army a : eligible) {
            eligibleSet.put(a, Boolean.TRUE);
        }
        for (Map.Entry<MapElement, List<Army>> entry : locations.entrySet()) {
            List<GameEvent> available = entry.getKey().getEvents();
            if (available.isEmpty()) {
                continue;
            }
            rollEventsForLocation(entry.getValue(), available, eligibleSet);
        }
        graph.fireArmyChanged();
    }

    /**
     * Performs the event roll for each eligible army on one location.
     *
     * @param hereArmies the armies on the location
     * @param available the events available at the location
     * @param eligibleSet the set of armies that just arrived
     */
    private void rollEventsForLocation(List<Army> hereArmies, List<GameEvent> available,
                                       Map<Army, Boolean> eligibleSet) {
        for (Army army : hereArmies) {
            if (!eligibleSet.containsKey(army)) {
                continue;
            }
            if (random.nextDouble() < NO_EVENT_PROBABILITY) {
                continue;
            }
            GameEvent event = available.get(random.nextInt(available.size()));
            String description = event.applyTo(army);
            report(description);
        }
    }

    /**
     * Removes empty armies from the entire graph at the end of a step.
     */
    private void purgeDefeatedArmies() {
        for (Node n : graph.getNodes()) {
            for (Army a : new ArrayList<>(n.getArmies())) {
                if (a.isDefeated()) {
                    n.removeArmy(a);
                }
            }
        }
        for (Edge e : graph.getEdges()) {
            for (Army a : new ArrayList<>(e.getArmies())) {
                if (a.isDefeated()) {
                    e.removeArmy(a);
                }
            }
        }
    }

    /**
     * Convenience that funnels a narrative string to the reporter, when set.
     *
     * @param message the narrative text to report
     */
    private void report(String message) {
        if (reporter != null && message != null && !message.isBlank()) {
            reporter.accept(message);
        }
    }
}
