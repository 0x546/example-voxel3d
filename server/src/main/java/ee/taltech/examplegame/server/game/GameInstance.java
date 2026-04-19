package ee.taltech.examplegame.server.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;

import static constant.Constants.GAME_TICK_RATE;
import static constant.Constants.PLAYER_COUNT_IN_GAME;
import static constant.Constants.WATER_LEVEL;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.listener.ServerListener;
import ee.taltech.examplegame.shared.game.TerrainGenerator;
import ee.taltech.examplegame.shared.game.TreeGenerator;
import ee.taltech.examplegame.shared.world.Chunk;
import ee.taltech.examplegame.shared.world.World;
import lombok.Getter;
import message.ChunkDataMessage;

/**
 * Represents the game logic and server-side management of the game instance.
 * Handles player connections, game state updates, bullet collisions, and
 * communication with clients.
 * <p>
 * This class extends {@link Thread} because the game loop needs to run
 * continuously
 * in the background, independent of other server operations. By running in a
 * separate thread,
 * it ensures that the game state updates at a fixed tick rate without blocking
 * other processes in the main server.
 */
public class GameInstance extends Thread {

    private final ServerListener server;
    private final GameStateHandler gameStateHandler = new GameStateHandler();

    private final Set<Connection> connections = new HashSet<>(); // Avoid a connection (player) joining the game twice
    @Getter
    private final List<Player> players = new ArrayList<>();

    // Server-side world for physics
    @Getter
    private final World world;

    private final TerrainGenerator terrainGenerator;
    private final TreeGenerator treeGenerator;

    /**
     * Initializes the game instance.
     *
     * @param server          Reference to ServerListener to call dispose() when the
     *                        game is finished or all players leave.
     * @param firstConnection Connection of the first player.
     */
    public GameInstance(ServerListener server, Connection firstConnection) {
        this.server = server;

        this.world = new World();
        this.terrainGenerator = new TerrainGenerator(WATER_LEVEL);
        this.treeGenerator = new TreeGenerator();

        Player newPlayer = new Player(firstConnection, this);
        players.add(newPlayer);
        connections.add(firstConnection);
    }

    public synchronized void handleChunkRequest(Connection connection, int chunkX, int chunkZ) {
        if (!world.hasChunk(chunkX, chunkZ)) {
            Chunk chunk = new Chunk(chunkX, chunkZ);
            world.addChunk(chunk);
            terrainGenerator.generate(chunk);
            treeGenerator.growTrees(terrainGenerator, chunk);
        }
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        connection.sendTCP(new ChunkDataMessage(chunk));
    }

    /**
     * Check if the game has the required number of players to start.
     */
    public boolean hasEnoughPlayers() {
        return connections.size() == PLAYER_COUNT_IN_GAME;
    }

    /**
     * Adds a new connection and player to the game.
     * If the required number of players is reached, the game is ready to start.
     *
     * @param connection Connection to the client side of the player.
     */
    public void addConnection(Connection connection) {
        if (connections.contains(connection)) {
            Log.info("Connection already in game: " + connection.getID());
            return;
        }

        if (hasEnoughPlayers()) {
            Log.info("Cannot add connection: Required number of players already connected.");
            return;
        }

        // Add new player and connection
        Player newPlayer = new Player(connection, this);
        players.add(newPlayer);
        connections.add(connection);

        // Check if the game is ready to start
        if (hasEnoughPlayers()) {
            gameStateHandler.setAllPlayersHaveJoined(true);
        }
    }

    public void removeConnection(Connection connection) {
        if (this.connections.remove(connection)) {
            players.removeIf(p -> p.getConnection().equals(connection));
        }
    }

    /**
     * Stops and disposes the current game instance, so a new one can be created
     * with the same or new players.
     */
    public void disposeGame() {
        players.forEach(Player::dispose); // remove movement and shooting listeners
        connections.clear();
        server.disposeGame(); // Sets the active game instance in main server to null
    }

    /**
     * Game loop. Updates the game state, checks for collisions, and sends updates
     * to clients.
     * The game loop runs until the game is stopped or no players remain.
     */
    @Override
    public void run() {
        boolean isGameRunning = true;
        long lastTime = System.nanoTime();

        while (isGameRunning) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;

            if (delta > 0.1f) delta = 0.1f;
            final float finalDelta = delta;

            gameStateHandler.incrementGameTimeIfPlayersPresent(finalDelta);

            // update players (physics, movement)
            float time = gameStateHandler.getGameTime();
            players.forEach(p -> p.update(finalDelta, world, time));

            // construct gameStateMessage
            var gameStateMessage = gameStateHandler.getGameStateMessage(players);
            // send the state of current game to all connected clients
            connections.forEach(connection -> connection.sendUDP(gameStateMessage));

            // If any player is dead, end the game
            if (players.stream().anyMatch(x -> x.getLives() == 0)) {
                // Use TCP to ensure that the last gameStateMessage reaches all clients
                connections.forEach(connection -> connection.sendTCP(gameStateMessage));
                disposeGame();
                isGameRunning = false;
            }
            // If no players are connected, stop the game loop
            if (connections.isEmpty()) {
                Log.info("No players connected, stopping game loop.");
                disposeGame();
                isGameRunning = false;
            }

            try {
                // We don't want to update the game state every millisecond, that would be
                // too much for the server to handle. So a tick rate is used to limit the
                // amount of updates per second.
                Thread.sleep(Duration.ofMillis(1000 / GAME_TICK_RATE));
            } catch (InterruptedException e) {
                Log.error("Game loop sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
