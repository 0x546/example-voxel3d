package ee.taltech.examplegame.shared.message;

import java.util.List;

import lombok.Data;
import ee.taltech.examplegame.shared.message.dto.PlayerState;

@Data
public class GameStateMessage {
    private List<PlayerState> playerStates;
    private int gameTime;
    private boolean allPlayersHaveJoined;
}
