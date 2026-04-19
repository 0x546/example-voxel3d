package ee.taltech.examplegame.server.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;

import ee.taltech.examplegame.shared.constant.BlockConstants;
import static ee.taltech.examplegame.shared.constant.Constants.FALLING_BLOCK_UPDATE_INTERVAL;
import static ee.taltech.examplegame.shared.constant.Constants.GAME_TICK_RATE;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_COUNT_IN_GAME;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_LEVEL;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.listener.ServerListener;
import ee.taltech.examplegame.shared.game.TerrainGenerator;
import ee.taltech.examplegame.shared.game.TreeGenerator;
import ee.taltech.examplegame.shared.message.BlockChangeMessage;
import ee.taltech.examplegame.shared.message.ChunkDataMessage;
import ee.taltech.examplegame.shared.world.Chunk;
import ee.taltech.examplegame.shared.world.World;
import lombok.Getter;

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

    private float waterUpdateTimer = 0;
    private static final float WATER_UPDATE_INTERVAL = 0.5f;

    private float fallingBlockTimer = 0;

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

    public synchronized void handleBlockChange(Connection connection, int x, int y, int z, int blockType) {
        world.setBlock(x, y, z, blockType);

        BlockChangeMessage msg = new BlockChangeMessage(x, y, z, blockType);
        connections.forEach(conn -> {
            if (conn != connection) {
                conn.sendTCP(msg);
            }
        });
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

            // update water flow
            updateWaterFlow(finalDelta);

            // update falling blocks
            updateFallingBlocks(finalDelta);

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
            // If no players are connected, do not stop game loop so world persists
            // Just idle

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

    private void updateWaterFlow(float delta) {
        waterUpdateTimer += delta;
        if (waterUpdateTimer < WATER_UPDATE_INTERVAL) return;
        waterUpdateTimer = 0;

        List<BlockChange> changes = new ArrayList<>();
        processWorldBlocks((worldX, worldY, worldZ, mat) -> {
            if (mat == BlockConstants.MAT_WATER) {
                // 1. Air directly below water becomes water
                checkAndAddWaterChange(worldX, worldY - 1, worldZ, changes);

                // 2. Air next to water becomes water if non-air is under it
                checkAndAddHorizontalWaterFlow(worldX + 1, worldY, worldZ, changes);
                checkAndAddHorizontalWaterFlow(worldX - 1, worldY, worldZ, changes);
                checkAndAddHorizontalWaterFlow(worldX, worldY, worldZ + 1, changes);
                checkAndAddHorizontalWaterFlow(worldX, worldY, worldZ - 1, changes);
            }
        });

        for (BlockChange change : changes) {
            handleBlockChange(null, change.x, change.y, change.z, BlockConstants.MAT_WATER);
        }
    }

    private void updateFallingBlocks(float delta) {
        fallingBlockTimer += delta;
        if (fallingBlockTimer < FALLING_BLOCK_UPDATE_INTERVAL) return;
        fallingBlockTimer = 0;

        List<BlockChange> changes = new ArrayList<>();
        processWorldBlocks((worldX, worldY, worldZ, mat) -> {
            if (mat == BlockConstants.MAT_SAND) {
                int below = world.getBlock(worldX, worldY - 1, worldZ);
                if (below == BlockConstants.MAT_AIR || below == BlockConstants.MAT_WATER) {
                    changes.add(new BlockChange(worldX, worldY, worldZ, BlockConstants.MAT_AIR));
                    changes.add(new BlockChange(worldX, worldY - 1, worldZ, BlockConstants.MAT_SAND));
                }
            }
        });

        for (BlockChange change : changes) {
            handleBlockChange(null, change.x, change.y, change.z, change.type);
        }
    }

    private void processWorldBlocks(BlockProcessor processor) {
        for (Chunk chunk : getActiveChunksSafe()) {
            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    for (int y = 1; y < Chunk.SIZE_Y; y++) {
                        int worldX = chunk.getChunkX() * Chunk.SIZE_X + x;
                        int worldZ = chunk.getChunkZ() * Chunk.SIZE_Z + z;
                        int mat = chunk.getBlocks()[x][y][z];
                        processor.process(worldX, y, worldZ, mat);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface BlockProcessor {
        void process(int x, int y, int z, int mat);
    }

    private List<Chunk> getActiveChunksSafe() {
        synchronized (this) {
            return new ArrayList<>(world.getChunks());
        }
    }

    private void checkAndAddWaterChange(int x, int y, int z, List<BlockChange> changes) {
        if (y < 0) return;
        if (world.getBlock(x, y, z) == BlockConstants.MAT_AIR) {
            changes.add(new BlockChange(x, y, z, BlockConstants.MAT_WATER));
        }
    }

    private void checkAndAddHorizontalWaterFlow(int x, int y, int z, List<BlockChange> changes) {
        if (world.getBlock(x, y, z) == BlockConstants.MAT_AIR) {
            int below = world.getBlock(x, y - 1, z);
            if (below != BlockConstants.MAT_AIR && below != -1) { // block below is solid/water
                changes.add(new BlockChange(x, y, z, BlockConstants.MAT_WATER));
            }
        }
    }

    private record BlockChange(int x, int y, int z, int type) {}
}
