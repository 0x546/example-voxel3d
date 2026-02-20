package ee.taltech.examplegame.server.game.object;

import com.esotericsoftware.kryonet.Connection;

import constant.BlockConstants;
import static constant.Constants.DAMPING;
import static constant.Constants.GRAVITY;
import static constant.Constants.JUMP_VELOCITY;
import static constant.Constants.MOVE_SPEED;
import static constant.Constants.PLAYER_HEIGHT;
import static constant.Constants.PLAYER_LIVES_COUNT;
import static constant.Constants.PLAYER_SPAWN_X;
import static constant.Constants.PLAYER_SPAWN_Y;
import static constant.Constants.PLAYER_SPAWN_Z;
import static constant.Constants.PLAYER_WIDTH;
import static constant.Constants.SWIM_UP_SPEED;
import static constant.Constants.VOID_LEVEL;
import static constant.Constants.WATER_DAMPING;
import static constant.Constants.WATER_GRAVITY;
import static constant.Constants.WATER_LEVEL;
import static constant.Constants.WATER_MOVE_SPEED;
import static constant.Constants.WATER_WAVE_AMPLITUDE;
import static constant.Constants.WATER_WAVE_SPEED;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.listener.PlayerMovementListener;
import lombok.Getter;
import lombok.Setter;
import message.PlayerMovementMessage;
import message.dto.PlayerState;

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
    private float moveForward;
    private float moveSideways;
    private boolean jump;
    private boolean sneak;

    public Player(Connection connection, GameInstance game) {
        this.connection = connection;
        this.id = connection.getID();
        this.game = game;
        this.connection.addListener(movementListener);

        // Spawn point
        this.x = PLAYER_SPAWN_X;
        this.y = PLAYER_SPAWN_Y;
        this.z = PLAYER_SPAWN_Z;
    }

    public void handleInput(PlayerMovementMessage message) {
        this.moveForward = message.getMoveForward();
        this.moveSideways = message.getMoveSideways();
        this.jump = message.isJump();
        this.sneak = message.isSneak();
        this.yaw = message.getYaw();
        this.pitch = message.getPitch();
    }

    public void update(float delta, int[][][] blocks, float time) {
        float depth = getWaterDepth(blocks, time);
        boolean inWater = depth > 0;

        // 1. Apply Input Forces
        float dx = (float) Math.sin(Math.toRadians(yaw));
        float dz = (float) Math.cos(Math.toRadians(yaw));
        float speed = inWater ? WATER_MOVE_SPEED : MOVE_SPEED;

        // Primary Movement axes
        if (moveForward != 0) {
            vx -= dx * moveForward * speed * delta;
            vz -= dz * moveForward * speed * delta;
        }

        // Strafing axes
        if (moveSideways != 0) {
            vx += dz * moveSideways * speed * delta;
            vz -= dx * moveSideways * speed * delta;
        }

        // Jump / Swim
        if (jump) {
            if (inWater) {
                vy += 20f * delta;
                if (vy > SWIM_UP_SPEED) vy = SWIM_UP_SPEED;

                if (isTouchingSolidBlock(blocks)) {
                    vy = 0.5f * JUMP_VELOCITY;
                }

            } else if (isOnGround(blocks)) {
                vy = JUMP_VELOCITY;
            }
        }

        // 2. Apply Gravity (reduced in water)
        vy -= (inWater ? WATER_GRAVITY : GRAVITY) * delta;

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

        // 5. Damping (stronger in water)
        float damp = inWater ? WATER_DAMPING : DAMPING;
        vx *= damp;
        vz *= damp;
        if (inWater) {
            vy *= 0.92f; // vertical drag in water
        }

        // Bounds check (keep in world)
        x = Math.clamp(x, 0, blocks.length - 1f);
        z = Math.clamp(z, 0, blocks[0][0].length - 1f);
        if (y < VOID_LEVEL) { // Void kill
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

    /**
     * Checks if the player is touching any solid block horizontally.
     * Used to allow jumping out of water onto land.
     */
    private boolean isTouchingSolidBlock(int[][][] blocks) {
        float padding = 0.1f; // Slight buffer outside the hitbox
        float checkRadius = PLAYER_WIDTH / 2 + padding;

        // Check 4 points around the player at feet level
        return (isSolid((int)(x + checkRadius), (int)y, (int)z, blocks)
            || isSolid((int)(x - checkRadius), (int)y, (int)z, blocks)
            || isSolid((int)x, (int)y, (int)(z + checkRadius), blocks)
            || isSolid((int)x, (int)y, (int)(z - checkRadius), blocks));
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
     * Returns how deep the player's feet are in water (0 if above water).
     */
    private float getWaterDepth(int[][][] blocks, float time) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y); // feet level
        int bz = (int) Math.floor(z);

        if (bx < 0 || bx >= blocks.length || by < 0 || by >= blocks[0].length || bz < 0 || bz >= blocks[0][0].length)
            return 0;

        // First check if the block at feet position is water
        if (blocks[bx][by][bz] != BlockConstants.MAT_WATER) return 0;

        // More precise check against animated waves (matching the water shader)
        float surfaceY = computeWaterSurfaceY(time, x, z);

        return Math.max(0, surfaceY - y);
    }

    private float computeWaterSurfaceY(float time, float x, float z) {
        float t = time * WATER_WAVE_SPEED;
        float w1 = (float) Math.sin(x * 1.8f + t) * WATER_WAVE_AMPLITUDE;
        float w2 = (float) Math.sin(z * 2.3f + t * 0.7f + 1.3f) * WATER_WAVE_AMPLITUDE * 0.5f;
        float w3 = (float) Math.sin((x + z) * 3.7f + t * 1.13f + 2.7f) * WATER_WAVE_AMPLITUDE * 0.3f;

        return WATER_LEVEL - 0.12f + w1 + w2 + w3;
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
        playerState.setVx(vx);
        playerState.setVy(vy);
        playerState.setVz(vz);
        playerState.setYaw(yaw);
        playerState.setPitch(pitch);
        playerState.setLives(lives);
        return playerState;
    }

    public void shoot() {
        // TODO: Implement 3D shooting using yaw/pitch
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
    }
}
