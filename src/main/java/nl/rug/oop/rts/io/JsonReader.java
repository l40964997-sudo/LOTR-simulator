package nl.rug.oop.rts.io;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.army.Unit;
import nl.rug.oop.rts.model.army.UnitFactory;
import nl.rug.oop.rts.model.event.EventFactory;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.EdgeType;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.model.graph.Wealth;
import nl.rug.oop.rts.util.IdGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-written JSON reader for the save format produced by {@link JsonWriter}.
 * <p>
 * Implemented as a tiny recursive-descent parser. The grammar is the JSON
 * subset required by the assignment: objects, arrays, strings, numbers,
 * booleans, and {@code null}. No external library is used, in line with
 * the spec.
 * <p>
 * Provided as a bonus to support the "loading from JSON" extra. The class
 * is robust against missing optional fields: unknown faction names, empty
 * armies, missing event entries, and out-of-order keys are all tolerated.
 * <p>
 * Errors during parsing are reported as {@link JsonParseException}, so
 * the UI can surface a single friendly dialog rather than a stack trace.
 */
public class JsonReader {

    /** JSON key for an element name. */
    private static final String KEY_NAME = "Name";

    /** JSON key for a numeric identifier. */
    private static final String KEY_ID = "Id";

    /** JSON key for the armies array. */
    private static final String KEY_ARMIES = "Armies";

    /** JSON key for the events array. */
    private static final String KEY_EVENTS = "Events";

    /** Parses the raw JSON document into generic Java objects. */
    private final JsonParser parser = new JsonParser();

    /** Unit factory used to rebuild units with explicit stats. */
    private final UnitFactory unitFactory = new UnitFactory();

