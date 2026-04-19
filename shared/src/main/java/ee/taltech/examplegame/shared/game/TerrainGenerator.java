package ee.taltech.examplegame.shared.game;

import ee.taltech.examplegame.shared.constant.BlockConstants;
import ee.taltech.examplegame.shared.world.Chunk;
import static ee.taltech.examplegame.shared.constant.Constants.DIRT_LAYER_THICKNESS;

/**
 * Generates base terrain heights and fills a Chunk with
 * stone/dirt/grass.
 * Handles water generation and ensures proper soil types (dirt vs grass)
 * based on water level.
 */
public class TerrainGenerator {

    // Generation Settings
    private static final int BASE_HEIGHT = 16;
    public final int waterLevel;

    public TerrainGenerator(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    /**
     * Fills the chunk array.
     */
    public void generate(Chunk chunk) {
        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                generateColumn(chunk, localX, localZ);
            }
        }
    }

    /**
     * Generates a single vertical column at (localX, localZ).
     */
    private void generateColumn(Chunk chunk, int localX, int localZ) {
        int worldX = chunk.getChunkX() * Chunk.SIZE_X + localX;
        int worldZ = chunk.getChunkZ() * Chunk.SIZE_Z + localZ;

        int surfaceHeight = calculateSurfaceHeight(worldX, worldZ);

        // 1. Fill Solid Terrain
        for (int y = 0; y < surfaceHeight; y++) {
            chunk.getBlocks()[localX][y][localZ] = getGroundMaterial(y, surfaceHeight);
        }

        // 2. Fill Water (if surface is below water level)
        fillWater(chunk, localX, localZ, surfaceHeight);
    }

    /**
     * Calculates the terrain height for a specific coordinate
     * using noise functions.
     */
    public int calculateSurfaceHeight(int x, int z) {
        // Simple sine-wave based noise
        float noise = (float) (Math.sin(x * 0.2f) * 2.5f
                + Math.cos(z * 0.3f) * 2.5f
                + Math.sin((x + z) * 0.1f) * 1.5f);

        int h = (int) (BASE_HEIGHT + noise);

        // Clamp
        return Math.clamp(h, 0, Chunk.SIZE_Y - 6);
    }

    /**
     * Determines which material to place at height y.
     */
    private int getGroundMaterial(int y, int surfaceHeight) {
        boolean isTopBlock = (y == surfaceHeight - 1);

        // Top layer logic
        if (isTopBlock) {
            boolean isUnderwater = (y < waterLevel);
            return isUnderwater ? BlockConstants.MAT_DIRT : BlockConstants.MAT_GRASS;
        }

        // Just below surface logic
        boolean isDirtLayer = (y >= surfaceHeight - DIRT_LAYER_THICKNESS);
        if (isDirtLayer) {
            return BlockConstants.MAT_DIRT;
        }

        // Deep logic
        return BlockConstants.MAT_STONE;
    }

    private void fillWater(Chunk chunk, int localX, int localZ, int startY) {
        if (startY >= waterLevel)
            return;

        int fillMax = Math.min(waterLevel, Chunk.SIZE_Y);

        for (int y = startY; y < fillMax; y++) {
            chunk.getBlocks()[localX][y][localZ] = BlockConstants.MAT_WATER;
        }
    }
}
