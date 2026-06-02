package nl.rug.oop.rts.controller.mouse;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.view.panel.GraphViewport;
import nl.rug.oop.rts.view.renderer.GraphRenderer;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

/**
 * Translates mouse input on the {@link nl.rug.oop.rts.view.panel.GraphPanel}
 * into model mutations and view adjustments.
 * <p>
 * Behaviour summary:
 * <ul>
 *   <li>left-click on a node to select it; if "Add Edge" mode is on,
 *       also create the edge to that node;</li>
 *   <li>left-click on an edge to select that edge;</li>
 *   <li>left-click on empty space to deselect;</li>
 *   <li>drag a selected node to move it (a single move command is recorded
 *       on release, not on every mouse event);</li>
 *   <li>drag empty space to pan the viewport;</li>
 *   <li>mouse wheel to zoom around the cursor (bonus feature).</li>
 * </ul>
 * The class extends {@link MouseInputAdapter} so it can serve as both the
 * mouse and motion listener, simplifying the wiring on the panel side.
 */
public class GraphMouseHandler extends MouseInputAdapter {

    /** The editor context. */
    private final EditorContext context;

    /** The viewport carrying pan and zoom. */
    private final GraphViewport viewport;

    /** The panel that receives repaint requests. */
    private final JComponent panel;

    /** The node currently being dragged, or {@code null}. */
    private Node draggedNode;

    /** Origin of an active drag in world coordinates. */
    private int dragOriginX;

    /** The y origin of an active node drag. */
    private int dragOriginY;

    /** Most recent screen point of an active drag, for delta computation. */
    private int lastScreenX;

    /** The most recent screen y during a drag. */
    private int lastScreenY;

    /** Whether a drag is currently panning the view (not dragging a node). */
    private boolean panning;

    /**
     * Constructs the mouse handler.
     *
     * @param context the editor context
     * @param viewport the viewport
     * @param panel the panel; used to request repaints
     */
    public GraphMouseHandler(EditorContext context, GraphViewport viewport, JComponent panel) {
        if (context == null || viewport == null || panel == null) {
            throw new IllegalArgumentException("Constructor arguments must be non-null");
        }
        this.context = context;
        this.viewport = viewport;
        this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        lastScreenX = e.getX();
        lastScreenY = e.getY();
        Point2D world = viewport.screenToWorld(e.getPoint());
        Graph graph = context.getGraph();
        Node hitNode = pickNode(graph, world);
        if (hitNode != null) {
            if (context.isAddEdgeMode()) {
                Node source = graph.getSelectedNode();
                if (source != null && source != hitNode) {
                    context.addEdge(source, hitNode);
                }
                context.setAddEdgeMode(false);
                graph.setSelectedNode(hitNode);
                return;
            }
            graph.setSelectedNode(hitNode);
            draggedNode = hitNode;
            dragOriginX = hitNode.getX();
            dragOriginY = hitNode.getY();
            return;
        }
        Edge hitEdge = pickEdge(graph, world);
        if (hitEdge != null) {
            graph.setSelectedEdge(hitEdge);
            return;
        }
        // Empty space.
        graph.clearSelection();
        context.setAddEdgeMode(false);
        panning = true;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastScreenX;
        int dy = e.getY() - lastScreenY;
        lastScreenX = e.getX();
        lastScreenY = e.getY();
        if (draggedNode != null) {
            // Convert the screen delta into a world delta.
            double zoom = viewport.getZoom();
            int newX = draggedNode.getX() + (int) Math.round(dx / zoom);
            int newY = draggedNode.getY() + (int) Math.round(dy / zoom);
            draggedNode.setPosition(newX, newY);
            context.getGraph().fireStructureChanged();
        } else if (panning) {
            viewport.pan(dx, dy);
            panel.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggedNode != null) {
            int finalX = draggedNode.getX();
            int finalY = draggedNode.getY();
            if (finalX != dragOriginX || finalY != dragOriginY) {
                // Undo the live drag and reapply through the command facade so
                // the move can be undone/redone like any other action.
                draggedNode.setPosition(dragOriginX, dragOriginY);
                context.moveNode(draggedNode, dragOriginX, dragOriginY, finalX, finalY);
            }
            draggedNode = null;
        }
        panning = false;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = e.getPreciseWheelRotation() < 0 ? 1.1 : 1.0 / 1.1;
        viewport.zoomAround(factor, e.getPoint());
        panel.repaint();
    }

    /* ===================== Hit testing ===================== */

    /**
     * Finds the top-most node containing a world point, if any.
     *
     * @param graph the graph to search
     * @param world the world-space point
     * @return the picked node, or {@code null}
     */
    private Node pickNode(Graph graph, Point2D world) {
        // Iterate in reverse so visually top-most nodes are picked first.
        for (int i = graph.getNodes().size() - 1; i >= 0; i--) {
            Node n = graph.getNodes().get(i);
            double dx = world.getX() - n.getX();
            double dy = world.getY() - n.getY();
            if (dx * dx + dy * dy <= (double) GraphRenderer.NODE_RADIUS * GraphRenderer.NODE_RADIUS) {
                return n;
            }
        }
        return null;
    }

    /**
     * Finds the edge near a world point, if any.
     *
     * @param graph the graph to search
     * @param world the world-space point
     * @return the picked edge, or {@code null}
     */
    private Edge pickEdge(Graph graph, Point2D world) {
        for (Edge e : graph.getEdges()) {
            if (distanceFromPointToSegment(world.getX(), world.getY(),
                    e.getNodeA().getX(), e.getNodeA().getY(),
                    e.getNodeB().getX(), e.getNodeB().getY()) <= 6.0) {
                return e;
            }
        }
        return null;
    }

    /**
     * Computes the shortest distance from a point to a line segment.
     *
     * @param px the point x coordinate
     * @param py the point y coordinate
     * @param ax the segment start x coordinate
     * @param ay the segment start y coordinate
     * @param bx the segment end x coordinate
     * @param by the segment end y coordinate
     * @return the shortest distance from the point to the segment
     */
    private double distanceFromPointToSegment(double px, double py,
                                              double ax, double ay,
                                              double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) {
            double ddx = px - ax;
            double ddy = py - ay;
            return Math.sqrt(ddx * ddx + ddy * ddy);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));
        double closestX = ax + t * dx;
        double closestY = ay + t * dy;
        double ddx = px - closestX;
        double ddy = py - closestY;
        return Math.sqrt(ddx * ddx + ddy * ddy);
    }
}
