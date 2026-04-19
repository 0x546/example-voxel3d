package constant;

/**
 * Centered location for all material colors used in the voxel engine.
 * Colors are stored as RGB triplets (0.0 - 1.0).
 */
public class Colors {

    public record ColorRGB(float r, float g, float b) {}

    public static final ColorRGB GRASS = new ColorRGB(0.22f, 0.7f, 0.2f);
    public static final ColorRGB DIRT = new ColorRGB(0.45f, 0.25f, 0.05f);
    public static final ColorRGB STONE = new ColorRGB(0.5f, 0.5f, 0.5f);
    public static final ColorRGB WOOD = new ColorRGB(0.45f, 0.28f, 0.12f);
    public static final ColorRGB LEAVES = new ColorRGB(0.15f, 0.6f, 0.15f);
    public static final ColorRGB WATER = new ColorRGB(0.1f, 0.35f, 0.55f);
    public static final ColorRGB PLAYER = new ColorRGB(0.8f, 0.2f, 0.2f);
    public static final ColorRGB SAND = new ColorRGB(0.85f, 0.8f, 0.6f);
    public static final ColorRGB BRICK = new ColorRGB(0.75f, 0.35f, 0.2f);
    public static final ColorRGB GLASS = new ColorRGB(0.6f, 0.85f, 0.9f);

}
