package nl.rug.oop.rts.io;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Unit;
import nl.rug.oop.rts.model.event.GameEvent;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.MapElement;
import nl.rug.oop.rts.model.graph.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Hand rolled JSON exporter for the simulator state.
 * <p>
 * The assignment explicitly forbids using JSON libraries, hence the
 * verbose-but-deterministic indentation logic. The exporter follows the
 * format described in the assignment specification:
 *
 * <pre>{@code
 * {
 *   "Nodes": [ <Node>, ... ],
 *   "Edges": [ <Edge>, ... ]
 * }
 * }</pre>
 *
 * The writer is implemented as a stateful helper rather than as
 * {@code static} methods so that the indentation depth can be tracked
 * cleanly without thread local kludges. Each public {@code write*} method
 * emits a self-contained subtree; the lower-level helpers
 * ({@link #writeLine(String)}, {@link #indent()}, {@link #dedent()}) keep
 * the indentation contract in one place.
 * <p>
 * Robustness notes:
 * <ul>
 *   <li>The exporter never throws on {@code null} optional sub-fields; it
 *       writes an empty array instead, matching the spec.</li>
 *   <li>String values are properly escaped (quotes, backslashes, newlines,
 *       tabs and the four other JSON control characters) by
 *       {@link #escape(String)}.</li>
 *   <li>The target file is forced to end in {@code .json} by
 *       {@link #saveToFile(Graph, File)}, so the user cannot easily lose
 *       the output by typing a stray extension.</li>
 * </ul>
 */
public class JsonWriter {

    /** Indentation step in spaces. */
    private static final String INDENT_UNIT = "  ";

    /** Buffer that accumulates output before flushing. */
    private final StringBuilder buffer = new StringBuilder();

    /** Current indentation level (number of {@link #INDENT_UNIT}s). */
    private int level;

    /**
     * Serialises the supplied graph to its JSON representation.
     *
     * @param graph the graph to serialise; must not be {@code null}
     * @return a complete JSON document
     */
    public String toJsonString(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        buffer.setLength(0);
        level = 0;
        writeGraph(graph);
        return buffer.toString();
    }

    /**
     * Writes the graph to a UTF-8 file, ensuring the {@code .json} extension.
     *
     * @param graph the graph to serialise
     * @param file the destination file; the extension is forced to {@code .json}
     * @return the actual file written to (with the corrected extension)
     * @throws IOException if writing fails
     */
    public File saveToFile(Graph graph, File file) throws IOException {
        if (graph == null || file == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }
        File target = file;
        String name = target.getName().toLowerCase();
        if (!name.endsWith(".json")) {
            target = new File(target.getParentFile(), target.getName() + ".json");
        }
        String json = toJsonString(graph);
        try (Writer writer = new BufferedWriter(
                Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8))) {
            writer.write(json);
        }
        return target;
    }

    /* ===================== Subtrees ===================== */

    /**
     * Writes the top-level graph object.
     *
     * @param graph the graph to serialise
     */
    private void writeGraph(Graph graph) {
        writeLine("{");
        indent();
        writeNodesArray(graph.getNodes());
        appendLineComma();
        writeEdgesArray(graph.getEdges());
        appendNewline();
        dedent();
        writeIndent();
        buffer.append("}");
    }

    /**
     * Writes the "Nodes" array.
     *
     * @param nodes the nodes to write
     */
    private void writeNodesArray(List<Node> nodes) {
        writeIndent();
        buffer.append("\"Nodes\": [");
        if (nodes.isEmpty()) {
            buffer.append(']');
            return;
        }
        appendNewline();
        indent();
        for (int i = 0; i < nodes.size(); i++) {
            writeNode(nodes.get(i));
            if (i < nodes.size() - 1) {
                buffer.append(',');
            }
            appendNewline();
        }
        dedent();
        writeIndent();
        buffer.append(']');
    }

    /**
     * Writes the "Edges" array.
     *
     * @param edges the edges to write
     */
    private void writeEdgesArray(List<Edge> edges) {
        writeIndent();
        buffer.append("\"Edges\": [");
        if (edges.isEmpty()) {
            buffer.append(']');
            return;
        }
        appendNewline();
        indent();
        for (int i = 0; i < edges.size(); i++) {
            writeEdge(edges.get(i));
            if (i < edges.size() - 1) {
                buffer.append(',');
            }
            appendNewline();
        }
        dedent();
        writeIndent();
        buffer.append(']');
    }

    /**
     * Writes a single node object.
     *
     * @param node the node to write
     */
    private void writeNode(Node node) {
        writeIndent();
        buffer.append('{');
        appendNewline();
        indent();
        writeNumberField("Id", node.getId());
        appendLineComma();
        writeStringField("Name", node.getName());
        appendLineComma();
        writeArmiesArray("Armies", node.getArmies());
        appendLineComma();
        writeEventsArray(node);
        appendNewline();
        dedent();
        writeIndent();
        buffer.append('}');
    }

    /**
     * Writes a single edge object.
     *
     * @param edge the edge to write
     */
    private void writeEdge(Edge edge) {
        writeIndent();
        buffer.append('{');
        appendNewline();
        indent();
        writeNumberField("Id", edge.getId());
        appendLineComma();
        writeStringField("Name", edge.getName());
        appendLineComma();
        writeNumberField("Node1", edge.getNodeA().getId());
        appendLineComma();
        writeNumberField("Node2", edge.getNodeB().getId());
        appendLineComma();
        writeArmiesArray("Armies", edge.getArmies());
        appendLineComma();
        writeEventsArray(edge);
        appendNewline();
        dedent();
        writeIndent();
        buffer.append('}');
    }

    /**
     * Writes a named array of armies.
     *
     * @param fieldName the JSON field name
     * @param armies the armies to write
     */
    private void writeArmiesArray(String fieldName, List<Army> armies) {
        writeIndent();
        buffer.append('"').append(fieldName).append("\": [");
        if (armies == null || armies.isEmpty()) {
            buffer.append(']');
            return;
        }
        appendNewline();
        indent();
        for (int i = 0; i < armies.size(); i++) {
            writeArmy(armies.get(i));
            if (i < armies.size() - 1) {
                buffer.append(',');
            }
            appendNewline();
        }
        dedent();
        writeIndent();
        buffer.append(']');
    }

    /**
     * Writes the "Events" array of an element.
     *
     * @param element the element whose events are written
     */
    private void writeEventsArray(MapElement element) {
        writeIndent();
        buffer.append("\"Events\": [");
        List<GameEvent> events = element.getEvents();
        if (events.isEmpty()) {
            buffer.append(']');
            return;
        }
        appendNewline();
        indent();
        for (int i = 0; i < events.size(); i++) {
            writeIndent();
            buffer.append('"').append(escape(events.get(i).getName())).append('"');
            if (i < events.size() - 1) {
                buffer.append(',');
            }
            appendNewline();
        }
        dedent();
        writeIndent();
        buffer.append(']');
    }

    /**
     * Writes a single army object.
     *
     * @param army the army to write
     */
    private void writeArmy(Army army) {
        writeIndent();
        buffer.append('{');
        appendNewline();
        indent();
        writeStringField("Name", army.getName());
        appendLineComma();
        writeStringField("Faction", army.getFaction().getDisplayName());
        appendLineComma();
        writeNumberField("Team", army.getTeam());
        appendLineComma();
        writeUnitsArray(army.getUnits());
        appendNewline();
        dedent();
        writeIndent();
        buffer.append('}');
    }

    /**
     * Writes the "Units" array of an army.
     *
     * @param units the units to write
     */
    private void writeUnitsArray(List<Unit> units) {
        writeIndent();
        buffer.append("\"Units\": [");
        if (units == null || units.isEmpty()) {
            buffer.append(']');
            return;
        }
        appendNewline();
        indent();
        for (int i = 0; i < units.size(); i++) {
            writeUnit(units.get(i));
            if (i < units.size() - 1) {
                buffer.append(',');
            }
            appendNewline();
        }
        dedent();
        writeIndent();
        buffer.append(']');
    }

    /**
     * Writes a single unit object.
     *
     * @param unit the unit to write
     */
    private void writeUnit(Unit unit) {
        writeIndent();
        buffer.append('{');
        appendNewline();
        indent();
        writeStringField("Name", unit.getName());
        appendLineComma();
        writeNumberField("Strength", unit.getStrength());
        appendLineComma();
        writeNumberField("Health", unit.getHealth());
        appendNewline();
        dedent();
        writeIndent();
        buffer.append('}');
    }

    /* ===================== Low level helpers ===================== */

    /**
     * Writes a string key/value field.
     *
     * @param key the field name
     * @param value the string value
     */
    private void writeStringField(String key, String value) {
        writeIndent();
        buffer.append('"').append(escape(key)).append("\": \"")
                .append(escape(value)).append('"');
    }

    /**
     * Writes a numeric key/value field.
     *
     * @param key the field name
     * @param value the numeric value
     */
    private void writeNumberField(String key, long value) {
        writeIndent();
        buffer.append('"').append(escape(key)).append("\": ").append(value);
    }

    /**
     * Writes an indented line followed by a newline.
     *
     * @param text the text to write
     */
    private void writeLine(String text) {
        writeIndent();
        buffer.append(text);
        appendNewline();
    }

    /**
     * Appends a comma and a newline.
     */
    private void appendLineComma() {
        buffer.append(',');
        appendNewline();
    }

    /**
     * Appends a single newline.
     */
    private void appendNewline() {
        buffer.append('\n');
    }

    /**
     * Appends the current indentation.
     */
    private void writeIndent() {
        for (int i = 0; i < level; i++) {
            buffer.append(INDENT_UNIT);
        }
    }

    /**
     * Increases the indentation level by one.
     */
    private void indent() {
        level++;
    }

    /**
     * Decreases the indentation level by one, not below zero.
     */
    private void dedent() {
        level = Math.max(0, level - 1);
    }

    /**
     * Escapes a JSON string content per RFC 8259.
     *
     * @param raw the raw string; {@code null} maps to the empty string
     * @return the escaped form, ready to drop between double quotes
     */
    static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
