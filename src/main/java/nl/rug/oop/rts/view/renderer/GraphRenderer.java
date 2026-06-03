package nl.rug.oop.rts.view.renderer;

import nl.rug.oop.rts.model.army.Army;
import nl.rug.oop.rts.model.army.Faction;
import nl.rug.oop.rts.model.graph.Edge;
import nl.rug.oop.rts.model.graph.Graph;
import nl.rug.oop.rts.model.graph.Node;
import nl.rug.oop.rts.util.TextureLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless drawing helper that paints a {@link Graph} on a Swing
 * {@link Graphics2D} surface.
 *
 * <p>Decoupling the drawing logic from the panel that owns the graphics
 * keeps the panel small and focuses this class on a single concern. The
 * renderer reads but never mutates the graph, so it can be called from any
 * paint pipeline.</p>
 *
 * <p>The renderer uses the course-supplied texture pack through
 * {@link TextureLoader}: a tiled map background, a neutral sprite per node,
 * the settlement's <em>home</em> faction fortress sprite (driven by
 * {@link Node#getFaction()} - never by whichever army happens to occupy
 * the spot), and the clash effect over locations where two opposing teams
 * meet. A small army-count badge is drawn over any node or edge that
 * hosts at least one army, so the map can be read at a glance without
 * opening the side panel.</p>
 *
 * <p>The renderer paints in screen coordinates after the caller has applied
 * the viewport transform, so the panel stays the single source of truth for
 * pan and zoom.</p>
 */
public class GraphRenderer {

    /** Visual radius (world units) of a node disk. */
    public static final int NODE_RADIUS = 22;

    /**
     * Diameter of the fortress sprite layered over a node, as a fraction of
     * the node diameter. Keeping it a touch larger than the ground tile lets
     * the fortress silhouette read clearly without hiding the tile entirely.
     */
    private static final double FORTRESS_OVERLAY_SCALE = 1.15;

    /**
     * Hit-test radius used by the mouse handler: matches the visible
     * fortress overlay so the whole icon is grabbable, even when armies
     * are stacked above the node.
     */
    public static final int NODE_HIT_RADIUS =
            (int) Math.round(NODE_RADIUS * FORTRESS_OVERLAY_SCALE);

    /** Stroke width for the un-selected edges. */
    private static final float EDGE_STROKE_WIDTH = 2.0f;

    /** Stroke width for selected edges. */
    private static final float EDGE_STROKE_WIDTH_SELECTED = 4.0f;

    /** Outline colour drawn around a selected node. */
    private static final Color NODE_HIGHLIGHT = new Color(0xEE6C4D);

    /** Outline / label colour. */
    private static final Color NODE_OUTLINE = new Color(0xE0FBFC);

    /** Edge colour (un-selected). */
    private static final Color EDGE_COLOR = new Color(0xCBB279);

    /** Edge colour when selected. */
    private static final Color EDGE_COLOR_SELECTED = new Color(0xF7B538);

    /** Army-count badge fill colour. */
    private static final Color ARMY_BADGE_FILL = new Color(0x2A3D5C);

    /** Diameter in pixels of the army-count badge. */
    private static final int ARMY_BADGE_SIZE = 16;

    /** Size in pixels of the tiled fallback background cells. */
    private static final int MAP_TILE = 256;

    /** Maximum dimension we ever request for the Middle Earth backdrop. */
    private static final int MAP_BACKDROP_MAX = 2048;

    /** Translucent overlay shade for legibility on top of the map. */
    private static final Color BACKDROP_VEIL = new Color(0, 0, 0, 110);

    /** Label font. */
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /** Font used for the edge terrain annotation. */
    private static final Font TERRAIN_FONT = new Font(Font.SERIF, Font.ITALIC, 13);

    /** Shared texture loader. */
    private final TextureLoader textures = TextureLoader.getInstance();

    /** Cache of fully-decoded sprites, keyed by name and size. */
    private final Map<String, Image> spriteCache = new HashMap<>();

