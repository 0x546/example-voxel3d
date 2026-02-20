package ee.taltech.examplegame.screen;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;

import constant.BlockConstants;
import static constant.Constants.CAMERA_FAR;
import static constant.Constants.CAMERA_FOV;
import static constant.Constants.CAMERA_NEAR;
import static constant.Constants.DAMPING;
import static constant.Constants.EYE_HEIGHT;
import static constant.Constants.GRAVITY;
import static constant.Constants.JUMP_VELOCITY;
import static constant.Constants.MOUSE_SENSITIVITY;
import static constant.Constants.MOVE_SPEED;
import static constant.Constants.PLAYER_HEIGHT;
import static constant.Constants.PLAYER_INTERPOLATION_SPEED;
import static constant.Constants.PLAYER_SNAP_DISTANCE;
import static constant.Constants.PLAYER_WIDTH;
import static constant.Constants.SWIM_UP_SPEED;
import static constant.Constants.WATER_DAMPING;
import static constant.Constants.WATER_GRAVITY;
import static constant.Constants.WATER_LEVEL;
import static constant.Constants.WATER_MOVE_SPEED;
import static constant.Constants.WATER_WAVE_AMPLITUDE;
import static constant.Constants.WATER_WAVE_SPEED;
import ee.taltech.examplegame.game.GameStateManager;
import ee.taltech.examplegame.game.PlayerInputManager;
import ee.taltech.examplegame.game.ProceduralVoxelWorld;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.screen.overlay.PauseOverlay;
import ee.taltech.examplegame.screen.overlay.VoxelHud;
import ee.taltech.examplegame.util.PlayerModelGenerator;
import message.dto.PlayerState;

public class VoxelScreen extends ScreenAdapter {

    private final PerspectiveCamera camera;
    private final Environment environment;
    private final ProceduralVoxelWorld voxelWorld;
    private final VoxelHud hud;
    private final PauseOverlay pauseOverlay;
    private boolean paused = false;
    private boolean underwater = false;

    private final GameStateManager gameStateManager;
    private final PlayerInputManager inputManager;
    private final ModelBatch modelBatch;
    private final Model playerModel;
    private final ModelInstance reusablePlayerInstance;
    private final Map<Integer, Vector3> playerPositions = new HashMap<>();

    private float pitch = 0;
    private float yaw = 0;

    // Physics state
    private float vx;
    private float vy;
    private float vz;
    private boolean onGround = false;

    public VoxelScreen(Game game) {

        camera = new PerspectiveCamera(CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 20, 12);
        camera.lookAt(0, 0, 0);
        camera.near = CAMERA_NEAR;
        camera.far = CAMERA_FAR;
        camera.update();

        yaw = -135f; // Initial direction
        pitch = -30f;

        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        voxelWorld = new ProceduralVoxelWorld();
        hud = new VoxelHud();

        gameStateManager = new GameStateManager();
        inputManager = new PlayerInputManager();
        modelBatch = new ModelBatch();

        playerModel = PlayerModelGenerator.createPlayerModel();
        reusablePlayerInstance = new ModelInstance(playerModel);

        pauseOverlay = new PauseOverlay(() -> {
            paused = false;
            Gdx.input.setCursorCatched(true);
            Gdx.input.setInputProcessor(null);
        }, () -> {
            this.dispose();
            game.setScreen(new TitleScreen(game));
        });
    }

    @Override
    public void render(float delta) {
        // Always update input state (so ESC/clicks are caught)
        inputManager.updateNoSend(paused);

        processPauseToggle();

        if (!paused) {
            // Local-only updates
            updateCamera();         // handles mouse-driven yaw/pitch & camera.direction
            handleInput(delta);     // keyboard -> vx/vy/vz / jump
            updatePhysics(delta);   // apply physics and update camera position
            inputManager.update(yaw, pitch); // send to server / networked state
            if (inputManager.isActionPressed()) {
                Gdx.input.setCursorCatched(true);
            }
        } else {
            // keep camera moving with viewport changes even when paused
            camera.update();
        }

        // Keep local player in sync with server authority
        synchronizeLocalPlayerWithServer(delta);

        // Detect underwater state
        underwater = isUnderwater();

        // --- Rendering ---
        clearScreen();

        voxelWorld.render(camera, underwater);

        renderOtherPlayers(delta);

        hud.render();

        if (paused) {
            pauseOverlay.render(delta);
        }
    }

    // -------------------------
    // Helper: pause / input
    // -------------------------
    private void processPauseToggle() {
        if (inputManager.isPausePressed()) {
            if (paused) {
                paused = false;
                Gdx.input.setCursorCatched(true);
                Gdx.input.setInputProcessor(null);
            } else {
                paused = true;
                Gdx.input.setCursorCatched(false);
                Gdx.input.setInputProcessor(pauseOverlay.getStage());
                inputManager.stopMovement(yaw, pitch); // notify server to stop player movement
            }
        }
    }

