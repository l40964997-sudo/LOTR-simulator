package nl.rug.oop.rts.view.panel;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Carries the pan offset and zoom level of the editor viewport.
 * <p>
 * Putting the affine state in its own object keeps the panel and the
 * mouse controller in sync without either of them having to know about
 * the other's coordinate frame. World coordinates are stored on the
 * model; screen coordinates are what the user clicks; this class is the
 * bridge between the two.
 * <p>
 * The class is intentionally plain old data - no observers, no
 * notifications. The owning panel repaints itself after mutating the
 * viewport because the rate of mouse-driven changes would otherwise spam
 * model events that the model layer does not need to hear about.
 */
public class GraphViewport {

    /** Smallest allowed zoom factor (5%). */
    public static final double MIN_ZOOM = 0.2;

    /** Largest allowed zoom factor (500%). */
    public static final double MAX_ZOOM = 5.0;

    /** Horizontal pan offset, in screen pixels. */
    private double offsetX;

    /** Vertical pan offset, in screen pixels. */
    private double offsetY;

    /** Uniform zoom factor; 1.0 means 1:1 with world coordinates. */
    private double zoom = 1.0;

    /**
     * Returns an affine transform that maps from world coordinates to
     * screen coordinates.
     *
     * @return the current world to screen transform
     */
    public AffineTransform worldToScreen() {
        AffineTransform tx = new AffineTransform();
        tx.translate(offsetX, offsetY);
        tx.scale(zoom, zoom);
        return tx;
    }

    /**
     * Converts a screen point to world coordinates.
     *
     * @param p the screen point; must not be {@code null}
     * @return the corresponding world point
     */
    public Point2D screenToWorld(Point p) {
        if (p == null) {
            return new Point2D.Double();
        }
        return new Point2D.Double((p.x - offsetX) / zoom, (p.y - offsetY) / zoom);
    }

    /**
     * Pans by a screen-space delta.
     *
     * @param dx horizontal delta in pixels
     * @param dy vertical delta in pixels
     */
    public void pan(double dx, double dy) {
        offsetX += dx;
        offsetY += dy;
    }

    /**
     * Zooms in or out around an anchor point so that the world point under
     * the anchor stays put on screen.
     *
     * @param factor multiplier applied to the current zoom
     * @param anchor screen point to keep stationary
     */
    public void zoomAround(double factor, Point anchor) {
        if (anchor == null) {
            return;
        }
        double oldZoom = zoom;
        double newZoom = clamp(oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
        if (newZoom == oldZoom) {
            return;
        }
        // Keep the world point under `anchor` invariant.
        offsetX = anchor.x - (anchor.x - offsetX) * (newZoom / oldZoom);
        offsetY = anchor.y - (anchor.y - offsetY) * (newZoom / oldZoom);
        zoom = newZoom;
    }

    /**
     * Returns the zoom factor.
     *
     * @return the current uniform zoom
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * Clamps a value to an inclusive range.
     *
     * @param v the value
     * @param lo the lower bound
     * @param hi the upper bound
     * @return the clamped value
     */
    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
