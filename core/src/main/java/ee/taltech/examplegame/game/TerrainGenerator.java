package ee.taltech.examplegame.game;

import com.badlogic.gdx.math.MathUtils;

/**
 * Generates base terrain heights and fills a 3D blocks array with stone/dirt/grass.
 * Handles water generation and ensures proper soil types (dirt vs grass) based on water level.
 */
public class TerrainGenerator {

    // Material Aliases for readability
    private static final int MAT_GRASS = ProceduralVoxelWorld.MAT_GRASS;
    private static final int MAT_DIRT  = ProceduralVoxelWorld.MAT_DIRT;
    private static final int MAT_STONE = ProceduralVoxelWorld.MAT_STONE;
    private static final int MAT_WATER = ProceduralVoxelWorld.MAT_WATER;

    // Generation Settings
    private static final int BASE_HEIGHT = 16;
    private static final int DIRT_LAYER_THICKNESS = 3;

    private final int width;
    private final int height;
    private final int depth;
    private final int waterLevel;

    public TerrainGenerator(int width, int height, int depth, int waterLevel) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.waterLevel = waterLevel;
    }

    /**
     * Fills the block array.
     */
    public void generate(int[][][] blocks) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                generateColumn(blocks, x, z);
            }
        }
    }

    /**
     * Generates a single vertical column at (x, z).
     * 1. Calculates height.
     * 2. Fills ground (Stone -> Dirt -> Grass/Dirt).
     * 3. Fills water if applicable.
     */
    private void generateColumn(int[][][] blocks, int x, int z) {
        int surfaceHeight = calculateSurfaceHeight(x, z);

        // 1. Fill Solid Terrain
        for (int y = 0; y < surfaceHeight; y++) {
            blocks[x][y][z] = getGroundMaterial(y, surfaceHeight);
        }

        // 2. Fill Water (if surface is below water level)
        fillWater(blocks, x, z, surfaceHeight);
    }

    /**
     * Calculates the terrain height for a specific coordinate using noise functions.
     */
    private int calculateSurfaceHeight(int x, int z) {
        // Simple sine-wave based noise
        float noise = (float) (Math.sin(x * 0.2f) * 2.5f
            + Math.cos(z * 0.3f) * 2.5f
            + Math.sin((x + z) * 0.1f) * 1.5f
            + (MathUtils.random() - 0.5f) * 0.5f);

        int h = (int) (BASE_HEIGHT + noise);

        // Clamp to ensure we don't go out of bounds or too close to the sky
        return MathUtils.clamp(h, 0, height - 6);
    }

    /**
     * Determines which material to place at height y, given the total column height.
     */
    private int getGroundMaterial(int y, int surfaceHeight) {
        boolean isTopBlock = (y == surfaceHeight - 1);

        // Top layer logic
        if (isTopBlock) {
            // If the top block is underwater, it must be DIRT, not GRASS
            boolean isUnderwater = (y < waterLevel);
            return isUnderwater ? MAT_DIRT : MAT_GRASS;
        }

        // Just below surface logic
        boolean isDirtLayer = (y >= surfaceHeight - DIRT_LAYER_THICKNESS);
        if (isDirtLayer) {
            return MAT_DIRT;
        }

        // Deep logic
        return MAT_STONE;
    }

    /**
     * Fills water from the surface up to the water level.
     */
    private void fillWater(int[][][] blocks, int x, int z, int startY) {
        if (startY >= waterLevel) return;

        // Clamp max water height to world height to avoid array index out of bounds
        int fillMax = Math.min(waterLevel, height);

        for (int y = startY; y < fillMax; y++) {
            blocks[x][y][z] = MAT_WATER;
        }
    }
}
