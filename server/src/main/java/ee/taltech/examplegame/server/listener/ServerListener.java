package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.Player;
import message.ChunkRequestMessage;
import message.GameJoinMessage;
import message.GenerateWorldMessage;


/**
 * This class listens for all connections and messages that are
 * sent to the server by the clients.
 * <p>
 * It contains 3 methods that can be overridden to add custom logic
 */
public class ServerListener extends Listener {
    private GameInstance game;

    /**
     * When a client connects to the server, this method is called.
     * Include any logic that should be executed when a client connects to the server.
     * ex - add the client to the game, etc.
     *
     * @param connection The connection object that is created when a client connects to the server.
     */
    @Override
    public void connected(Connection connection) {
        Log.info("Client connected: " + connection.getRemoteAddressTCP().getAddress().getHostAddress());

        super.connected(connection);
    }

    /**
     * When a client disconnects from the server, this method is called.
     * Include any logic that should be executed when a client disconnects from the server.
     * ex - clean up resources, remove the client from the game, etc.
     *
     * @param connection The connection object of the client that disconnected.
     */
    @Override
    public void disconnected(Connection connection) {
        Log.info("Client disconnected");
        if (game != null) {
            game.removeConnection(connection);
        }
        super.disconnected(connection);
    }

    /**
     * When a message is received from a client, this method is called.
     * ex - client sends a JoinGame message, the server will add the client to the game.
     *
     * @param connection The connection object of the client that sent the message.
     * @param object     The object that is sent by the client.
     */
    @Override
    public void received(Connection connection, Object object) {
        Log.debug("Received message from client (" + connection.getRemoteAddressTCP().getAddress().getHostAddress() + "): " + object.toString());

        // when a GameJoinMessage is received, the server will add the connection to the game instance
        // if there is no active instance, a new one is created
        if (object instanceof GameJoinMessage) {
            if (game == null) {
                game = new GameInstance(this, connection);  // Create a new game instance for the first player (connection)
                game.start();  // Start the Thread, which contains the main game loop
            } else {
                game.addConnection(connection);  // Add a second player (connection) if there is enough room in the game
            }
        } else if (object instanceof message.GameLeaveMessage) {
            if (game != null) {
                game.removeConnection(connection);
            }
        } else if ((object instanceof message.PlayerRespawnMessage) && game != null) {
            game.getPlayers().stream()
                .filter(p -> p.getConnection().equals(connection))
                .findFirst()
                .ifPresent(p -> {
                    p.getPhysicsState().setX(constant.Constants.PLAYER_SPAWN_X);
                    p.getPhysicsState().setY(constant.Constants.PLAYER_SPAWN_Y);
                    p.getPhysicsState().setZ(constant.Constants.PLAYER_SPAWN_Z);
                    p.getPhysicsState().setVx(0);
                    p.getPhysicsState().setVy(0);
                    p.getPhysicsState().setVz(0);
                });
        } else if (object instanceof ChunkRequestMessage req && game != null) {
            game.handleChunkRequest(connection, req.getChunkX(), req.getChunkZ());
        } else if (object instanceof GenerateWorldMessage && game != null) {
            // Clear state, kick players
            for (var conn : game.getPlayers().stream().map(Player::getConnection).toList()) {
                conn.close();
            }
            game.disposeGame();
        }

        super.received(connection, object);
    }

    /**
     * Disposes of the current game instance by setting the game reference to null.
     */
    public void disposeGame() {
        this.game = null;
    }
}
