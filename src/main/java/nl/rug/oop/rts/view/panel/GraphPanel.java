package nl.rug.oop.rts.view.panel;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.controller.mouse.GraphMouseHandler;
import nl.rug.oop.rts.view.renderer.GraphRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * The drawing surface of the editor.
 * <p>
 * The panel renders the {@link Graph} through a {@link GraphRenderer} and
 * funnels mouse input to a {@link GraphMouseHandler}. It is itself a
 * {@code ModelListener} so that any change in the graph triggers a
 * repaint. As required by the assignment the model is unaware of the
 * panel; the panel subscribes to the model rather than the other way
 * round.
 */
public class GraphPanel extends JPanel {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** Preferred initial size of the drawing area. */
    private static final Dimension PREFERRED_SIZE = new Dimension(900, 600);

    /** The editor context (model + history + simulator + flags). */
    private final EditorContext context;

    /** The viewport carrying pan + zoom. */
    private final GraphViewport viewport;

    /** The renderer that does the actual drawing. */
    private final GraphRenderer renderer;

    /**
     * Constructs the panel and wires up listeners.
     *
     * @param context the editor context; must not be {@code null}
     */
    public GraphPanel(EditorContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context;
        this.viewport = new GraphViewport();
        this.renderer = new GraphRenderer();
        setPreferredSize(PREFERRED_SIZE);
        setFocusable(true);

        // Repaint on any model or context change.
        this.context.getGraph().addListener(e -> repaint());
        this.context.addListener(e -> repaint());

        // Wire up mouse input.
        GraphMouseHandler mouseHandler = new GraphMouseHandler(context, viewport, this);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderer.render(g2, viewport.worldToScreen(),
                    context.getGraph(), getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    /**
     * Returns the viewport in case the controller needs to reset it.
     *
     * @return the viewport
     */
    public GraphViewport getViewport() {
        return viewport;
    }
}
