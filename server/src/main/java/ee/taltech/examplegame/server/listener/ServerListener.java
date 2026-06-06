package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.shared.constant.Constants;
import ee.taltech.examplegame.shared.message.BlockChangeMessage;
import ee.taltech.examplegame.shared.message.ChunkRequestMessage;
import ee.taltech.examplegame.shared.message.GameJoinMessage;
import ee.taltech.examplegame.shared.message.GameLeaveMessage;
import ee.taltech.examplegame.shared.message.GenerateWorldMessage;
import ee.taltech.examplegame.shared.message.PlayerRespawnMessage;
import ee.taltech.examplegame.shared.message.ServerStatusRequestMessage;
import ee.taltech.examplegame.shared.message.ServerStatusResponseMessage;


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

        if (object instanceof ServerStatusRequestMessage) {
            handleServerStatusRequest(connection);
            return;
        } else if (object instanceof GameJoinMessage) {
            handleGameJoin(connection);
        } else if (object instanceof GameLeaveMessage) {
            handleGameLeave(connection);
        } else if (object instanceof PlayerRespawnMessage) {
            handlePlayerRespawn(connection);
        } else if (object instanceof ChunkRequestMessage req) {
            handleChunkRequest(connection, req);
        } else if (object instanceof BlockChangeMessage bcm) {
            handleBlockChange(bcm);
        } else if (object instanceof GenerateWorldMessage
            && handleGenerateWorld(connection)) {
            return;
        }

        super.received(connection, object);
    }

    private void handleServerStatusRequest(Connection connection) {
        boolean running = game != null;
        int count = running ? game.getPlayers().size() : 0;
        connection.sendTCP(new ServerStatusResponseMessage(running, count));
    }

    private void handleGameJoin(Connection connection) {
        if (game == null) {
            game = new GameInstance(this, connection);  // Create a new game instance for the first player (connection)
            game.start();  // Start the Thread, which contains the main game loop
        } else {
            game.addConnection(connection);  // Add a second player (connection) if there is enough room in the game
        }
    }

    private void handleGameLeave(Connection connection) {
        if (game != null) {
            game.removeConnection(connection);
        }
    }

    private void handlePlayerRespawn(Connection connection) {
        if (game != null) {
            game.getPlayers().stream()
                .filter(p -> p.getConnection().equals(connection))
                .findFirst()
                .ifPresent(p -> {
                    p.getPhysicsState().setX(Constants.PLAYER_SPAWN_X);
                    p.getPhysicsState().setY(Constants.PLAYER_SPAWN_Y);
                    p.getPhysicsState().setZ(Constants.PLAYER_SPAWN_Z);
                    p.getPhysicsState().setVx(0);
                    p.getPhysicsState().setVy(0);
                    p.getPhysicsState().setVz(0);
                });
        }
    }

    private void handleChunkRequest(Connection connection, ChunkRequestMessage req) {
        if (game != null) {
            game.handleChunkRequest(connection, req.getChunkX(), req.getChunkZ());
        }
    }

    private void handleBlockChange(BlockChangeMessage bcm) {
        if (game != null) {
            game.handleBlockChange(bcm.getX(), bcm.getY(), bcm.getZ(), bcm.getBlockType());
        }
    }

    private boolean handleGenerateWorld(Connection connection) {
        if (game != null) {
            if (!game.getPlayers().isEmpty()) {
                // Two players clicked "New Game" simultaneously. Server will gracefully redirect the latecomer to simply join the active newly started game!
                game.addConnection(connection);
                return true;
            }
            // Clear dead state
            game.disposeGame();
        }
        game = new GameInstance(this, connection);
        game.start();
        return false;
    }

    /**
     * Disposes of the current game instance by setting the game reference to null.
     */
    public void disposeGame() {
        this.game = null;
    }
}
