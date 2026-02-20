package ee.taltech.examplegame.server.game.object;

import com.esotericsoftware.kryonet.Connection;
import constant.BlockConstants;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.listener.PlayerMovementListener;
import ee.taltech.examplegame.server.listener.PlayerShootingListener;
import lombok.Getter;
import lombok.Setter;
import message.PlayerInputMessage;
import message.dto.Direction;
import message.dto.PlayerState;

import static constant.Constants.PLAYER_LIVES_COUNT;

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
    private final PlayerShootingListener shootingListener = new PlayerShootingListener(this);

    private final int id;
    private final GameInstance game;

    // Position
    private float x;
    private float y;
    private float z;
    // Velocity
    private float vx;
    private float vy;
    private float vz;
    // Rotation
    private float yaw;
    private float pitch;

    private int lives = PLAYER_LIVES_COUNT;

    // Input state
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sneak;

    // Physics constants
    private static final float GRAVITY = 25f;
    private static final float JUMP_VELOCITY = 10f;
    private static final float MOVE_SPEED = 16f;
    private static final float DAMPING = 0.9f;
    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_HEIGHT = 1.8f;

    public Player(Connection connection, GameInstance game) {
        this.connection = connection;
        this.id = connection.getID();
        this.game = game;
        this.connection.addListener(movementListener);
        this.connection.addListener(shootingListener);

        // Spawn point
        this.x = 12f;
        this.y = 25f;
        this.z = 12f;
    }

    public void handleInput(PlayerInputMessage message) {
        this.up = message.isUp();
        this.down = message.isDown();
        this.left = message.isLeft();
        this.right = message.isRight();
        this.jump = message.isJump();
        this.sneak = message.isSneak();
        this.yaw = message.getYaw();
        this.pitch = message.getPitch();
    }

    public void update(float delta, int[][][] blocks) {
        // 1. Apply Input Forces
        float dx = (float) Math.sin(Math.toRadians(yaw));
        float dz = (float) Math.cos(Math.toRadians(yaw));

        // Forward/Back
        if (up) {
            vx -= dx * MOVE_SPEED * delta;
            vz -= dz * MOVE_SPEED * delta;
        }
        if (down) {
            vx += dx * MOVE_SPEED * delta;
            vz += dz * MOVE_SPEED * delta;
        }

        // Strafe Left/Right
        if (left) {
            vx -= dz * MOVE_SPEED * delta;
            vz += dx * MOVE_SPEED * delta;
        }
        if (right) {
            vx += dz * MOVE_SPEED * delta;
            vz -= dx * MOVE_SPEED * delta;
        }

        // Jump (only if on ground? For now, allow infinite jump for testing/flight if
        // needed, but lets try gravity)
        if (jump && isOnGround(blocks)) {
            vy = JUMP_VELOCITY;
        }

        // 2. Apply Gravity
        vy -= GRAVITY * delta;

        // 3. Apply Velocity
        float nextX = x + vx * delta;
        float nextY = y + vy * delta;
        float nextZ = z + vz * delta;

        // 4. Collision Detection (Simple AABB vs Voxel)
        // Check X axis
        if (!checkCollision(nextX, y, z, blocks)) {
            x = nextX;
        } else {
            vx = 0;
        }

        // Check Z axis
        if (!checkCollision(x, y, nextZ, blocks)) {
            z = nextZ;
        } else {
            vz = 0;
        }

        // Check Y axis
        if (!checkCollision(x, nextY, z, blocks)) {
            y = nextY;
        } else {
            vy = 0;
        }

        // 5. Damping
        vx *= DAMPING;
        vz *= DAMPING;

        // Bounds check (keep in world)
        x = Math.clamp(x, 0, blocks.length - 1f);
        z = Math.clamp(z, 0, blocks[0][0].length - 1f);
        if (y < -10) { // Void kill
            lives = 0;
        }
    }

    private boolean checkCollision(float px, float py, float pz, int[][][] blocks) {
        // Check bounding box corners
        float minX = px - PLAYER_WIDTH / 2;
        float maxX = px + PLAYER_WIDTH / 2;
        float minY = py; // Feet
        float maxY = py + PLAYER_HEIGHT; // Head
        float minZ = pz - PLAYER_WIDTH / 2;
        float maxZ = pz + PLAYER_WIDTH / 2;

        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(maxX);
        int startY = (int) Math.floor(minY);
        int endY = (int) Math.floor(maxY);
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(maxZ);

        for (int ix = startX; ix <= endX; ix++) {
            for (int iy = startY; iy <= endY; iy++) {
                for (int iz = startZ; iz <= endZ; iz++) {
                    if (isSolid(ix, iy, iz, blocks)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSolid(int x, int y, int z, int[][][] blocks) {
        if (x < 0 || x >= blocks.length || z < 0 || z >= blocks[0][0].length)
            return true;
        if (y < 0)
            return true; // Bedrock
        if (y >= blocks[0].length)
            return false;

        int type = blocks[x][y][z];
        return type != BlockConstants.MAT_AIR && type != BlockConstants.MAT_WATER; // Water is non-solid for now?
    }

    private boolean isOnGround(int[][][] blocks) {
        return checkCollision(x, y - 0.1f, z, blocks);
    }

    /**
     * Returns the current state of the player, consisting of their position and remaining lives.
     */
    public PlayerState getState() {
        PlayerState playerState = new PlayerState();
        playerState.setId(connection.getID());
        playerState.setX(x);
        playerState.setY(y);
        playerState.setZ(z);
        playerState.setYaw(yaw);
        playerState.setPitch(pitch);
        playerState.setLives(lives);
        return playerState;
    }

    public void shoot(Direction direction) {
        // Implement 3D shooting later
    }

    public void decreaseLives() {
        if (lives > 0) {
            setLives(getLives() - 1);
        }
    }

    /**
     * Removes the movement and shooting listeners from the player's connection.
     * This should be called when the player disconnects or the game ends.
     * Disposing of the listeners prevents potential thread exceptions when reusing
     * same connections for future game instances.
     */
    public void dispose() {
        connection.removeListener(movementListener);
        connection.removeListener(shootingListener);
    }

}
