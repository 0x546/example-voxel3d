package message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerStatusResponseMessage {
    private boolean instanceRunning;
    private int playerCount;
}
