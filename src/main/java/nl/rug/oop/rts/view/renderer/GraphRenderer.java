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
 * the controlling faction's fortress sprite when a node is held, the small
 * faction emblems for the army chips, and the clash effect over locations
 * where two opposing teams meet.</p>
 *
 * <p>The renderer paints in screen coordinates after the caller has applied
 * the viewport transform, so the panel stays the single source of truth for
 * pan and zoom.</p>
 */
public class GraphRenderer {

    /** Visual radius (world units) of a node disk. */
    public static final int NODE_RADIUS = 32;

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

    /** Size in pixels of the tiled background cells. */
    private static final int MAP_TILE = 256;

    /** Label font. */
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

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
     * Fills the panel by tiling the map texture across it.
     *
     * @param g2 the graphics surface
     * @param width the panel width
     * @param height the panel height
     */
    private void paintBackground(Graphics2D g2, int width, int height) {
        Image tile = sprite("mapTexture", MAP_TILE);
        for (int x = 0; x < width; x += MAP_TILE) {
            for (int y = 0; y < height; y += MAP_TILE) {
                g2.drawImage(tile, x, y, null);
            }
        }
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, width, height);
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
     * Draws one edge plus any armies or clash effect on it.
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
        drawArmyBadges(g2, edge.getArmies(), mx, my, false);
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
     * Draws one node: selection halo, base sprite, label, army chips, clash
     * overlay and event badge.
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
        drawArmyBadges(g2, node.getArmies(), x, y - NODE_RADIUS - 16, true);
        if (hasClash(node.getArmies())) {
            drawClash(g2, x, y);
        }
        drawEventBadge(g2, node, x, y);
    }

    /**
     * Draws the node base sprite: a fortress when held, else a node sprite.
     *
     * @param g2 the graphics surface
     * @param node the node being drawn
     * @param x the node centre x
     * @param y the node centre y
     * @param diameter the sprite diameter
     */
    private void drawNodeBase(Graphics2D g2, Node node, int x, int y, int diameter) {
        Faction holder = controllingFaction(node.getArmies());
        String key = holder != null
                ? "fortress" + holder.textureKey()
                : "node" + (node.getId() % 4 + 1);
        Image base = sprite(key, diameter);
        g2.drawImage(base, x - NODE_RADIUS, y - NODE_RADIUS, null);
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
     * Draws faction emblem chips for each army present at a location.
     *
     * @param g2 the graphics surface
     * @param armies the armies to depict
     * @param cx the centre x of the location
     * @param cy the reference y of the location
     * @param above whether the chips sit above the reference point
     */
    private void drawArmyBadges(Graphics2D g2, List<Army> armies, int cx, int cy, boolean above) {
        if (armies == null || armies.isEmpty()) {
            return;
        }
        int chip = 18;
        int spacing = 3;
        int totalWidth = armies.size() * (chip + spacing) - spacing;
        int startX = cx - totalWidth / 2;
        int y = above ? cy - chip : cy - chip / 2;
        for (int i = 0; i < armies.size(); i++) {
            drawArmyChip(g2, armies.get(i), startX + i * (chip + spacing), y, chip);
        }
    }

    /**
     * Draws a single army chip: faction emblem plus unit count.
     *
     * @param g2 the graphics surface
     * @param army the army to depict
     * @param x the chip x position
     * @param y the chip y position
     * @param chip the chip size in pixels
     */
    private void drawArmyChip(Graphics2D g2, Army army, int x, int y, int chip) {
        Image emblem = sprite("faction" + army.getFaction().textureKey(), chip);
        g2.drawImage(emblem, x, y, null);
        String count = String.valueOf(army.size());
        g2.setFont(LABEL_FONT.deriveFont(Font.BOLD, 11f));
        int textWidth = g2.getFontMetrics().stringWidth(count);
        int textX = x + (chip - textWidth) / 2;
        int textY = y + chip - 4;
        g2.setColor(Color.BLACK);
        g2.drawString(count, textX + 1, textY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(count, textX, textY);
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
     * Returns the faction that solely controls a location, or {@code null}
     * if it is empty or contested by both teams.
     *
     * @param armies the armies present at the location
     * @return the controlling faction, or {@code null}
     */
    private Faction controllingFaction(List<Army> armies) {
        if (armies == null || armies.isEmpty()) {
            return null;
        }
        Faction first = armies.get(0).getFaction();
        int team = armies.get(0).getTeam();
        for (Army army : armies) {
            if (army.getTeam() != team) {
                return null;
            }
        }
        return first;
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
