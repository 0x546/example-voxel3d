package ee.taltech.examplegame.shared.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ee.taltech.examplegame.shared.world.Chunk;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDataMessage {
    private Chunk chunk;
}

