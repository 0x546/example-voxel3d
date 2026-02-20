package ee.taltech.examplegame.game;

import java.util.ArrayList;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.network.listener.GameStateMessageListener;
import lombok.Getter;
import lombok.Setter;
import message.GameStateMessage;

@Setter
@Getter
public class GameStateManager {

    private GameStateMessage latestGameStateMessage;

    /**
     * GameStateManager listens for GameStateMessages sent from the server
     * and forwards it to relevant components like the HUD and Arena.
     */
    public GameStateManager() {
        // For listening updates from the server
        ServerConnection
            .getInstance()
            .getClient()
            .addListener(new GameStateMessageListener(this));

        // Initialize latestGameStateMessage to prevent NullPointerException, for example when the HUD is
        // already being rendered, but the first gameStateMessage from the server hasn't arrived yet.
        // Other components use this object without null checks, so a default
        // instance ensures safe usage and avoids unnecessary null handling.
        latestGameStateMessage = new GameStateMessage();
        latestGameStateMessage.setPlayerStates(new ArrayList<>());
        latestGameStateMessage.setGameTime(0);
        latestGameStateMessage.setAllPlayersHaveJoined(false);
    }

}
