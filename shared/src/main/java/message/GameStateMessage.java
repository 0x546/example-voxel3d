package message;

import java.util.List;

import lombok.Data;
import message.dto.PlayerState;

@Data
public class GameStateMessage {
    private List<PlayerState> playerStates;
    private int gameTime;
    private boolean allPlayersHaveJoined;
}
