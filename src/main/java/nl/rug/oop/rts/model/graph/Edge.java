package nl.rug.oop.rts.model.graph;

import java.util.Objects;

/**
 * An undirected edge connecting two distinct {@link Node}s.
 * <p>
 * An edge owns no extra spatial data - its endpoints carry it -
 * but, just like a node, it can host armies and events while they traverse
 * the route. The edge is immutable in its endpoint references; rerouting
 * requires deleting and recreating the edge through the {@link Graph}.
 */
public class Edge extends MapElement {

    /** The first endpoint. */
    private final Node nodeA;

    /** The second endpoint. */
    private final Node nodeB;

    /** Terrain type of the route; drives the side-panel picker. */
    private EdgeType edgeType = EdgeType.WOOD;

    /**
     * Constructs an edge.
     *
     * @param id    unique edge id
     * @param name  initial display name
     * @param nodeA first endpoint, must not be {@code null}
     * @param nodeB second endpoint, must not be {@code null} and must differ from {@code nodeA}
     */
    public Edge(int id, String name, Node nodeA, Node nodeB) {
        super(id, name);
        if (nodeA == null || nodeB == null) {
            throw new IllegalArgumentException("Both endpoints must be non-null");
        }
        if (nodeA == nodeB) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    /**
     * Returns the first endpoint.
     *
     * @return one of the two endpoints
     */
    public Node getNodeA() {
        return nodeA;
    }

    /**
     * Returns the second endpoint.
     *
     * @return the other endpoint
     */
    public Node getNodeB() {
        return nodeB;
    }

    /**
     * Given one endpoint, returns the other one.
     *
     * @param origin the known endpoint; must be one of the two endpoints
     * @return the opposite endpoint
     * @throws IllegalArgumentException when {@code origin} is not incident to this edge
     */
    public Node opposite(Node origin) {
        if (Objects.equals(origin, nodeA)) {
            return nodeB;
        }
        if (Objects.equals(origin, nodeB)) {
            return nodeA;
        }
        throw new IllegalArgumentException("Node is not incident to this edge");
    }

    /**
     * Reports whether the supplied node is one of the two endpoints.
     *
     * @param node the node to test; may be {@code null}
     * @return {@code true} if {@code node} is an endpoint
     */
    public boolean isIncidentTo(Node node) {
        return node != null && (node == nodeA || node == nodeB);
    }

    /**
     * Returns the terrain type of this route.
     *
     * @return the edge type
     */
    public EdgeType getEdgeType() {
        return edgeType;
    }

    /**
     * Updates the terrain type of this route. {@code null} is ignored so
     * the field always has a valid value.
     *
     * @param edgeType the new terrain type
     */
    public void setEdgeType(EdgeType edgeType) {
        if (edgeType == null) {
            return;
        }
        this.edgeType = edgeType;
    }
}
