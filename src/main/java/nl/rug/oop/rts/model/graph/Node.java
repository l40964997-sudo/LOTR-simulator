package nl.rug.oop.rts.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A location in the world map.
 * <p>
 * In addition to the data inherited from {@link MapElement} (id, name,
 * armies, events), a node knows the {@link Edge}s incident to it and its
 * spatial coordinates in graph space. The coordinates are intentionally
 * stored on the model rather than computed by the view, because:
 * <ul>
 *   <li>they survive across panning and zooming;</li>
 *   <li>they round-trip through Undo/Redo (moving a node is a recorded
 *       action);</li>
 *   <li>they would be needed if the JSON exporter were extended with
 *       layout persistence.</li>
 * </ul>
 */
public class Node extends MapElement {

    /** Edges currently incident to this node. */
    private final List<Edge> edges = new ArrayList<>();

    /** X coordinate in graph space. */
    private int x;

    /** Y coordinate in graph space. */
    private int y;

    /**
     * Constructs a node with explicit coordinates.
     *
     * @param id   unique node id
     * @param name initial display name
     * @param x    horizontal position
     * @param y    vertical position
     */
    public Node(int id, String name, int x, int y) {
        super(id, name);
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return getName() + "(" + getId() + ")@(" + x + "," + y + ")";
    }

    /**
     * Returns the x coordinate.
     *
     * @return horizontal position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y coordinate.
     *
     * @return vertical position
     */
    public int getY() {
        return y;
    }

    /**
     * Updates the node position.
     *
     * @param x new x
     * @param y new y
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns an unmodifiable view of the incident edges.
     *
     * @return the edges adjacent to this node
     */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /**
     * Registers an incident edge. Package private because only the
     * {@link Graph} should call it.
     *
     * @param edge the edge to register; ignored when {@code null} or already present
     */
    void attachEdge(Edge edge) {
        if (edge == null || edges.contains(edge)) {
            return;
        }
        edges.add(edge);
    }

    /**
     * Unregisters an incident edge. Package private; see {@link #attachEdge(Edge)}.
     *
     * @param edge the edge
     */
    void detachEdge(Edge edge) {
        edges.remove(edge);
    }
}