    /**
     * Reads a graph from disk.
     *
     * @param file the JSON file
     * @return the deserialised graph
     * @throws IOException if reading the file fails
     * @throws JsonParseException if the contents do not match the expected format
     */
    public Graph loadFromFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        String text = Files.readString(Path.of(file.toURI()));
        return parse(text);
    }

    /**
     * Parses a JSON document into a graph.
     *
     * @param text the document
     * @return the graph
     * @throws JsonParseException on malformed input
     */
    public Graph parse(String text) {
        Object root = parser.parse(text);
        if (!(root instanceof Map)) {
            throw new JsonParseException("Expected top-level JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) root;
        return buildGraph(rootMap);
    }

    /* ===================== Graph reconstruction ===================== */

    /**
     * Builds the graph from the parsed root object.
     *
     * @param rootMap the parsed top-level JSON object
     * @return the reconstructed graph
     */
    private Graph buildGraph(Map<String, Object> rootMap) {
        Graph graph = new Graph();
        Map<Integer, Node> byOldId = new HashMap<>();
        Object nodesObj = rootMap.get("Nodes");
        if (nodesObj instanceof List<?> nodesList) {
            for (Object n : nodesList) {
                if (n instanceof Map<?, ?> nMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) nMap;
                    Node newNode = readNode(graph, typed);
                    Object idObj = typed.get("Id");
                    if (idObj instanceof Number num) {
                        byOldId.put(num.intValue(), newNode);
                    }
                }
            }
        }
        Object edgesObj = rootMap.get("Edges");
        if (edgesObj instanceof List<?> edgesList) {
            for (Object e : edgesList) {
                if (e instanceof Map<?, ?> eMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) eMap;
                    readEdge(graph, typed, byOldId);
                }
            }
        }
        return graph;
    }

    /**
     * Reconstructs a single node and adds it to the graph.
     *
     * @param graph the graph being built
     * @param nodeMap the parsed JSON object for the node
     * @return the created node
     */
    private Node readNode(Graph graph, Map<String, Object> nodeMap) {
        Object xObj = nodeMap.get("X");
        Object yObj = nodeMap.get("Y");
        int x;
        int y;
        if (xObj instanceof Number xNum && yObj instanceof Number yNum) {
            x = xNum.intValue();
            y = yNum.intValue();
        } else {
            // Older save without coordinates: lay out on a grid by insertion order.
            int idx = graph.getNodes().size();
            x = 80 + (idx % 5) * 140;
            y = 80 + (idx / 5) * 120;
        }
        Node node = graph.addNode(x, y);
        Object nameObj = nodeMap.get(KEY_NAME);
        if (nameObj instanceof String name) {
            node.setName(name);
        }
        Object idObj = nodeMap.get(KEY_ID);
        if (idObj instanceof Number number) {
            IdGenerator.seedNodeIdAtLeast(number.intValue());
        }
        applyNodeSettlementFields(node, nodeMap);
        parseArmiesInto(node, nodeMap.get(KEY_ARMIES));
        parseEventsInto(node, nodeMap.get(KEY_EVENTS));
        return node;
    }

    /**
     * Applies the settlement-level fields (faction, demographics, wealth,
     * language) to the freshly created node. Missing or unknown values are
     * silently ignored so older save files still load.
     *
     * @param node the node being reconstructed
     * @param nodeMap the parsed JSON object for the node
     */
    private void applyNodeSettlementFields(Node node, Map<String, Object> nodeMap) {
        if (nodeMap.get("Faction") instanceof String fName) {
            Faction f = Faction.fromDisplayName(fName);
            if (f != null) {
                node.setFaction(f);
            }
        }
        if (nodeMap.get("Population") instanceof Number pop) {
            node.setPopulation(pop.intValue());
        }
        if (nodeMap.get("Literacy") instanceof Number lit) {
            node.setLiteracy(lit.intValue());
        }
        if (nodeMap.get("Wealth") instanceof String wName) {
            Wealth w = Wealth.fromDisplayName(wName);
            if (w != null) {
                node.setWealth(w);
            }
        }
        if (nodeMap.get("Language") instanceof String lang && !lang.isBlank()) {
            node.setLanguage(lang);
        }
    }

    /**
     * Parses an armies array and attaches each army to the element.
     *
     * @param element the node or edge receiving the armies
     * @param armiesObj the raw value found under the armies key
     */
    private void parseArmiesInto(nl.rug.oop.rts.model.graph.MapElement element, Object armiesObj) {
        if (!(armiesObj instanceof List<?> armiesList)) {
            return;
        }
        for (Object entry : armiesList) {
            if (entry instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) rawMap;
                Army army = readArmy(typed);
                if (army != null) {
                    element.addArmy(army);
                }
            }
        }
    }

    /**
     * Parses an events array and attaches each known event to the element.
     *
     * @param element the node or edge receiving the events
     * @param eventsObj the raw value found under the events key
     */
    private void parseEventsInto(nl.rug.oop.rts.model.graph.MapElement element, Object eventsObj) {
        if (!(eventsObj instanceof List<?> eventsList)) {
            return;
        }
        for (Object entry : eventsList) {
            if (entry instanceof String name) {
                GameEvent event = EventFactory.fromName(name);
                if (event != null) {
                    element.addEvent(event);
                }
            }
        }
    }

    /**
     * Reconstructs a single edge and adds it to the graph.
     *
     * @param graph the graph being built
     * @param edgeMap the parsed JSON object for the edge
     * @param byOldId mapping from saved node id to reconstructed node
     */
    private void readEdge(Graph graph, Map<String, Object> edgeMap, Map<Integer, Node> byOldId) {
        Edge edge = createEdgeFrom(graph, edgeMap, byOldId);
        if (edge == null) {
            return;
        }
        Object nameObj = edgeMap.get(KEY_NAME);
        if (nameObj instanceof String name) {
            edge.setName(name);
        }
        Object idObj = edgeMap.get(KEY_ID);
        if (idObj instanceof Number number) {
            IdGenerator.seedEdgeIdAtLeast(number.intValue());
        }
        if (edgeMap.get("Terrain") instanceof String terrain) {
            EdgeType type = EdgeType.fromDisplayName(terrain);
            if (type != null) {
                edge.setEdgeType(type);
            }
        }
        parseArmiesInto(edge, edgeMap.get(KEY_ARMIES));
        parseEventsInto(edge, edgeMap.get(KEY_EVENTS));
    }

    /**
     * Resolves the two endpoints and creates the edge, or returns
     * {@code null} when the endpoints are missing or invalid.
     *
     * @param graph the graph being built
     * @param edgeMap the parsed JSON object for the edge
     * @param byOldId mapping from saved node id to reconstructed node
     * @return the new edge, or {@code null}
     */
    private Edge createEdgeFrom(Graph graph, Map<String, Object> edgeMap,
                                Map<Integer, Node> byOldId) {
        if (!(edgeMap.get("Node1") instanceof Number n1)
                || !(edgeMap.get("Node2") instanceof Number n2)) {
            return null;
        }
        Node a = byOldId.get(n1.intValue());
        Node b = byOldId.get(n2.intValue());
        if (a == null || b == null || a == b) {
            return null;
        }
        return graph.addEdge(a, b);
    }

    /**
     * Reconstructs an army from its parsed JSON object.
     *
     * @param armyMap the parsed army object
     * @return the army, or {@code null} if the faction is missing or unknown
     */
    private Army readArmy(Map<String, Object> armyMap) {
        Object factionObj = armyMap.get("Faction");
        if (!(factionObj instanceof String factionName)) {
            return null;
        }
        Faction faction = Faction.fromDisplayName(factionName);
        if (faction == null) {
            return null;
        }
        Object nameObj = armyMap.get(KEY_NAME);
        String name = nameObj instanceof String s ? s : null;
        List<Unit> units = new ArrayList<>();
        Object unitsObj = armyMap.get("Units");
        if (unitsObj instanceof List<?> unitsList) {
            for (Object u : unitsList) {
                if (u instanceof Map<?, ?> uMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) uMap;
                    Unit unit = readUnit(typed, faction);
                    if (unit != null) {
                        units.add(unit);
                    }
                }
            }
        }
        Army army = new Army(name, faction, units);
        if (armyMap.get("PlayerControlled") instanceof Boolean controlled) {
            army.setPlayerControlled(controlled);
        }
        return army;
    }

    /**
     * Reconstructs a single unit from its parsed JSON object.
     *
     * @param uMap the parsed unit object
     * @param faction the faction the unit belongs to
     * @return the unit, or {@code null} if the name is missing or blank
     */
    private Unit readUnit(Map<String, Object> uMap, Faction faction) {
        Object nameObj = uMap.get(KEY_NAME);
        if (!(nameObj instanceof String name) || name.isBlank()) {
            return null;
        }
        int strength = readIntField(uMap, "Strength", 10);
        int health = readIntField(uMap, "Health", 100);
        return unitFactory.createUnitWithStats(faction, name, strength, health);
    }

    /**
     * Reads an integer field with a fallback when absent or non-numeric.
     *
     * @param map the parsed object
     * @param key the field key
     * @param fallback the value to use if the key is missing
     * @return the integer value or the fallback
     */
    private int readIntField(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        return fallback;
    }

}
