package ee.taltech.examplegame.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.examplegame.game.GameStateManager;
import ee.taltech.examplegame.shared.message.GameStateMessage;

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
        }
    }
}
