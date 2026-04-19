package ee.taltech.examplegame.shared.physics;

import ee.taltech.examplegame.shared.constant.BlockConstants;
import static ee.taltech.examplegame.shared.constant.Constants.DAMPING;
import static ee.taltech.examplegame.shared.constant.Constants.GRAVITY;
import static ee.taltech.examplegame.shared.constant.Constants.JUMP_VELOCITY;
import static ee.taltech.examplegame.shared.constant.Constants.MOVE_SPEED;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_HEIGHT;
import static ee.taltech.examplegame.shared.constant.Constants.PLAYER_WIDTH;
import static ee.taltech.examplegame.shared.constant.Constants.SWIM_UP_SPEED;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_DAMPING;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_GRAVITY;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_LEVEL;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_MOVE_SPEED;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_WAVE_AMPLITUDE;
import static ee.taltech.examplegame.shared.constant.Constants.WATER_WAVE_SPEED;
import ee.taltech.examplegame.shared.world.World;
import lombok.Getter;
import lombok.Setter;

public class VoxelPhysics {
    private VoxelPhysics() {
        /* This utility class should not be instantiated */
    }

    public static class PhysicsState {
        @Getter @Setter private float x;
        @Getter @Setter private float y;
        @Getter @Setter private float z;
        @Getter @Setter private float vx;
        @Getter @Setter private float vy;
        @Getter @Setter private float vz;
        @Getter @Setter private boolean onGround;
        @Getter @Setter private boolean inWater;
    }

    @Getter @Setter
    public static class InputState {
        private float moveForward;
        private float moveSideways;
        private boolean jump;
        private boolean sneak;
        private boolean fly;
        private float yaw;
        private float pitch;
    }

    public static void update(PhysicsState state, InputState input, float delta, float time, World world) {
        state.setInWater(getWaterDepth(state.getX(), state.getY(), state.getZ(), time, world) > 0);

        if (input.isFly()) {
            handleFlyMovement(state, input);
        } else {
            handleWalkMovement(state, input, delta, world);
        }

        // 2. Physics (Gravity, Collision, Damping, Bounds)
        applyPhysics(state, delta, input.isFly(), world);
    }

    private static void handleFlyMovement(PhysicsState state, InputState input) {
        float flySpeed = MOVE_SPEED * 1.5f;
        float moveX = 0;
        float moveZ = 0;

        float dx = (float) Math.sin(Math.toRadians(input.getYaw()));
        float dz = (float) Math.cos(Math.toRadians(input.getYaw()));

        if (input.getMoveForward() != 0) {
            moveX -= dx * input.getMoveForward();
            moveZ -= dz * input.getMoveForward();
        }
        if (input.getMoveSideways() != 0) {
            moveX += dz * input.getMoveSideways();
            moveZ -= dx * input.getMoveSideways();
        }

        if (moveX != 0 || moveZ != 0) {
            float len = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            state.setVx((moveX / len) * flySpeed);
            state.setVz((moveZ / len) * flySpeed);
        } else {
            state.setVx(0);
            state.setVz(0);
        }

        if (input.isJump()) {
            state.setVy(flySpeed);
        } else if (input.isSneak()) {
            state.setVy(-flySpeed);
        } else {
            state.setVy(0); // Hover
        }
    }

    private static void handleWalkMovement(PhysicsState state, InputState input, float delta, World world) {
        float speed = state.isInWater() ? WATER_MOVE_SPEED : MOVE_SPEED;
        float dx = (float) Math.sin(Math.toRadians(input.getYaw()));
        float dz = (float) Math.cos(Math.toRadians(input.getYaw()));

        if (input.getMoveForward() != 0) {
            state.setVx(state.getVx() - dx * input.getMoveForward() * speed * delta);
            state.setVz(state.getVz() - dz * input.getMoveForward() * speed * delta);
        }
        if (input.getMoveSideways() != 0) {
            state.setVx(state.getVx() + dz * input.getMoveSideways() * speed * delta);
            state.setVz(state.getVz() - dx * input.getMoveSideways() * speed * delta);
        }

        if (input.isJump()) {
            handleJump(state, delta, world);
        }
    }

    private static void handleJump(PhysicsState state, float delta, World world) {
        if (state.isInWater()) {
            state.setVy(Math.min(state.getVy() + 20f * delta, SWIM_UP_SPEED));
            if (isTouchingSolidBlock(state.getX(), state.getY(), state.getZ(), world)) {
                state.setVy(0.5f * JUMP_VELOCITY);
            }
        } else if (state.isOnGround()) {
            state.setVy(JUMP_VELOCITY);
        }
    }

