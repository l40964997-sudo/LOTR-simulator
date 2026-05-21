package nl.rug.oop.rts.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that can be used to load images from the resources folder.
 * <p>
 * The idea is that you can call one of the preloaded textures and use them in
 * your program.
 * Alternatively, you can use this class to load textures from any given path
 * you provide to it.
 * Should it not be able to load an image, then a blank image is returned.
 * </p>
 * <p>
 * The following is an example usage where we use the texture loader to retrieve
 * the image "images/factions/elves.pmg" with a size of 100x100:
 * </p>
 *
 * <pre>
 * <code>
 *     TextureLoader.getInstance().getTexture("factionElves", 100, 100);
 * </code>
 * </pre>
 * <p>
 * A number of textures are loaded in by default. These can be accessed using
 * the following names:
 * </p>
 * <ul>
 * <li><strong>factionMen</strong>: images/factions/men.png</li>
 * <li><strong>factionElves</strong>: images/factions/elves.png</li>
 * <li><strong>factionDwarves</strong>: images/factions/dwarves.png</li>
 * <li><strong>factionMordor</strong>: images/factions/mordor.png</li>
 * <li><strong>factionIsengard</strong>: images/factions/isengard.png</li>
 *
 * <li><strong>fortressMen</strong>: images/fortress/men.png</li>
 * <li><strong>fortressElves</strong>: images/fortress/elves.png</li>
 * <li><strong>fortressDwarves</strong>: images/fortress/dwarves.png</li>
 * <li><strong>fortressMordor</strong>: images/fortress/mordor.png</li>
 * <li><strong>fortressIsengard</strong>: images/fortress/isengard.png</li>
 *
 * <li><strong>node1</strong>: images/nodes/node1.png</li>
 * <li><strong>node2</strong>: images/nodes/node2.png</li>
 * <li><strong>node3</strong>: images/nodes/node3.png</li>
 * <li><strong>node4</strong>: images/nodes/node4.png</li>
 *
 * <li><strong>mapTexture</strong>: images/maps/mapTexture.jpg</li>
 * <li><strong>mapLotr</strong>: images/maps/lotrMap.jpg</li>
 *
 * <li><strong>flash</strong>: images/effects/clash.png</li>
 * </ul>
 * <p>
 * You are free to edit this class if you need to.
 *
 * @author Niels Bugel
 */
public class TextureLoader {

    /** Main instance for the singleton. */
    private static final TextureLoader INSTANCE = new TextureLoader();

    /** Subdirectory name where all textures are stored. */
    private static final String TEXTURE_DIR = "images";

    /** Subdirectory name where faction textures are stored. */
    private static final String FACTION_SUB_DIR = "factions";

    /** Subdirectory name where fortress textures are stored. */
    private static final String FORTRESS_SUB_DIR = "fortress";

    /** Subdirectory name where map textures are stored. */
    private static final String MAP_SUB_DIR = "maps";

    /** Subdirectory name where node textures are stored. */
    private static final String NODE_SUB_DIR = "nodes";

    /** Subdirectory name where effects are stored. */
    private static final String EFFECTS_SUB_DIR = "effects";

    /** A map of all the textures. */
    private final Map<String, InputStream> textures;

    /** A map of all the parsed and cached textures. */
    private final Map<String, Image> cachedTextures;

    /**
     * A private constructor to prevent instantiation of this class.
     */
    private TextureLoader() {
        textures = new HashMap<>();
        cachedTextures = new HashMap<>();
        initTextures();
    }

    /**
     * Get the instance of this texture loader.
     *
     * @return The texture loader instance.
     */
    public static TextureLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves an input stream for a file in the "resources" folder based on the
     * given filename.
     *
     * @param first Name/path of the file within the resource folder to load.
     * @param more  Additional items of the path.
     * @return Input stream used to read the loaded file. Throws an exception if the
     *         file does not exist.
     */
    private InputStream getResourceFromPath(String first, String... more) {
        String resourceFilePath = Path.of(first, more).toString();
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourceFilePath);

