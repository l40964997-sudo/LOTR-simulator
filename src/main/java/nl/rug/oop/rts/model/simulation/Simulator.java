package nl.rug.oop.rts.model.simulation;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.battle.BattleResult;
import nl.rug.oop.rts.model.battle.BattleStrategy;
import nl.rug.oop.rts.model.battle.StandardBattleStrategy;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;
import nl.rug.oop.rts.model.graph.Node;

import java.util.*;
import java.util.function.Consumer;

/**
 * Executes a single simulation step against a {@link Graph}.
 * <p>
 * A step has seven phases, in order:
 * <ol>
 *   <li>resolve any battles on the starting nodes;</li>
 *   <li>move every army to a random outgoing edge of its current node;</li>
 *   <li>resolve any battles that ensue on the edges;</li>
 *   <li>trigger one random event per army on its edge (50% chance of nothing);</li>
 *   <li>move every army to the opposite endpoint of its edge;</li>
 *   <li>resolve any battles on those destination nodes;</li>
 *   <li>trigger one random event per army on the destination node.</li>
 * </ol>
 * The simulator does <em>not</em> mutate selection state and does not know
 * about the view: it talks to the model and reports back through
 * {@link Graph#fireSimulationStepped()} once the step is done.
 * <p>
 * Battle and pathing decisions are pluggable through the constructor,
 * keeping coupling between this class and the algorithmic details low.
 * Test runs of the program inject a deterministic {@link Random} to make
 * the otherwise stochastic behaviour reproducible.
 */
public class Simulator {

    /** The graph being simulated. */
    private final Graph graph;

    /** Strategy used to resolve battles. */
    private final BattleStrategy battleStrategy;

    /** Strategy used to pick an outgoing edge. */
    private final PathingStrategy pathingStrategy;

    /** Random source for event triggering. */
    private final Random random;

    /** Reporter for narrative events and battles. The view subscribes to it. */
    private Consumer<String> reporter;

    /** Probability that an army encounters NO event when it has the chance. */
    private static final double NO_EVENT_PROBABILITY = 0.5;

    /**
     * Convenience constructor wiring up the default strategies.
     *
     * @param graph the graph to simulate; must not be {@code null}
     */
    public Simulator(Graph graph) {
        this(graph, new StandardBattleStrategy(), new RandomPathing(), new Random());
    }

    /**
     * Fully parametrised constructor.
     *
     * @param graph           the graph to simulate
     * @param battleStrategy  the battle resolver
     * @param pathingStrategy the pathing algorithm
     * @param random          the random source used for event chance
     */
    public Simulator(Graph graph, BattleStrategy battleStrategy,
                     PathingStrategy pathingStrategy, Random random) {
        if (graph == null || battleStrategy == null || pathingStrategy == null || random == null) {
            throw new IllegalArgumentException("Constructor arguments must be non-null");
        }
        this.graph = graph;
        this.battleStrategy = battleStrategy;
        this.pathingStrategy = pathingStrategy;
        this.random = random;
    }

    /**
     * Sets the reporter used to surface battle and event narratives. The
     * controller typically points this at a popup dialog. Pass {@code null}
     * to silence reporting.
     *
     * @param reporter a sink for narrative strings
     */
    public void setReporter(Consumer<String> reporter) {
        this.reporter = reporter;
    }

    /* ===================== Public API ===================== */

    /**
     * Executes one simulation step across all seven phases.
     */
    public void simulateStep() {
        resolveBattlesEverywhere(graph.getNodes());
        Map<Army, Node> originNodes = snapshotArmyOrigins();
        Map<Army, Edge> chosenEdges = moveArmiesToEdges(originNodes);
        resolveBattlesEverywhere(graph.getEdges());
        triggerArrivedEvents(graph.getEdges(), chosenEdges.keySet());
        Map<Army, Node> destinations = moveArmiesToNodes(originNodes, chosenEdges);
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
     * Moves each army from its origin node onto a chosen incident edge.
     *
     * @param originNodes the army to origin-node mapping
     * @return an identity map from army to the edge it moved onto
     */
    private Map<Army, Edge> moveArmiesToEdges(Map<Army, Node> originNodes) {
        Map<Army, Edge> chosenEdges = new IdentityHashMap<>();
        for (Map.Entry<Army, Node> entry : originNodes.entrySet()) {
            Army army = entry.getKey();
            Node from = entry.getValue();
            Edge picked = pathingStrategy.selectNextEdge(army, from, from.getEdges());
            if (picked == null) {
                continue;
            }
            from.removeArmy(army);
            picked.addArmy(army);
            chosenEdges.put(army, picked);
        }
        return chosenEdges;
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
     * Triggers events on the given locations for the armies that arrived.
     *
     * @param locations the locations to inspect
     * @param arrived the armies that moved this phase
     */
    private void triggerArrivedEvents(List<? extends MapElement> locations,
                                      java.util.Set<Army> arrived) {
        Map<MapElement, List<Army>> active = collectActiveLocations(locations);
        triggerEvents(active, arrived);
    }

    /* ===================== Private helpers ===================== */

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
            // Remove every defeated army from the location and the graph.
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
     * Triggers one random event per army on each active location, subject
     * to the {@link #NO_EVENT_PROBABILITY} skip chance.
     *
     * @param locations map of locations to the armies that just arrived
     * @param eligible only these armies may roll for an event (those that
     *                 just arrived this phase)
     */
    private void triggerEvents(Map<MapElement, List<Army>> locations, Iterable<Army> eligible) {
        if (locations.isEmpty()) {
            return;
        }
        // Use identity hashing because two armies may share faction/units yet
        // still be distinct combatants.
        Map<Army, Boolean> eligibleSet = new IdentityHashMap<>();
        for (Army a : eligible) {
            eligibleSet.put(a, Boolean.TRUE);
        }
        for (Map.Entry<MapElement, List<Army>> entry : locations.entrySet()) {
            MapElement location = entry.getKey();
            List<GameEvent> available = location.getEvents();
            if (available.isEmpty()) {
                continue;
            }
            for (Army army : entry.getValue()) {
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
        graph.fireArmyChanged();
    }

    /**
     * Removes empty armies from the entire graph at the end of a step.
     */
    private void purgeDefeatedArmies() {
        for (Node n : graph.getNodes()) {
            List<Army> snapshot = new ArrayList<>(n.getArmies());
            for (Army a : snapshot) {
                if (a.isDefeated()) {
                    n.removeArmy(a);
                }
            }
        }
        for (Edge e : graph.getEdges()) {
            List<Army> snapshot = new ArrayList<>(e.getArmies());
            for (Army a : snapshot) {
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
