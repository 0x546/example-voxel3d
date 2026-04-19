package ee.taltech.examplegame.shared.world;

import java.util.HashMap;
import java.util.Map;
import constant.BlockConstants;

public class World {
    private final Map<String, Chunk> chunks = new HashMap<>();

    public void addChunk(Chunk chunk) {
        chunks.put(getChunkKey(chunk.getChunkX(), chunk.getChunkZ()), chunk);
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(getChunkKey(chunkX, chunkZ));
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        return chunks.containsKey(getChunkKey(chunkX, chunkZ));
    }

    public int getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return BlockConstants.MAT_AIR;

        int chunkX = (int) Math.floor((float) x / Chunk.SIZE_X);
        int chunkZ = (int) Math.floor((float) z / Chunk.SIZE_Z);

        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return -1; // Unloaded chunk
        }

        int localX = x - (chunkX * Chunk.SIZE_X);
        int localZ = z - (chunkZ * Chunk.SIZE_Z);

        return chunk.getBlocks()[localX][y][localZ];
    }

    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }

    public void clear() {
        chunks.clear();
    }
}

