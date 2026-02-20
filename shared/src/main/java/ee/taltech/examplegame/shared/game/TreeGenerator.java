package ee.taltech.examplegame.shared.game;

import java.util.Random;

import constant.BlockConstants;
import static constant.Constants.CLEAR_RADIUS;
import static constant.Constants.LEAF_RADIUS;
import static constant.Constants.LEAVES_START_OFFSET;
import static constant.Constants.LEAVES_TOP_OFFSET;
import static constant.Constants.PLANT_CHANCE;
import static constant.Constants.TRUNK_HEIGHT;

/**
 * Places simple trees into a blocks array.
 */
public class TreeGenerator {

    private final int width;
    private final int height;
    private final int depth;
    private final Random random = new Random(12345); // Fixed seed for deterministic trees

    public TreeGenerator(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public void growTrees(int[][][] blocks) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                tryPlantTreeAt(blocks, x, z);
            }
        }
    }

    private void tryPlantTreeAt(int[][][] blocks, int x, int z) {
        int groundY = findGroundY(blocks, x, z);

        if (groundY < 0 || groundY < 6)
            return;
        if (blocks[x][groundY][z] != BlockConstants.MAT_GRASS)
            return;

        if (random.nextFloat() >= PLANT_CHANCE)
            return;

        int plantY = groundY + 1;
        if (!isVolumeClear(blocks, x, plantY, z))
            return;

        buildTrunk(blocks, x, plantY, z);
        buildLeaves(blocks, x, plantY, z);
    }

    private int findGroundY(int[][][] blocks, int x, int z) {
        for (int y = height - 1; y >= 0; y--) {
            if (blocks[x][y][z] != BlockConstants.MAT_AIR) {
                return y;
            }
        }
        return -1;
    }

    private boolean isVolumeClear(int[][][] blocks, int baseX, int baseY, int baseZ) {
        int minX = baseX - CLEAR_RADIUS;
        int maxX = baseX + CLEAR_RADIUS;
        int minZ = baseZ - CLEAR_RADIUS;
        int maxZ = baseZ + CLEAR_RADIUS;

        int minY = baseY;
        int maxY = baseY + TRUNK_HEIGHT + LEAVES_TOP_OFFSET;

        int startX = Math.max(0, minX);
        int endX = Math.min(width - 1, maxX);
        int startZ = Math.max(0, minZ);
        int endZ = Math.min(depth - 1, maxZ);
        int startY = Math.max(0, minY);
        int endY = Math.min(height - 1, maxY);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    int material = blocks[x][y][z];
                    if (material == BlockConstants.MAT_WOOD ||
                            material == BlockConstants.MAT_LEAVES) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void buildTrunk(int[][][] blocks, int x, int baseY, int z) {
        for (int i = 0; i < TRUNK_HEIGHT; i++) {
            safeSetBlock(blocks, x, baseY + i, z, BlockConstants.MAT_WOOD, false);
        }
    }

    private void buildLeaves(int[][][] blocks, int baseX, int baseY, int baseZ) {
        int startLeafY = baseY + TRUNK_HEIGHT - LEAVES_START_OFFSET;
        int endLeafY = baseY + TRUNK_HEIGHT + LEAVES_TOP_OFFSET;

        for (int y = startLeafY; y <= endLeafY; y++) {
            boolean isTopLayer = (y == endLeafY);

            for (int lx = -LEAF_RADIUS; lx <= LEAF_RADIUS; lx++) {
                for (int lz = -LEAF_RADIUS; lz <= LEAF_RADIUS; lz++) {
                    if (shouldSkipCorner(lx, lz, isTopLayer) || (lx == 0 && lz == 0 && y < baseY + TRUNK_HEIGHT))
                        continue;

                    safeSetBlock(blocks, baseX + lx, y, baseZ + lz, BlockConstants.MAT_LEAVES, true);
                }
            }
        }
    }

    private boolean shouldSkipCorner(int lx, int lz, boolean isTopLayer) {
        int dist = Math.abs(lx) + Math.abs(lz);
        if (isTopLayer) {
            return dist > 1;
        }
        return (Math.abs(lx) == LEAF_RADIUS && Math.abs(lz) == LEAF_RADIUS);
    }

    private void safeSetBlock(int[][][] blocks, int x, int y, int z, int material, boolean softSet) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth)
            return;

        if (softSet) {
            int current = blocks[x][y][z];
            if (current != BlockConstants.MAT_AIR && current != BlockConstants.MAT_LEAVES) {
                return;
            }
        }
        blocks[x][y][z] = material;
    }
}
