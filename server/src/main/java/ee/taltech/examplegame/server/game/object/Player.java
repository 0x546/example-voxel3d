package ee.taltech.examplegame.server.game.object;

import com.esotericsoftware.kryonet.Connection;

import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_LIVES_COUNT;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_SPAWN_X;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_SPAWN_Y;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_SPAWN_Z;
import static ee.taltech.examplegame.shared.constant.Constants.VOID_LEVEL;
import ee.taltech.examplegame.shared.message.PlayerMovementMessage;
import ee.taltech.examplegame.shared.message.dto.PlayerState;
import ee.taltech.examplegame.shared.physics.VoxelPhysics;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.listener.PlayerMovementListener;
import ee.taltech.examplegame.shared.world.World;
import lombok.Getter;
import lombok.Setter;

/**
 * Server-side representation of a player in the game.
 * Handles 3D physics, collision, and input processing.
 */
@Getter
@Setter
public class Player {
    private final Connection connection;
    // Keep track of listener objects for each player connection, so they can be disposed when the game ends
    private final PlayerMovementListener movementListener = new PlayerMovementListener(this);

    private final int id;
    private final GameInstance game;

    private int lives = PLAYER_LIVES_COUNT;

    @Getter private final VoxelPhysics.PhysicsState physicsState = new VoxelPhysics.PhysicsState();
    @Getter private final VoxelPhysics.InputState inputState = new VoxelPhysics.InputState();

    public Player(Connection connection, GameInstance game) {
        this.connection = connection;
        this.id = connection.getID();
        this.game = game;
        this.connection.addListener(movementListener);

        // Spawn point
        physicsState.setX(PLAYER_SPAWN_X);
        physicsState.setY(PLAYER_SPAWN_Y);
        physicsState.setZ(PLAYER_SPAWN_Z);
    }

    public void handleInput(PlayerMovementMessage message) {
        inputState.setMoveForward(message.getMoveForward());
        inputState.setMoveSideways(message.getMoveSideways());
        inputState.setJump(message.isJump());
        inputState.setSneak(message.isSneak());
        inputState.setFly(message.isFly());
        inputState.setYaw(message.getYaw());
        inputState.setPitch(message.getPitch());
    }

    public void update(float delta, World world, float time) {
        VoxelPhysics.update(physicsState, inputState, delta, time, world);

        // Bounds check (keep in world)
        if (physicsState.getY() < VOID_LEVEL) { // Void kill
            lives = 0;
        }
    }

    /**
     * Returns the current state of the player, consisting of their position and remaining lives.
     */
    public PlayerState getState() {
        PlayerState playerState = new PlayerState();
        playerState.setId(connection.getID());
        playerState.setX(physicsState.getX());
        playerState.setY(physicsState.getY());
        playerState.setZ(physicsState.getZ());
        playerState.setVx(physicsState.getVx());
        playerState.setVy(physicsState.getVy());
        playerState.setVz(physicsState.getVz());
        playerState.setYaw(inputState.getYaw());
        playerState.setPitch(inputState.getPitch());
        playerState.setLives(lives);
        return playerState;
    }

    /**
     * Removes the movement and shooting listeners from the player's connection.
     * This should be called when the player disconnects or the game ends.
     * Disposing of the listeners prevents potential thread exceptions when reusing
     * same connections for future game instances.
     */
    public void dispose() {
        connection.removeListener(movementListener);
    }
}
