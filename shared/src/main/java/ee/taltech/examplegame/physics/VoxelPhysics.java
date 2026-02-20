package ee.taltech.examplegame.physics;

import constant.BlockConstants;
import static constant.Constants.DAMPING;
import static constant.Constants.GRAVITY;
import static constant.Constants.JUMP_VELOCITY;
import static constant.Constants.MOVE_SPEED;
import static constant.Constants.PLAYER_HEIGHT;
import static constant.Constants.PLAYER_WIDTH;
import static constant.Constants.SWIM_UP_SPEED;
import static constant.Constants.WATER_DAMPING;
import static constant.Constants.WATER_GRAVITY;
import static constant.Constants.WATER_LEVEL;
import static constant.Constants.WATER_MOVE_SPEED;
import static constant.Constants.WATER_WAVE_AMPLITUDE;
import static constant.Constants.WATER_WAVE_SPEED;
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
        private float yaw;
        private float pitch;
    }

    public static void update(PhysicsState state, InputState input, float delta, float time, int[][][] blocks) {
        state.setInWater(getWaterDepth(state.getX(), state.getY(), state.getZ(), time, blocks) > 0);

        // 1. Move Input
        float dx = (float) Math.sin(Math.toRadians(input.getYaw()));
        float dz = (float) Math.cos(Math.toRadians(input.getYaw()));
        float speed = state.isInWater() ? WATER_MOVE_SPEED : MOVE_SPEED;

        if (input.getMoveForward() != 0) {
            state.setVx(state.getVx() - dx * input.getMoveForward() * speed * delta);
            state.setVz(state.getVz() - dz * input.getMoveForward() * speed * delta);
        }

        if (input.getMoveSideways() != 0) {
            state.setVx(state.getVx() + dz * input.getMoveSideways() * speed * delta);
            state.setVz(state.getVz() - dx * input.getMoveSideways() * speed * delta);
        }

        if (input.isJump()) {
            if (state.isInWater()) {
                state.setVy(state.getVy() + 20f * delta);
                if (state.getVy() > SWIM_UP_SPEED) state.setVy(SWIM_UP_SPEED);

                if (isTouchingSolidBlock(state.getX(), state.getY(), state.getZ(), blocks)) {
                    state.setVy(0.5f * JUMP_VELOCITY);
                }
            } else if (state.isOnGround()) {
                state.setVy(JUMP_VELOCITY);
            }
        }

        // 2. Physics (Gravity, Collision, Damping, Bounds)
        updatePhysics(state, delta, state.isInWater(), blocks);
    }

    public static void updatePhysics(PhysicsState state, float delta, boolean inWater, int[][][] blocks) {
        // 1. Gravity (reduced in water for slow sinking)
        state.setVy(state.getVy() - (inWater ? WATER_GRAVITY : GRAVITY) * delta);

        float nextX = state.getX() + state.getVx() * delta;
        float nextY = state.getY() + state.getVy() * delta;
        float nextZ = state.getZ() + state.getVz() * delta;

        // X axis
        if (checkCollision(nextX, state.getY(), state.getZ(), blocks)) {
            state.setVx(0);
        } else {
            state.setX(nextX);
        }

        // Z axis
        if (checkCollision(state.getX(), state.getY(), nextZ, blocks)) {
            state.setVz(0);
        } else {
            state.setZ(nextZ);
        }

        // Y axis
        if (checkCollision(state.getX(), nextY, state.getZ(), blocks)) {
            if (state.getVy() < 0) state.setOnGround(true);
            state.setVy(0);
        } else {
            state.setY(nextY);
            state.setOnGround(false);
        }

        // Damping (stronger in water)
        float damp = inWater ? WATER_DAMPING : DAMPING;
        state.setVx(state.getVx() * damp);
        state.setVz(state.getVz() * damp);
        if (inWater) {
            state.setVy(state.getVy() * 0.92f); // vertical drag in water
        }

        // Bounds check
        if (blocks != null) {
            state.setX(Math.clamp(state.getX(), 0, blocks.length - 1f));
            state.setZ(Math.clamp(state.getZ(), 0, blocks[0][0].length - 1f));
        }
    }

    public static boolean checkCollision(float px, float py, float pz, int[][][] blocks) {
        if (blocks == null) return false;

        float minX = px - PLAYER_WIDTH / 2;
        float maxX = px + PLAYER_WIDTH / 2;
        float minY = py;
        float maxY = py + PLAYER_HEIGHT;
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

    public static boolean isTouchingSolidBlock(float px, float py, float pz, int[][][] blocks) {
        if (blocks == null) return false;

        float padding = 0.1f;
        float checkRadius = PLAYER_WIDTH / 2 + padding;

        return (isSolid((int)(px + checkRadius), (int)py, (int)pz, blocks)
            || isSolid((int)(px - checkRadius), (int)py, (int)pz, blocks)
            || isSolid((int)px, (int)py, (int)(pz + checkRadius), blocks)
            || isSolid((int)px, (int)py, (int)(pz - checkRadius), blocks));
    }

    public static boolean isSolid(int x, int y, int z, int[][][] blocks) {
        if (blocks == null) return false;
        if (x < 0 || x >= blocks.length || z < 0 || z >= blocks[0][0].length) return true;
        if (y < 0) return true;
        if (y >= blocks[0].length) return false;

        int type = blocks[x][y][z];
        return type != BlockConstants.MAT_AIR && type != BlockConstants.MAT_WATER;
    }

    public static float computeWaterSurfaceY(float time, float x, float z) {
        float t = time * WATER_WAVE_SPEED;
        float w1 = (float) Math.sin(x * 1.8f + t) * WATER_WAVE_AMPLITUDE;
        float w2 = (float) Math.sin(z * 2.3f + t * 0.7f + 1.3f) * WATER_WAVE_AMPLITUDE * 0.5f;
        float w3 = (float) Math.sin((x + z) * 3.7f + t * 1.13f + 2.7f) * WATER_WAVE_AMPLITUDE * 0.3f;

        return WATER_LEVEL - 0.12f + w1 + w2 + w3;
    }

    public static float getWaterDepth(float px, float py, float pz, float time, int[][][] blocks) {
        if (blocks == null) return 0f;

        int bx = (int) Math.floor(px);
        int by = (int) Math.floor(py); // feet level
        int bz = (int) Math.floor(pz);

        if (bx < 0 || bx >= blocks.length || by < 0 || by >= blocks[0].length || bz < 0 || bz >= blocks[0][0].length)
            return 0f;

        if (blocks[bx][by][bz] != BlockConstants.MAT_WATER) return 0f;

        float surfaceY = computeWaterSurfaceY(time, px, pz);

        return Math.max(0f, surfaceY - py);
    }

    public static boolean isUnderwater(float eyeX, float eyeY, float eyeZ, float time, int[][][] blocks) {
        if (blocks == null) return false;

        int bx = (int) Math.floor(eyeX);
        int by = (int) Math.floor(eyeY);
        int bz = (int) Math.floor(eyeZ);

        if (bx < 0 || bx >= blocks.length || by < 0 || by >= blocks[0].length || bz < 0 || bz >= blocks[0][0].length)
            return false;

        if (blocks[bx][by][bz] == BlockConstants.MAT_WATER) {
            float surfaceY = computeWaterSurfaceY(time, eyeX, eyeZ);
            return eyeY < surfaceY;
        }
        return false;
    }
}