    // -------------------------
    // Helper: camera & input
    // -------------------------
    private void updateCamera() {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
            float deltaY = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;

            yaw += deltaX;
            pitch += deltaY;

            // Clamp pitch to avoid flipping
            if (pitch > 89f) pitch = 89f;
            if (pitch < -89f) pitch = -89f;

            camera.direction.set(0, 0, -1);
            camera.direction.rotate(Vector3.Y, yaw);

            Vector3 side = camera.direction.cpy().crs(Vector3.Y).nor();
            camera.direction.rotate(side, pitch);

            camera.up.set(0, 1, 0);
            camera.update();
        }
    }

    private void handleInput(float delta) {
        float dx = (float) Math.sin(Math.toRadians(yaw));
        float dz = (float) Math.cos(Math.toRadians(yaw));

        float f = inputManager.getMoveForward();
        float s = inputManager.getMoveSideways();

        float depth = getWaterDepth();
        boolean inWater = depth > 0;
        float speed = inWater ? WATER_MOVE_SPEED : MOVE_SPEED;

        if (f != 0) {
            vx -= dx * f * speed * delta;
            vz -= dz * f * speed * delta;
        }

        if (s != 0) {
            // Strafing
            vx += dz * s * speed * delta;
            vz -= dx * s * speed * delta;
        }

        if (inputManager.isJump()) {
            if (inWater) {
                vy += 20f * delta;
                if (vy > SWIM_UP_SPEED) vy = SWIM_UP_SPEED;

                if (isTouchingSolidBlock()) {
                    vy = 0.5f * JUMP_VELOCITY;
                }
            } else if (onGround) {
                vy = JUMP_VELOCITY;
            }
        }
    }

    // -------------------------
    // Helper: physics + collision
    // -------------------------
    private void updatePhysics(float delta) {
        float depth = getWaterDepth();
        boolean inWater = depth > 0;

        // 1. Gravity (reduced in water for slow sinking)
        vy -= (inWater ? WATER_GRAVITY : GRAVITY) * delta;

        // Current feet position
        float x = camera.position.x;
        float y = camera.position.y - EYE_HEIGHT;
        float z = camera.position.z;

        float nextX = x + vx * delta;
        float nextY = y + vy * delta;
        float nextZ = z + vz * delta;

        int[][][] blocks = voxelWorld.getBlocks();

        // X axis
        if (checkCollision(nextX, y, z, blocks)) {
            vx = 0;
        } else {
            x = nextX;
        }

        // Z axis
        if (checkCollision(x, y, nextZ, blocks)) {
            vz = 0;
        } else {
            z = nextZ;
        }

        // Y axis
        if (checkCollision(x, nextY, z, blocks)) {
            if (vy < 0) onGround = true;
            vy = 0;
        } else {
            y = nextY;
            onGround = false;
        }

        // Damping (stronger in water)
        float damp = inWater ? WATER_DAMPING : DAMPING;
        vx *= damp;
        vz *= damp;
        if (inWater) {
            vy *= 0.92f; // vertical drag in water
        }

        // Bounds check
        if (blocks != null) {
            x = Math.clamp(x, 0, blocks.length - 1f);
            z = Math.clamp(z, 0, blocks[0][0].length - 1f);
        }

        // Void / respawn (client-side visual)
        if (y < -10) {
            y = 30;
            vy = 0;
        }

        // Update camera to follow feet + eye offset
        camera.position.set(x, y + EYE_HEIGHT, z);
        camera.update();
    }

    private boolean checkCollision(float px, float py, float pz, int[][][] blocks) {
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

    private boolean isTouchingSolidBlock() {
        int[][][] blocks = voxelWorld.getBlocks();
        if (blocks == null) return false;

        float x = camera.position.x;
        float y = camera.position.y - EYE_HEIGHT;
        float z = camera.position.z;

        float padding = 0.1f;
        float checkRadius = PLAYER_WIDTH / 2 + padding;

        return (isSolid((int)(x + checkRadius), (int)y, (int)z, blocks)
            || isSolid((int)(x - checkRadius), (int)y, (int)z, blocks)
            || isSolid((int)x, (int)y, (int)(z + checkRadius), blocks)
            || isSolid((int)x, (int)y, (int)(z - checkRadius), blocks));
    }

    private boolean isSolid(int x, int y, int z, int[][][] blocks) {
        if (x < 0 || x >= blocks.length || z < 0 || z >= blocks[0][0].length) return true;
        if (y < 0) return true;
        if (y >= blocks[0].length) return false;

        int type = blocks[x][y][z];
        return type != 0 && type != 6; // 0=Air, 6=Water
    }

    // -------------------------
    // Helper: server synchronization for local player
    // -------------------------
    private void synchronizeLocalPlayerWithServer(float delta) {
        var latestMsg = gameStateManager.getLatestGameStateMessage();
        if (latestMsg == null) return;

        var playerStates = latestMsg.getPlayerStates();
        if (playerStates == null) return;

        int myId = ServerConnection.getInstance().getClient().getID();

        for (var state : playerStates) {
            if (state.getId() != myId) continue;

            float dx = state.getX() - camera.position.x;
            float dy = (state.getY() + EYE_HEIGHT) - camera.position.y;
            float dz = state.getZ() - camera.position.z;
            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > PLAYER_SNAP_DISTANCE * PLAYER_SNAP_DISTANCE) {
                // Snap to authoritative server position
                camera.position.set(state.getX(), state.getY() + EYE_HEIGHT, state.getZ());
                vx = state.getVx();
                vy = state.getVy();
                vz = state.getVz();
                camera.update();
            } else if (distSq > 0.01f) {
                // Soft correction
                camera.position.lerp(new Vector3(state.getX(), state.getY() + EYE_HEIGHT, state.getZ()), 5f * delta);
                camera.update();
            }
        }
    }

    // -------------------------
    // Helper: underwater detection
    // -------------------------
    private boolean isUnderwater() {
        float cx = camera.position.x;
        float cy = camera.position.y;
        float cz = camera.position.z;

        if (isWaterBlock(cx, cy, cz)) {
            float surfaceY = computeWaterSurfaceY(cx, cz);
            return cy < surfaceY;
        }
        return false;
    }

    private float getWaterDepth() {
        float cx = camera.position.x;
        float fy = camera.position.y - EYE_HEIGHT;
        float cz = camera.position.z;

        if (isWaterBlock(cx, fy, cz)) {
            float surfaceY = computeWaterSurfaceY(cx, cz);
            return Math.max(0f, surfaceY - fy);
        }
        return 0f;
    }

    private boolean isWaterBlock(float x, float y, float z) {
        int[][][] blocks = voxelWorld.getBlocks();
        if (blocks == null) return false;

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        if (bx < 0 || bx >= blocks.length ||
            by < 0 || by >= blocks[0].length ||
            bz < 0 || bz >= blocks[0][0].length)
            return false;

        return blocks[bx][by][bz] == BlockConstants.MAT_WATER;
    }

    private float computeWaterSurfaceY(float x, float z) {
        float time = voxelWorld.getTime();
        float t = time * WATER_WAVE_SPEED;

        float w1 = (float) Math.sin(x * 1.8f + t) * WATER_WAVE_AMPLITUDE;
        float w2 = (float) Math.sin(z * 2.3f + t * 0.7f + 1.3f) * WATER_WAVE_AMPLITUDE * 0.5f;
        float w3 = (float) Math.sin((x + z) * 3.7f + t * 1.13f + 2.7f) * WATER_WAVE_AMPLITUDE * 0.3f;

        return WATER_LEVEL - 0.12f + w1 + w2 + w3;
    }

    // -------------------------
    // Helper: rendering
    // -------------------------
    private void clearScreen() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (underwater) {
            Gdx.gl.glClearColor(0.02f, 0.06f, 0.14f, 1f); // dark underwater fog
        } else {
            Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f); // sky blue
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    private void renderOtherPlayers(float delta) {
        var latestMsg = gameStateManager.getLatestGameStateMessage();
        if (latestMsg == null) return;
        var playerStates = latestMsg.getPlayerStates();
        if (playerStates == null) return;

        int myId = ServerConnection.getInstance().getClient().getID();

        modelBatch.begin(camera);
        for (var state : playerStates) {
            if (state.getId() == myId) continue;

            renderRemotePlayer(state, delta);
        }
        modelBatch.end();
    }

    /**
     * Render a single remote player. Creates a fresh ModelInstance from the base model
     * so node rotations / transforms do not interfere between players.
     */
    private void renderRemotePlayer(PlayerState state, float delta) {
        Vector3 targetPos = new Vector3(state.getX(), state.getY(), state.getZ());

        playerPositions.putIfAbsent(state.getId(), targetPos.cpy());
        Vector3 currentPos = playerPositions.get(state.getId());
        currentPos.lerp(targetPos, PLAYER_INTERPOLATION_SPEED * delta);

        reusablePlayerInstance.transform.setToTranslation(currentPos);
        reusablePlayerInstance.transform.rotate(Vector3.Y, state.getYaw());

        var headNode = reusablePlayerInstance.getNode("head");
        if (headNode != null) {
            headNode.rotation.setEulerAngles(0, state.getPitch(), 0);
            reusablePlayerInstance.calculateTransforms();
        }

        modelBatch.render(reusablePlayerInstance, environment);
    }

    // -------------------------
    // Lifecycle
    // -------------------------
    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        pauseOverlay.resize(width, height);
    }

    @Override
    public void dispose() {
        voxelWorld.dispose();
        hud.dispose();
        pauseOverlay.dispose();
        modelBatch.dispose();
        if (playerModel != null) playerModel.dispose();
    }
}
