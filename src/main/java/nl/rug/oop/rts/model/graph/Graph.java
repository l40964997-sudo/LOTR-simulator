package nl.rug.oop.rts.model.graph;

import nl.rug.oop.rts.model.observer.ModelEvent;
import nl.rug.oop.rts.model.observer.Observable;
import nl.rug.oop.rts.util.IdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The complete map: every {@link Node} and {@link Edge}, plus the current
 * selection.
 * <p>
 * The class is the central {@link Observable} for the application. Views
 * subscribe to it and update themselves whenever an {@link ModelEvent}
 * is fired. The model has no awareness of the view: that strict separation
 * is exactly what the MVC requirement of the assignment is about.
 * <p>
 * The class is also the only point in the program that decides what
 * "selected" means; nodes and edges themselves do not carry a {@code selected}
 * flag. That makes the selection state easy to clear (no need to walk the
 * graph) and ensures that at most one element is selected at any time,
 * which simplifies the UI logic considerably.
 */
public class Graph extends Observable {

    /** Every node currently in the graph. */
    private final List<Node> nodes = new ArrayList<>();

    /** Every edge currently in the graph. */
    private final List<Edge> edges = new ArrayList<>();

    /** The currently selected node, or {@code null} if no node is selected. */
    private Node selectedNode;

    /** The currently selected edge, or {@code null} if no edge is selected. */
    private Edge selectedEdge;

    /** Constructs an empty graph. */
    public Graph() {
        // no-op
    }

    /* ===================== Structure mutation ===================== */

    /**
     * Adds a node to the graph at the given coordinates with an auto generated
     * name.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the freshly created node
     */
    public Node addNode(int x, int y) {
        int id = IdGenerator.nextNodeId();
        Node node = new Node(id, "Node" + id, x, y);
        nodes.add(node);
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
        return node;
    }

    /**
     * Re-inserts an existing node, used by the Undo/Redo system. Does
     * <em>not</em> allocate a fresh id, because the action history depends
     * on stable identity.
     *
     * @param node the node to reinsert; ignored when {@code null} or already present
     */
    public void reinsertNode(Node node) {
        if (node == null || nodes.contains(node)) {
            return;
        }
        nodes.add(node);
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
    }

    /**
     * Removes a node and every edge incident to it.
     *
     * @param node the node to remove; ignored when {@code null} or not in the graph
     * @return list of edges that were also removed (so they can be undone)
     */
    public List<Edge> removeNode(Node node) {
        if (node == null || !nodes.contains(node)) {
            return Collections.emptyList();
        }
        // Snapshot first; modifying the edge list while we iterate would explode.
        List<Edge> incident = new ArrayList<>(node.getEdges());
        for (Edge e : incident) {
            removeEdgeInternal(e);
        }
        nodes.remove(node);
        if (selectedNode == node) {
            selectedNode = null;
        }
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
        return incident;
    }

    /**
     * Adds an edge between two distinct nodes that are both in the graph.
     * Silently ignores requests for self-loops or duplicate edges.
     *
     * @param a one endpoint
     * @param b the other endpoint
     * @return the new edge, or {@code null} if the request was invalid
     */
    public Edge addEdge(Node a, Node b) {
        if (a == null || b == null || a == b) {
            return null;
        }
        if (!nodes.contains(a) || !nodes.contains(b)) {
            return null;
        }
        // Simple graph: no parallel edges.
        for (Edge e : edges) {
            if ((e.getNodeA() == a && e.getNodeB() == b)
                    || (e.getNodeA() == b && e.getNodeB() == a)) {
                return null;
            }
        }
        int id = IdGenerator.nextEdgeId();
        Edge edge = new Edge(id, "Edge" + id, a, b);
        edges.add(edge);
        a.attachEdge(edge);
        b.attachEdge(edge);
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
        return edge;
    }

    /**
     * Re-inserts a previously removed edge. Used by Undo/Redo.
     *
     * @param edge the edge; must reference two nodes currently in the graph
     */
    public void reinsertEdge(Edge edge) {
        if (edge == null || edges.contains(edge)) {
            return;
        }
        if (!nodes.contains(edge.getNodeA()) || !nodes.contains(edge.getNodeB())) {
            return;
        }
        edges.add(edge);
        edge.getNodeA().attachEdge(edge);
        edge.getNodeB().attachEdge(edge);
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
    }

    /**
     * Removes an edge from the graph.
     *
     * @param edge the edge; ignored when {@code null} or not present
     */
    public void removeEdge(Edge edge) {
        if (edge == null || !edges.contains(edge)) {
            return;
        }
        removeEdgeInternal(edge);
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
    }

    /**
     * Internal edge removal that does not fire a notification.
     */
    private void removeEdgeInternal(Edge edge) {
        edge.getNodeA().detachEdge(edge);
        edge.getNodeB().detachEdge(edge);
        edges.remove(edge);
        if (selectedEdge == edge) {
            selectedEdge = null;
        }
    }

    /* ===================== Accessors ===================== */

    /**
     * Returns an unmodifiable view of the node list.
     *
     * @return the nodes
     */
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Returns an unmodifiable view of the edge list.
     *
     * @return the edges
     */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /* ===================== Selection ===================== */

    /**
     * Returns the currently selected node, or {@code null}.
     *
     * @return the selected node
     */
    public Node getSelectedNode() {
        return selectedNode;
    }

    /**
     * Selects a node, deselecting any current edge selection.
     *
     * @param node the node to select; pass {@code null} to clear
     */
    public void setSelectedNode(Node node) {
        if (this.selectedNode == node && this.selectedEdge == null) {
            return;
        }
        this.selectedNode = node;
        this.selectedEdge = null;
        notifyListeners(ModelEvent.Type.SELECTION);
    }

    /**
     * Returns the currently selected edge, or {@code null}.
     *
     * @return the selected edge
     */
    public Edge getSelectedEdge() {
        return selectedEdge;
    }

    /**
     * Selects an edge, deselecting any current node selection.
     *
     * @param edge the edge to select; pass {@code null} to clear
     */
    public void setSelectedEdge(Edge edge) {
        if (this.selectedEdge == edge && this.selectedNode == null) {
            return;
        }
        this.selectedEdge = edge;
        this.selectedNode = null;
        notifyListeners(ModelEvent.Type.SELECTION);
    }

    /**
     * Clears every selection.
     */
    public void clearSelection() {
        if (selectedNode == null && selectedEdge == null) {
            return;
        }
        selectedNode = null;
        selectedEdge = null;
        notifyListeners(ModelEvent.Type.SELECTION);
    }

    /* ===================== Notification escalations ===================== */

    /**
     * Fires a generic ARMY notification. Called by controllers and
     * commands after they mutate army state, so the view repaints.
     */
    public void fireArmyChanged() {
        notifyListeners(ModelEvent.Type.ARMY);
    }

    /**
     * Fires an EVENT notification.
     */
    public void fireEventChanged() {
        notifyListeners(ModelEvent.Type.EVENT);
    }

    /**
     * Fires a SIMULATION notification.
     */
    public void fireSimulationStepped() {
        notifyListeners(ModelEvent.Type.SIMULATION);
    }

    /**
     * Fires a GRAPH_STRUCTURE notification (e.g. when nodes are moved).
     */
    public void fireStructureChanged() {
        notifyListeners(ModelEvent.Type.GRAPH_STRUCTURE);
    }
}
