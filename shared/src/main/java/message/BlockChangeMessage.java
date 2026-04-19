package message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockChangeMessage {
    private int x;
    private int y;
    private int z;
    private int blockType;
}
