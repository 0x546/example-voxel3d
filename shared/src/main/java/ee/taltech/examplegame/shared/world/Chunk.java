package ee.taltech.examplegame.shared.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 256;
    public static final int SIZE_Z = 16;

    private int chunkX;
    private int chunkZ;
    private int[][][] blocks;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    }
}