    /**
     * Returns a fully-loaded sprite for the given texture key and size.
     *
     * <p>The course texture loader hands back images produced by
     * {@code getScaledInstance}, which decode asynchronously; drawing one
     * immediately can paint nothing. Wrapping it in an {@link ImageIcon}
     * forces synchronous loading via a media tracker, after which the image
     * is safe to draw. Results are cached so this cost is paid once per
     * key and size.</p>
     *
     * @param key the texture key understood by the loader
     * @param size the square size in pixels
     * @return a decoded image ready to draw
     */
    private Image sprite(String key, int size) {
        String cacheKey = key + "@" + size;
        Image cached = spriteCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image raw = textures.getTexture(key, size, size);
        Image ready = new ImageIcon(raw).getImage();
        spriteCache.put(cacheKey, ready);
        return ready;
    }

    /**
     * Paints the entire graph.
     *
     * @param g2 the graphics surface; must not be {@code null}
     * @param viewport the world-to-screen transform
     * @param graph the graph to render
     * @param panelWidth panel width in pixels
     * @param panelHeight panel height in pixels
     */
    public void render(Graphics2D g2, AffineTransform viewport, Graph graph,
                       int panelWidth, int panelHeight) {
        if (g2 == null || graph == null) {
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintBackground(g2, panelWidth, panelHeight);
        AffineTransform original = g2.getTransform();
        try {
            g2.transform(viewport);
            drawEdges(g2, graph);
            drawNodes(g2, graph);
        } finally {
            g2.setTransform(original);
        }
    }

    /**
     * Paints the Middle Earth backdrop. The {@code lotrMap.jpg} resource is
     * scaled to cover the whole panel; the parchment tile is drawn first as
     * a backup so any margin caused by aspect-ratio differences still looks
     * like a map rather than blank space. A translucent dark veil sits on
     * top to keep the foreground elements readable.
     *
     * @param g2 the graphics surface
     * @param width the panel width
     * @param height the panel height
     */
    private void paintBackground(Graphics2D g2, int width, int height) {
        paintTiledFallback(g2, width, height);
        int side = Math.min(MAP_BACKDROP_MAX, Math.max(width, height));
        Image backdrop = sprite("mapLotr", side);
        int imgW = backdrop.getWidth(null);
        int imgH = backdrop.getHeight(null);
        if (imgW <= 0 || imgH <= 0) {
            imgW = side;
            imgH = side;
        }
        double scale = Math.max((double) width / imgW, (double) height / imgH);
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int dx = (width - drawW) / 2;
        int dy = (height - drawH) / 2;
        g2.drawImage(backdrop, dx, dy, drawW, drawH, null);
        g2.setColor(BACKDROP_VEIL);
        g2.fillRect(0, 0, width, height);
    }

    /**
     * Tiles the parchment texture across the panel as a fallback under the
     * Middle Earth map, so any unfilled margin keeps the map feel.
     *
     * @param g2 the graphics surface
     * @param width the panel width
     * @param height the panel height
     */
    private void paintTiledFallback(Graphics2D g2, int width, int height) {
        Image tile = sprite("mapTexture", MAP_TILE);
        for (int x = 0; x < width; x += MAP_TILE) {
            for (int y = 0; y < height; y += MAP_TILE) {
                g2.drawImage(tile, x, y, null);
            }
        }
    }

    /**
     * Draws all edges, highlighting the selected one.
     *
     * @param g2 the graphics surface
     * @param graph the graph being drawn
     */
    private void drawEdges(Graphics2D g2, Graph graph) {
        Edge selected = graph.getSelectedEdge();
        for (Edge edge : graph.getEdges()) {
            drawSingleEdge(g2, edge, edge == selected);
        }
    }

    /**
     * Draws one edge plus the clash effect at its midpoint when two
     * opposing teams are travelling along it.
     *
     * @param g2 the graphics surface
     * @param edge the edge to draw
     * @param isSelected whether this edge is selected
     */
    private void drawSingleEdge(Graphics2D g2, Edge edge, boolean isSelected) {
        g2.setStroke(new BasicStroke(
                isSelected ? EDGE_STROKE_WIDTH_SELECTED : EDGE_STROKE_WIDTH,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[] {8f, 4f}, 0f));
        g2.setColor(isSelected ? EDGE_COLOR_SELECTED : EDGE_COLOR);
        Node a = edge.getNodeA();
        Node b = edge.getNodeB();
        g2.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
        int mx = (a.getX() + b.getX()) / 2;
        int my = (a.getY() + b.getY()) / 2;
        if (hasClash(edge.getArmies())) {
            drawClash(g2, mx, my);
        }
        drawArmyBadge(g2, edge.getArmies(),
                mx - ARMY_BADGE_SIZE / 2,
                my - NODE_RADIUS / 2 - ARMY_BADGE_SIZE);
        drawEdgeTerrainLabel(g2, edge, mx, my);
    }

    /**
     * Draws the terrain name underneath the edge midpoint so the user can
     * read the route's terrain at a glance.
     *
     * @param g2 the graphics surface
     * @param edge the edge being drawn
     * @param mx the edge midpoint x
     * @param my the edge midpoint y
     */
    private void drawEdgeTerrainLabel(Graphics2D g2, Edge edge, int mx, int my) {
        if (edge.getEdgeType() == null) {
            return;
        }
        String label = edge.getEdgeType().getDisplayName();
        g2.setFont(TERRAIN_FONT);
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int boxX = mx - textWidth / 2 - 4;
        int boxY = my + NODE_RADIUS / 2;
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(boxX, boxY, textWidth + 8, 16, 8, 8);
        g2.setColor(NODE_OUTLINE);
        g2.drawString(label, mx - textWidth / 2, boxY + 12);
    }

    /**
     * Draws every node, highlighting the selected one.
     *
     * @param g2 the graphics surface
     * @param graph the graph being drawn
     */
    private void drawNodes(Graphics2D g2, Graph graph) {
        Node selected = graph.getSelectedNode();
        for (Node node : graph.getNodes()) {
            drawSingleNode(g2, node, node == selected);
        }
    }

    /**
     * Draws one node: selection halo, base sprite, label, clash overlay
     * (when contested) and the small event-count badge.
     *
     * @param g2 the graphics surface
     * @param node the node to draw
     * @param isSelected whether the node is currently selected
     */
    private void drawSingleNode(Graphics2D g2, Node node, boolean isSelected) {
        int x = node.getX();
        int y = node.getY();
        int diameter = NODE_RADIUS * 2;
        if (isSelected) {
            g2.setColor(NODE_HIGHLIGHT);
            g2.fillOval(x - NODE_RADIUS - 5, y - NODE_RADIUS - 5, diameter + 10, diameter + 10);
        }
        drawNodeBase(g2, node, x, y, diameter);
        drawNodeLabel(g2, node.getName(), x, y);
        if (hasClash(node.getArmies())) {
            drawClash(g2, x, y);
        }
        drawEventBadge(g2, node, x, y);
        drawArmyBadge(g2, node.getArmies(),
                x - NODE_RADIUS - ARMY_BADGE_SIZE / 2 + 2,
                y - NODE_RADIUS - 2);
    }

    /**
     * Draws the node base: a neutral node tile underneath, then the
     * settlement's <em>home</em> faction fortress on top. The visible
     * fortress reflects which realm owns the location, never which army
     * happens to be standing on it, so capturing a location does not
     * silently re-skin it.
     *
     * @param g2 the graphics surface
     * @param node the node being drawn
     * @param x the node centre x
     * @param y the node centre y
     * @param diameter the sprite diameter
     */
    private void drawNodeBase(Graphics2D g2, Node node, int x, int y, int diameter) {
        // Neutral node tile underneath - cycles node1..node4 so all four
        // ground sprites remain visible across the map.
        String groundKey = "node" + (Math.floorMod(node.getId(), 4) + 1);
        Image ground = sprite(groundKey, diameter);
        g2.drawImage(ground, x - NODE_RADIUS, y - NODE_RADIUS, null);

        // Home faction fortress on top, slightly larger than the ground
        // tile so its silhouette reads clearly.
        Faction home = node.getFaction();
        if (home != null) {
            int overlay = (int) Math.round(diameter * FORTRESS_OVERLAY_SCALE);
            Image fortress = sprite("fortress" + home.textureKey(), overlay);
            g2.drawImage(fortress, x - overlay / 2, y - overlay / 2, null);
        }
    }

    /**
     * Draws the node name with a translucent backdrop for legibility.
     *
     * @param g2 the graphics surface
     * @param label the node name
     * @param x the node centre x
     * @param y the node centre y
     */
    private void drawNodeLabel(Graphics2D g2, String label, int x, int y) {
        g2.setFont(LABEL_FONT);
        int textWidth = g2.getFontMetrics().stringWidth(label);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(x - textWidth / 2 - 4, y + NODE_RADIUS + 2, textWidth + 8, 16, 8, 8);
        g2.setColor(NODE_OUTLINE);
        g2.drawString(label, x - textWidth / 2, y + NODE_RADIUS + 14);
    }

    /**
     * Draws the small event-count badge if the node hosts any events.
     *
     * @param g2 the graphics surface
     * @param node the node being drawn
     * @param x the node centre x
     * @param y the node centre y
     */
    private void drawEventBadge(Graphics2D g2, Node node, int x, int y) {
        if (node.getEvents().isEmpty()) {
            return;
        }
        g2.setColor(new Color(0xFFD23F));
        g2.fillOval(x + NODE_RADIUS - 6, y - NODE_RADIUS - 2, 14, 14);
        g2.setColor(Color.BLACK);
        g2.setFont(LABEL_FONT.deriveFont(Font.BOLD, 10f));
        g2.drawString(String.valueOf(node.getEvents().size()), x + NODE_RADIUS - 2, y - NODE_RADIUS + 9);
    }

    /**
     * Draws a small army-count badge on top of a node or edge whenever at
     * least one army is present. The badge sits well inside the node
     * radius so it never covers the fortress silhouette underneath.
     *
     * @param g2 the graphics surface
     * @param armies the armies present at the location
     * @param bx top-left x of the badge
     * @param by top-left y of the badge
     */
    private void drawArmyBadge(Graphics2D g2, List<Army> armies, int bx, int by) {
        if (armies == null || armies.isEmpty()) {
            return;
        }
        g2.setColor(ARMY_BADGE_FILL);
        g2.fillOval(bx, by, ARMY_BADGE_SIZE, ARMY_BADGE_SIZE);
        g2.setColor(NODE_OUTLINE);
        g2.drawOval(bx, by, ARMY_BADGE_SIZE, ARMY_BADGE_SIZE);
        String text = String.valueOf(armies.size());
        g2.setFont(LABEL_FONT.deriveFont(Font.BOLD, 10f));
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text,
                bx + (ARMY_BADGE_SIZE - textWidth) / 2,
                by + ARMY_BADGE_SIZE - 4);
    }

    /**
     * Draws the clash effect centred at the given world point.
     *
     * @param g2 the graphics surface
     * @param x the centre x
     * @param y the centre y
     */
    private void drawClash(Graphics2D g2, int x, int y) {
        int size = NODE_RADIUS * 2;
        Image clash = sprite("flash", size);
        g2.drawImage(clash, x - size / 2, y - size / 2, null);
    }

    /**
     * Reports whether two opposing teams are both present.
     *
     * @param armies the armies present at the location
     * @return {@code true} if both teams are represented
     */
    private boolean hasClash(List<Army> armies) {
        if (armies == null || armies.size() < 2) {
            return false;
        }
        boolean free = false;
        boolean shadow = false;
        for (Army army : armies) {
            if (army.getTeam() == Faction.TEAM_FREE_PEOPLES) {
                free = true;
            } else {
                shadow = true;
            }
        }
        return free && shadow;
    }
}
