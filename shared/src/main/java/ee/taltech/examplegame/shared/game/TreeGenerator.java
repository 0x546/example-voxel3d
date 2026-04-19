package ee.taltech.examplegame.shared.game;

import java.util.Random;

import constant.BlockConstants;
import ee.taltech.examplegame.shared.world.Chunk;
import static constant.Constants.CLEAR_RADIUS;
import static constant.Constants.LEAF_RADIUS;
import static constant.Constants.LEAVES_START_OFFSET;
import static constant.Constants.LEAVES_TOP_OFFSET;
import static constant.Constants.PLANT_CHANCE;
import static constant.Constants.TRUNK_HEIGHT;

/**
 * Places simple trees iteratively and deterministically across chunks boundaries.
 */
public class TreeGenerator {

    public void growTrees(TerrainGenerator terrainGen, Chunk chunk) {
        int worldX = chunk.getChunkX() * Chunk.SIZE_X;
        int worldZ = chunk.getChunkZ() * Chunk.SIZE_Z;

        int maxRadius = Math.max(LEAF_RADIUS, CLEAR_RADIUS);

        for (int x = -maxRadius; x < Chunk.SIZE_X + maxRadius; x++) {
            for (int z = -maxRadius; z < Chunk.SIZE_Z + maxRadius; z++) {
                tryPlantTreeAt(chunk, terrainGen, worldX + x, worldZ + z);
            }
        }
    }

    private void tryPlantTreeAt(Chunk chunk, TerrainGenerator terrainGen, int x, int z) {
        int surfaceHeight = terrainGen.calculateSurfaceHeight(x, z);
        int groundY = surfaceHeight - 1;

        if (groundY < 6 || groundY < terrainGen.waterLevel)
            return;

        long seed = (x * 31298711L) ^ (z * 1161293L) ^ 12345L;
        Random r = new Random(seed);
        if (r.nextFloat() >= PLANT_CHANCE)
            return;

        if (!isVolumeClear(terrainGen, x, z, seed))
            return;

        int plantY = groundY + 1;
        buildTrunk(chunk, x, plantY, z);
        buildLeaves(chunk, x, plantY, z);
    }

    private boolean isVolumeClear(TerrainGenerator terrainGen, int baseX, int baseZ, long originalSeed) {
        int minX = baseX - CLEAR_RADIUS;
        int maxX = baseX + CLEAR_RADIUS;
        int minZ = baseZ - CLEAR_RADIUS;
        int maxZ = baseZ + CLEAR_RADIUS;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x == baseX && z == baseZ) continue;

                if (hasConflictingTree(terrainGen, x, z, originalSeed)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasConflictingTree(TerrainGenerator terrainGen, int x, int z, long originalSeed) {
        int sH = terrainGen.calculateSurfaceHeight(x, z);
        int gY = sH - 1;
        if (gY >= 6 && gY >= terrainGen.waterLevel) {
            long seedB = (x * 31298711L) ^ (z * 1161293L) ^ 12345L;
            Random rB = new Random(seedB);
            return rB.nextFloat() < PLANT_CHANCE && seedB > originalSeed;
        }
        return false;
    }

    private void buildTrunk(Chunk chunk, int x, int baseY, int z) {
        for (int i = 0; i < TRUNK_HEIGHT; i++) {
            safeSetBlock(chunk, x, baseY + i, z, BlockConstants.MAT_WOOD, true);
        }
    }

    private void buildLeaves(Chunk chunk, int baseX, int baseY, int baseZ) {
        int startLeafY = baseY + TRUNK_HEIGHT - LEAVES_START_OFFSET;
        int endLeafY = baseY + TRUNK_HEIGHT + LEAVES_TOP_OFFSET;

        for (int y = startLeafY; y <= endLeafY; y++) {
            boolean isTopLayer = (y == endLeafY);

            for (int lx = -LEAF_RADIUS; lx <= LEAF_RADIUS; lx++) {
                for (int lz = -LEAF_RADIUS; lz <= LEAF_RADIUS; lz++) {
                    if (shouldSkipCorner(lx, lz, isTopLayer) || (lx == 0 && lz == 0 && y < baseY + TRUNK_HEIGHT))
                        continue;

                    safeSetBlock(chunk, baseX + lx, y, baseZ + lz, BlockConstants.MAT_LEAVES, false);
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

    private void safeSetBlock(Chunk chunk, int x, int y, int z, int material, boolean isWood) {
        if (y < 0 || y >= Chunk.SIZE_Y) return;

        int localX = x - (chunk.getChunkX() * Chunk.SIZE_X);
        int localZ = z - (chunk.getChunkZ() * Chunk.SIZE_Z);

        if (localX >= 0 && localX < Chunk.SIZE_X && localZ >= 0 && localZ < Chunk.SIZE_Z) {
            int current = chunk.getBlocks()[localX][y][localZ];
            if (current == BlockConstants.MAT_AIR || (!isWood && current == BlockConstants.MAT_LEAVES)) {
                chunk.getBlocks()[localX][y][localZ] = material;
            }
        }
    }
}
