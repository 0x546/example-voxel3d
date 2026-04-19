package message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkRequestMessage {
    private int chunkX;
    private int chunkZ;
}

