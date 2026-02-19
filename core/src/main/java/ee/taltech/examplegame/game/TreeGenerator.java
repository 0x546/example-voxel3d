package ee.taltech.examplegame.game;

import com.badlogic.gdx.math.MathUtils;

/**
 * Places simple trees (wood + leaves) into a blocks array.
 * Logic is decoupled from fixed magic numbers, allowing the tree to scale with trunkHeight.
 */
public class TreeGenerator {

    private final int width;
    private final int height;
    private final int depth;

    // Configuration
    private static final int TRUNK_HEIGHT = 4;
    // How far down from the top of the trunk the leaves start
    private static final int LEAVES_START_OFFSET = 1;
    // How far above the trunk the leaves extend
    private static final int LEAVES_TOP_OFFSET = 1;
    // Radius of the leaves
    private static final int LEAF_RADIUS = 2;
    // Radius to check for collisions before planting
    private static final int CLEAR_RADIUS = 2;
    // Probability to plant a tree per valid chunk
    private static final float PLANT_CHANCE = 0.05f;

    public TreeGenerator(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    /**
     * Attempts to grow sparse trees across the world.
     *
     * @param blocks 3D block array to mutate (blocks[x][y][z])
     */
    public void growTrees(int[][][] blocks) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                tryPlantTreeAt(blocks, x, z);
            }
        }
    }

    /**
     * Checks conditions and plants a single tree at the specific column if valid.
     */
    private void tryPlantTreeAt(int[][][] blocks, int x, int z) {
        int groundY = findGroundY(blocks, x, z);

        // 1. Check valid ground height and material
        if (groundY < 0 || groundY < 6) return;
        if (blocks[x][groundY][z] != ProceduralVoxelWorld.MAT_GRASS) return;

        // 2. Random chance
        if (MathUtils.random() >= PLANT_CHANCE) return;

        // 3. Check spatial availability
        int plantY = groundY + 1;
        if (!isVolumeClear(blocks, x, plantY, z)) return;

        // 4. Plant
        buildTrunk(blocks, x, plantY, z);
        buildLeaves(blocks, x, plantY, z);
    }

    private int findGroundY(int[][][] blocks, int x, int z) {
        for (int y = height - 1; y >= 0; y--) {
            if (blocks[x][y][z] != ProceduralVoxelWorld.MAT_AIR) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Checks if the area required for the tree (trunk + leaves) is free of obstacles.
     */
    private boolean isVolumeClear(int[][][] blocks, int baseX, int baseY, int baseZ) {
        // Calculate the bounding box for the whole tree
        int minX = baseX - CLEAR_RADIUS;
        int maxX = baseX + CLEAR_RADIUS;
        int minZ = baseZ - CLEAR_RADIUS;
        int maxZ = baseZ + CLEAR_RADIUS;

        int minY = baseY;
        int maxY = baseY + TRUNK_HEIGHT + LEAVES_TOP_OFFSET;

        // Clamp to world bounds
        int startX = Math.max(0, minX);
        int endX   = Math.min(width - 1, maxX);
        int startZ = Math.max(0, minZ);
        int endZ   = Math.min(depth - 1, maxZ);
        int startY = Math.max(0, minY);
        int endY   = Math.min(height - 1, maxY);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    int material = blocks[x][y][z];
                    if (material == ProceduralVoxelWorld.MAT_WOOD ||
                        material == ProceduralVoxelWorld.MAT_LEAVES) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void buildTrunk(int[][][] blocks, int x, int baseY, int z) {
        for (int i = 0; i < TRUNK_HEIGHT; i++) {
            // Only overwrite air or leaves, don't break existing structures
            // Note: Since we checked isVolumeClear, this is mostly just setting the block
            safeSetBlock(blocks, x, baseY + i, z, ProceduralVoxelWorld.MAT_WOOD, false);
        }
    }

    private void buildLeaves(int[][][] blocks, int baseX, int baseY, int baseZ) {
        // Leaves start below the top of the trunk and extend above it
        int startLeafY = baseY + TRUNK_HEIGHT - LEAVES_START_OFFSET;
        int endLeafY   = baseY + TRUNK_HEIGHT + LEAVES_TOP_OFFSET;

        for (int y = startLeafY; y <= endLeafY; y++) {
            // Determine distance from the "center" of the leaves (vertical center)
            // or simply make the radius smaller at the very top
            boolean isTopLayer = (y == endLeafY);

            // Generate a layer of leaves
            for (int lx = -LEAF_RADIUS; lx <= LEAF_RADIUS; lx++) {
                for (int lz = -LEAF_RADIUS; lz <= LEAF_RADIUS; lz++) {
                    if (shouldSkipCorner(lx, lz, isTopLayer) || (lx == 0 && lz == 0 && y < baseY + TRUNK_HEIGHT))
                        continue;

                    safeSetBlock(blocks, baseX + lx, y, baseZ + lz, ProceduralVoxelWorld.MAT_LEAVES, true);
                }
            }
        }
    }

    /**
     * Determines the shape of the leaves.
     * Returns true if this voxel should be empty (rounding the corners).
     */
    private boolean shouldSkipCorner(int lx, int lz, boolean isTopLayer) {
        int dist = Math.abs(lx) + Math.abs(lz);

        // If it's the very top, make it a tight cross shape (radius 1 effectively)
        if (isTopLayer) {
            return dist > 1;
        }

        // For lower layers, round the corners of the 5x5 box
        // (If x and z are both at max radius, skip)
        return (Math.abs(lx) == LEAF_RADIUS && Math.abs(lz) == LEAF_RADIUS);
    }

    /**
     * Sets a block safely within bounds.
     *
     * @param softSet if true, only replaces AIR or existing LEAVES (won't destroy other blocks)
     */
    private void safeSetBlock(int[][][] blocks, int x, int y, int z, int material, boolean softSet) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) return;

        if (softSet) {
            int current = blocks[x][y][z];
            if (current != ProceduralVoxelWorld.MAT_AIR && current != ProceduralVoxelWorld.MAT_LEAVES) {
                return;
            }
        }
        blocks[x][y][z] = material;
    }
}