    private static void applyPhysics(PhysicsState state, float delta, boolean flying, World world) {
        boolean inWater = state.isInWater();

        if (!flying) {
            state.setVy(state.getVy() - (inWater ? WATER_GRAVITY : GRAVITY) * delta);
        }

        applyCollisionAndMove(state, delta, world);
        applyDamping(state, inWater);
    }

    private static void applyCollisionAndMove(PhysicsState state, float delta, World world) {
        float nextX = state.getX() + state.getVx() * delta;
        float nextY = state.getY() + state.getVy() * delta;
        float nextZ = state.getZ() + state.getVz() * delta;

        if (checkCollision(nextX, state.getY(), state.getZ(), world)) {
            state.setVx(0);
        } else {
            state.setX(nextX);
        }

        if (checkCollision(state.getX(), state.getY(), nextZ, world)) {
            state.setVz(0);
        } else {
            state.setZ(nextZ);
        }

        if (checkCollision(state.getX(), nextY, state.getZ(), world)) {
            if (state.getVy() < 0) state.setOnGround(true);
            state.setVy(0);
        } else {
            state.setY(nextY);
            state.setOnGround(false);
        }
    }

    private static void applyDamping(PhysicsState state, boolean inWater) {
        float damp = inWater ? WATER_DAMPING : DAMPING;
        state.setVx(state.getVx() * damp);
        state.setVz(state.getVz() * damp);
        if (inWater) {
            state.setVy(state.getVy() * 0.92f); // vertical drag in water
        }
    }

    public static boolean checkCollision(float px, float py, float pz, World world) {
        if (world == null) return false;

        float minX = px - PLAYER_WIDTH / 2;
        float maxX = px + PLAYER_WIDTH / 2;
        float maxY = py + PLAYER_HEIGHT;
        float minZ = pz - PLAYER_WIDTH / 2;
        float maxZ = pz + PLAYER_WIDTH / 2;

        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(maxX);
        int startY = (int) Math.floor(py);
        int endY = (int) Math.floor(maxY);
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(maxZ);

        for (int ix = startX; ix <= endX; ix++) {
            for (int iy = startY; iy <= endY; iy++) {
                for (int iz = startZ; iz <= endZ; iz++) {
                    if (isSolid(ix, iy, iz, world)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isTouchingSolidBlock(float px, float py, float pz, World world) {
        if (world == null) return false;

        float padding = 0.1f;
        float checkRadius = PLAYER_WIDTH / 2 + padding;

        return (isSolid((int)(px + checkRadius), (int)py, (int)pz, world)
            || isSolid((int)(px - checkRadius), (int)py, (int)pz, world)
            || isSolid((int)px, (int)py, (int)(pz + checkRadius), world)
            || isSolid((int)px, (int)py, (int)(pz - checkRadius), world));
    }

    public static boolean isSolid(int x, int y, int z, World world) {
        if (world == null) return false;
        if (y < 0) return true;

        int type = world.getBlock(x, y, z);
        if (type == -1) return true; // Unloaded chunks are solid barriers properly stopping the player

        return type != BlockConstants.MAT_AIR && type != BlockConstants.MAT_WATER;
    }

    public static float computeWaterSurfaceY(float time, float x, float z) {
        float t = time * WATER_WAVE_SPEED;
        float w1 = (float) Math.sin(x * 1.8f + t) * WATER_WAVE_AMPLITUDE;
        float w2 = (float) Math.sin(z * 2.3f + t * 0.7f + 1.3f) * WATER_WAVE_AMPLITUDE * 0.5f;
        float w3 = (float) Math.sin((x + z) * 3.7f + t * 1.13f + 2.7f) * WATER_WAVE_AMPLITUDE * 0.3f;

        return WATER_LEVEL - 0.12f + w1 + w2 + w3;
    }

    public static float getWaterDepth(float px, float py, float pz, float time, World world) {
        if (world == null) return 0f;

        int bx = (int) Math.floor(px);
        int by = (int) Math.floor(py); // feet level
        int bz = (int) Math.floor(pz);

        int type = world.getBlock(bx, by, bz);
        if (type != BlockConstants.MAT_WATER) return 0f;

        float surfaceY = computeWaterSurfaceY(time, px, pz);

        return Math.max(0f, surfaceY - py);
    }

    public static boolean isUnderwater(float eyeX, float eyeY, float eyeZ, float time, World world) {
        if (world == null) return false;

        int bx = (int) Math.floor(eyeX);
        int by = (int) Math.floor(eyeY);
        int bz = (int) Math.floor(eyeZ);

        if (world.getBlock(bx, by, bz) == BlockConstants.MAT_WATER) {
            float surfaceY = computeWaterSurfaceY(time, eyeX, eyeZ);
            return eyeY < surfaceY;
        }
        return false;
    }
}
