package nl.rug.oop.rts.io;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.army.Unit;
import nl.rug.oop.rts.model.army.UnitFactory;
import nl.rug.oop.rts.model.event.EventFactory;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;
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

    /** Source string being parsed. */
    private String source = "";

    /** Current cursor position. */
    private int pos;

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
        this.source = text == null ? "" : text;
        this.pos = 0;
        skipWhitespace();
        Object root = readValue();
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
        // Coordinates are not part of the JSON spec, so lay nodes out on a
        // simple grid by insertion order for post-load inspection.
        int idx = graph.getNodes().size();
        Node node = graph.addNode(80 + (idx % 5) * 140, 80 + (idx / 5) * 120);
        Object nameObj = nodeMap.get(KEY_NAME);
        if (nameObj instanceof String name) {
            node.setName(name);
        }
        Object idObj = nodeMap.get(KEY_ID);
        if (idObj instanceof Number number) {
            IdGenerator.seedNodeIdAtLeast(number.intValue());
        }
        parseArmiesInto(node, nodeMap.get(KEY_ARMIES));
        parseEventsInto(node, nodeMap.get(KEY_EVENTS));
        return node;
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
        Object n1 = edgeMap.get("Node1");
        Object n2 = edgeMap.get("Node2");
        if (!(n1 instanceof Number) || !(n2 instanceof Number)) {
            return null;
        }
        Node a = byOldId.get(((Number) n1).intValue());
        Node b = byOldId.get(((Number) n2).intValue());
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
        return new Army(name, faction, units);
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

    /* ===================== Recursive descent ===================== */

    /**
     * Reads any JSON value at the cursor.
     *
     * @return the parsed value: a map, list, string, number, boolean or null
     */
    private Object readValue() {
        skipWhitespace();
        if (pos >= source.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = source.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            default -> readScalar(c);
        };
    }

    /**
     * Reads a scalar value (number, boolean or null) at the cursor.
     *
     * @param c the lookahead character
     * @return the parsed scalar
     */
    private Object readScalar(char c) {
        if (c == '-' || (c >= '0' && c <= '9')) {
            return readNumber();
        }
        if (matchKeyword("true")) {
            return Boolean.TRUE;
        }
        if (matchKeyword("false")) {
            return Boolean.FALSE;
        }
        if (matchKeyword("null")) {
            return null;
        }
        throw new JsonParseException("Unexpected character '" + c + "' at position " + pos);
    }

    /**
     * Reads a JSON object at the cursor.
     *
     * @return a map of the object's keys to values
     */
    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> map = new HashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        boolean more = true;
        while (more) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            map.put(key, readValue());
            skipWhitespace();
            more = consumeSeparator('}');
        }
        return map;
    }

    /**
     * Reads a JSON array at the cursor.
     *
     * @return a list of the array's values
     */
    private List<Object> readArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        boolean more = true;
        while (more) {
            list.add(readValue());
            skipWhitespace();
            more = consumeSeparator(']');
        }
        return list;
    }

    /**
     * Consumes a separator after a collection element: a comma to continue
     * or the closing bracket to stop.
     *
     * @param closing the closing bracket character expected at the end
     * @return {@code true} if more elements follow, {@code false} if closed
     */
    private boolean consumeSeparator(char closing) {
        char c = peek();
        if (c == ',') {
            pos++;
            return true;
        }
        if (c == closing) {
            pos++;
            return false;
        }
        throw new JsonParseException("Expected ',' or '" + closing + "' at position " + pos);
    }

    /**
     * Reads a quoted JSON string at the cursor, resolving escape sequences.
     *
     * @return the unescaped string content
     */
    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            pos++;
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                appendEscape(sb);
            } else {
                sb.append(c);
            }
        }
        throw new JsonParseException("Unterminated string");
    }

    /**
     * Reads and appends one escape sequence to the builder.
     *
     * @param sb the builder receiving the decoded character
     */
    private void appendEscape(StringBuilder sb) {
        if (pos >= source.length()) {
            throw new JsonParseException("Dangling backslash");
        }
        char esc = source.charAt(pos);
        pos++;
        switch (esc) {
            case '"' -> sb.append('"');
            case '\\' -> sb.append('\\');
            case '/' -> sb.append('/');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'u' -> appendUnicodeEscape(sb);
            default -> throw new JsonParseException("Bad escape");
        }
    }

    /**
     * Reads a four hex digit unicode escape and appends the character.
     *
     * @param sb the builder receiving the decoded character
     */
    private void appendUnicodeEscape(StringBuilder sb) {
        if (pos + 4 > source.length()) {
            throw new JsonParseException("Bad unicode escape");
        }
        int codePoint = Integer.parseInt(source.substring(pos, pos + 4), 16);
        sb.append((char) codePoint);
        pos += 4;
    }

    /**
     * Reads a JSON number at the cursor.
     *
     * @return a {@link Long} for integers or a {@link Double} otherwise
     */
    private Number readNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < source.length() && isNumberChar(source.charAt(pos))) {
            pos++;
        }
        String token = source.substring(start, pos);
        return parseNumberToken(token);
    }

    /**
     * Reports whether a character can appear inside a JSON number token.
     *
     * @param c the character to test
     * @return {@code true} if it is a digit, sign, dot or exponent marker
     */
    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-';
    }

    /**
     * Parses a numeric token into a boxed number.
     *
     * @param token the textual number
     * @return the parsed value
     */
    private Number parseNumberToken(String token) {
        try {
            if (token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                return Double.valueOf(token);
            }
            return Long.valueOf(token);
        } catch (NumberFormatException ex) {
            throw new JsonParseException("Bad number '" + token + "'");
        }
    }

    /* ===================== Lexing utilities ===================== */

    /**
     * Attempts to match a literal keyword at the cursor, advancing on match.
     *
     * @param keyword the keyword to match
     * @return {@code true} if matched and consumed
     */
    private boolean matchKeyword(String keyword) {
        if (pos + keyword.length() > source.length()) {
            return false;
        }
        if (source.regionMatches(pos, keyword, 0, keyword.length())) {
            pos += keyword.length();
            return true;
        }
        return false;
    }

    /**
     * Consumes the expected character or throws.
     *
     * @param c the required character
     */
    private void expect(char c) {
        if (pos >= source.length() || source.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    /**
     * Returns the character at the cursor without consuming it.
     *
     * @return the lookahead character, or NUL at end of input
     */
    private char peek() {
        if (pos >= source.length()) {
            return '\0';
        }
        return source.charAt(pos);
    }

    /**
     * Advances the cursor past any whitespace.
     */
    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }

    /**
     * Thrown when the input does not match the expected JSON grammar.
     */
    public static class JsonParseException extends RuntimeException {

        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the exception with a descriptive message.
         *
         * @param message human readable diagnostic
         */
        public JsonParseException(String message) {
            super(message);
        }
    }
}