        if (inputStream == null) {
            throw new IllegalArgumentException("File " + resourceFilePath + " not found.");
        }
        return inputStream;
    }

    /**
     * Initializes the textures map with all the default textures.
     */
    private void initTextures() {
        textures.put("factionMen", getResourceFromPath(TEXTURE_DIR, FACTION_SUB_DIR, "men.png"));
        textures.put("factionElves", getResourceFromPath(TEXTURE_DIR, FACTION_SUB_DIR, "elves.png"));
        textures.put("factionDwarves", getResourceFromPath(TEXTURE_DIR, FACTION_SUB_DIR, "dwarves.png"));
        textures.put("factionMordor", getResourceFromPath(TEXTURE_DIR, FACTION_SUB_DIR, "mordor.png"));
        textures.put("factionIsengard", getResourceFromPath(TEXTURE_DIR, FACTION_SUB_DIR, "isengard.png"));

        textures.put("fortressMen", getResourceFromPath(TEXTURE_DIR, FORTRESS_SUB_DIR, "men.png"));
        textures.put("fortressElves", getResourceFromPath(TEXTURE_DIR, FORTRESS_SUB_DIR, "elves.png"));
        textures.put("fortressDwarves", getResourceFromPath(TEXTURE_DIR, FORTRESS_SUB_DIR, "dwarves.png"));
        textures.put("fortressMordor", getResourceFromPath(TEXTURE_DIR, FORTRESS_SUB_DIR, "mordor.png"));
        textures.put("fortressIsengard", getResourceFromPath(TEXTURE_DIR, FORTRESS_SUB_DIR, "isengard.png"));

        textures.put("node1", getResourceFromPath(TEXTURE_DIR, NODE_SUB_DIR, "node1.png"));
        textures.put("node2", getResourceFromPath(TEXTURE_DIR, NODE_SUB_DIR, "node2.png"));
        textures.put("node3", getResourceFromPath(TEXTURE_DIR, NODE_SUB_DIR, "node3.png"));
        textures.put("node4", getResourceFromPath(TEXTURE_DIR, NODE_SUB_DIR, "node4.png"));

        textures.put("mapTexture", getResourceFromPath(TEXTURE_DIR, MAP_SUB_DIR, "mapTexture.jpg"));
        textures.put("mapLotr", getResourceFromPath(TEXTURE_DIR, MAP_SUB_DIR, "lotrMap.jpg"));

        textures.put("flash", getResourceFromPath(TEXTURE_DIR, EFFECTS_SUB_DIR, "clash.png"));
    }

    /**
     * Retrieves an empty texture with the provided width and height.
     *
     * @param width  The width of the empty texture.
     * @param height The height of the empty texture.
     * @return An empty texture with the provided width and height.
     */
    private Image getEmptyTexture(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Reads an image from the provided input stream and scales it to the given
     * width and height.
     *
     * @param inputStream The input stream to read the image from.
     * @param width       The width to scale the image to.
     * @param height      The height to scale the image to.
     * @return The scaled image, or an empty texture if an error occurs.
     */
    private Image readScaledImage(InputStream inputStream, int width, int height) {
        try {
            Image loadedImage = ImageIO.read(inputStream);
            return loadedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            return getEmptyTexture(width, height);
        }
    }

    /**
     * Retrieves a scaled texture from the provided path. Will return a blank
     * texture in case an invalid path was
     * provided. If the image was loaded before already, it will be retrieved from
     * the cache. Note that in this case,
     * the width and height parameters are ignored. If you want to resize it, call
     * the method with resized=true.
     *
     * @param path   The path at which the texture is located.
     * @param width  The width the image should have.
     * @param height The height the image should have.
     * @return A scaled texture located at the provided path.
     */
    public Image getTexture(Path path, int width, int height) {
        return getTexture(path, width, height, false);
    }

    /**
     * Retrieves a scaled texture from the provided path. Will return a blank
     * texture in case an invalid path was
     * provided. This function does not use the cached images if resized is set to
     * true.
     * As a result, it is slightly less efficient. However, if the size of an image
     * changes frequently,
     * you can call this function with resized=true.
     *
     * @param path    The path at which the texture is located.
     * @param width   The width the image should have.
     * @param height  The height the image should have.
     * @param resized Whether the image is resized or not. True to ignore the cached
     *                images, false otherwise.
     * @return A scaled texture located at the provided path.
     */
    public Image getTexture(Path path, int width, int height, boolean resized) {
        String name = path.toString();
        if (cachedTextures.containsKey(name) && !resized) {
            return cachedTextures.get(name);
        }
        Image image = readScaledImage(getResourceFromPath(path.toString()), width, height);
        cachedTextures.put(name, image);
        return image;
    }

    /**
     * Retrieves a scaled texture with the provided name. Will return a blank
     * texture in case an invalid name was
     * provided. If the image was loaded before already, it will be retrieved from
     * the cache. Note that in this case,
     * the width and height parameters are ignored. If you want to resize it, call
     * the method with resized=true.
     *
     * @param name   The name of the texture. For a list of available texture names:
     *               {@see TextureLoader}
     * @param width  The width the image should have.
     * @param height The height the image should have.
     * @return A scaled texture located at the provided path.
     */
    public Image getTexture(String name, int width, int height) {
        return getTexture(name, width, height, false);
    }

    /**
     * Retrieves a scaled texture with the provided name. Will return a blank
     * texture in case an invalid name was
     * provided. This function does not use the cached images if resized is set to
     * true.
     * As a result, it is slightly less efficient. However, if the size of an image
     * changes frequently,
     * you can call this function with resized=true.
     *
     * @param name    The name of the texture. For a list of available texture
     *                names: {@see TextureLoader}
     * @param width   The width the image should have.
     * @param height  The height the image should have.
     * @param resized Whether the image is resized or not. True to ignore the cached
     *                images, false otherwise.
     * @return A scaled texture located at the provided path.
     */
    public Image getTexture(String name, int width, int height, boolean resized) {
        if (cachedTextures.containsKey(name) && !resized) {
            return cachedTextures.get(name);
        }
        if (!textures.containsKey(name)) {
            return getEmptyTexture(width, height);
        }
        Image image = readScaledImage(textures.get(name), width, height);
        cachedTextures.put(name, image);
        return image;
    }

}
