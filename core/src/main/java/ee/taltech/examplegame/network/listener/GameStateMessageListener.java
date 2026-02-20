package ee.taltech.examplegame.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.examplegame.game.GameStateManager;
import message.GameStateMessage;

/**
 * Listener for handling incoming GameStateMessages from the server.
 */
public class GameStateMessageListener extends Listener {

    private final GameStateManager gameStateManager;

    public GameStateMessageListener(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    /**
     * Processes received GameStateMessage objects.
     * @param connection Connection that sent the message.
     * @param object     Received object (in this case GameStateMessage).
     */
    @Override
    public void received(Connection connection, Object object) {
        super.received(connection, object);

        if (object instanceof GameStateMessage gameStateMessage) {
            // Update the game state
            gameStateManager.setLatestGameStateMessage(gameStateMessage);
            gameStateMessage.getPlayerStates().forEach(playerState -> {
                // Ignore our own player
                if (playerState.getId() == connection.getID())
                    return;

                // Sync remote players
                // In a real game, you would interpolate here
                // For now, we need a way to access the remote players list in the client
                // game/screen
            });
        }
    }
}
