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

import static constant.Constants.CAMERA_FAR;
import static constant.Constants.CAMERA_FOV;
import static constant.Constants.CAMERA_NEAR;
import static constant.Constants.EYE_HEIGHT;
import static constant.Constants.MOUSE_SENSITIVITY;
import static constant.Constants.PLAYER_INTERPOLATION_SPEED;
import static constant.Constants.PLAYER_SNAP_DISTANCE;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.examplegame.game.GameStateManager;
import ee.taltech.examplegame.game.PlayerInputManager;
import ee.taltech.examplegame.game.ProceduralVoxelWorld;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.physics.VoxelPhysics;
import ee.taltech.examplegame.screen.overlay.PauseOverlay;
import ee.taltech.examplegame.screen.overlay.VoxelHud;
import ee.taltech.examplegame.util.PlayerModelGenerator;
import message.dto.PlayerState;
import message.ChunkRequestMessage;
import message.ChunkDataMessage;
import ee.taltech.examplegame.shared.world.Chunk;

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
    private final Map<Integer, ModelInstance> playerInstances = new HashMap<>();
    private final Map<Integer, Vector3> playerPositions = new HashMap<>();

    private float pitch;
    private float yaw;

    private final VoxelPhysics.PhysicsState physicsState = new VoxelPhysics.PhysicsState();
    private final VoxelPhysics.InputState inputState = new VoxelPhysics.InputState();

    private float animationTimer = 0;

    private float chunkRequestTimer = 0;

    private final Listener networkListener;

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

        networkListener = new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof ChunkDataMessage cDM) {
                    Gdx.app.postRunnable(() -> voxelWorld.addChunk(cDM.getChunk()));
                }
            }
        };
        ServerConnection.getInstance().getClient().addListener(networkListener);

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
        animationTimer += delta;
        // Always update input state (so ESC/clicks are caught)
        inputManager.updateNoSend(paused);

        processPauseToggle();

        if (!paused) {
            // Local-only updates
            updateCamera();         // handles mouse-driven yaw/pitch & camera.direction
            updatePhysics(delta);   // handles movement input, physics, and camera position
            inputManager.update(yaw, pitch); // send to server / networked state
            if (inputManager.isActionPressed()) {
                Gdx.input.setCursorCatched(true);
            }
        } else {
            // keep camera moving with viewport changes even when paused
            camera.update();
        }

        // Keep local player in sync with server authority
        synchronizeLocalPlayerWithServer();

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

        handleReceivedMessages();

        // request chunks
        chunkRequestTimer += delta;
        if (chunkRequestTimer >= 0.5f) {
            chunkRequestTimer = 0;
            requestNeededChunks();
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
            camera.direction.rotate(Vector3.Y, inputState.getYaw());

            Vector3 side = camera.direction.cpy().crs(Vector3.Y).nor();
            camera.direction.rotate(side, inputState.getPitch());

            camera.up.set(0, 1, 0);
            camera.update();
        }
    }

    private void updatePhysics(float delta) {
        // Position and velocity are kept in physicsState directly
        physicsState.setX(camera.position.x);
        physicsState.setY(camera.position.y - EYE_HEIGHT);
        physicsState.setZ(camera.position.z);

        inputState.setMoveForward(inputManager.getMoveForward());
        inputState.setMoveSideways(inputManager.getMoveSideways());
        inputState.setJump(inputManager.isJump());
        inputState.setSneak(inputManager.isSneak());
        inputState.setFly(inputManager.isFly());
        inputState.setYaw(yaw);
        inputState.setPitch(pitch);

        VoxelPhysics.update(physicsState, inputState, delta, voxelWorld.getTime(), voxelWorld.getWorld());

        float resY = physicsState.getY();

        // Void / respawn (client-side visual)
        if (resY < -10) {
            resY = 30;
            physicsState.setVy(0);
        }

        // Update camera
        camera.position.set(physicsState.getX(), resY + EYE_HEIGHT, physicsState.getZ());
        camera.update();
    }

    // -------------------------
    // Helper: server synchronization for local player
    // -------------------------
    private void synchronizeLocalPlayerWithServer() {
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
                physicsState.setVx(state.getVx());
                physicsState.setVy(state.getVy());
                physicsState.setVz(state.getVz());
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

        return VoxelPhysics.isUnderwater(cx, cy, cz, voxelWorld.getTime(), voxelWorld.getWorld());
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

        float distanceMoved = currentPos.dst(targetPos);
        currentPos.lerp(targetPos, PLAYER_INTERPOLATION_SPEED * delta);

        playerInstances.putIfAbsent(state.getId(), new ModelInstance(playerModel));
        ModelInstance instance = playerInstances.get(state.getId());

        // 1. Root transform (Model Center is at Y=1.1 relative to feet)
        instance.transform.setToTranslation(currentPos.x, currentPos.y + 1.1f, currentPos.z);
        instance.transform.rotate(Vector3.Y, state.getYaw());

        boolean isMoving = distanceMoved > 0.005f;
        boolean remoteUnderwater = VoxelPhysics.isUnderwater(
            currentPos.x,
            currentPos.y + EYE_HEIGHT,
            currentPos.z,
            voxelWorld.getTime(),
            voxelWorld.getWorld()
        );

        // 2. Compute local animations
        if (remoteUnderwater) {
            instance.transform.rotate(Vector3.X, -45); // Lean forward
            float swimAngle = (float) Math.sin(animationTimer * 5f) * 30f;

            updateLimb(instance, "body", 0, 0, 0, Vector3.X, 0);
            updateLimb(instance, "head", 0, 0.3f, 0, Vector3.X, state.getPitch());

            updateLimb(instance, "left_arm", -0.35f, 0.3f, 0, Vector3.Y, swimAngle + 45);
            updateLimb(instance, "right_arm", 0.35f, 0.3f, 0, Vector3.Y, -swimAngle - 45);
            updateLimb(instance, "left_leg", -0.15f, -0.3f, 0, Vector3.X, 10);
            updateLimb(instance, "right_leg", 0.15f, -0.3f, 0, Vector3.X, 10);

        } else {
            float walkSpeed = 15f;
            float angle = isMoving ? (float) Math.sin(animationTimer * walkSpeed) * 35f : 0f;

            // Apply falling arm lift
            float fallArmAngle = (!isMoving && Math.abs(state.getVy()) > 1.0f) ? 20f : 0f;

            // These exact offsets position the limbs natively from the body center
            updateLimb(instance, "body", 0, 0, 0, Vector3.X, 0);
            updateLimb(instance, "head", 0, 0.3f, 0, Vector3.X, state.getPitch());

            updateLimb(instance, "left_arm", -0.35f, 0.3f, 0, Vector3.Z, fallArmAngle);
            if (fallArmAngle == 0) updateLimb(instance, "left_arm", -0.35f, 0.3f, 0, Vector3.X, -angle);

            updateLimb(instance, "right_arm", 0.35f, 0.3f, 0, Vector3.Z, -fallArmAngle);
            if (fallArmAngle == 0) updateLimb(instance, "right_arm", 0.35f, 0.3f, 0, Vector3.X, angle);

            updateLimb(instance, "left_leg", -0.15f, -0.3f, 0, Vector3.X, angle);
            updateLimb(instance, "right_leg", 0.15f, -0.3f, 0, Vector3.X, -angle);
        }

        // 3. LibGDX reads the translations and rotations we just set and updates the matrices correctly!
        instance.calculateTransforms();
        modelBatch.render(instance, environment);
    }

    private void updateLimb(ModelInstance instance, String id, float x, float y, float z, Vector3 axis, float angle) {
        var node = instance.getNode(id);
        if (node != null) {
            node.translation.set(x, y, z);
            node.rotation.setFromAxis(axis, angle);
        }
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
        ServerConnection.getInstance().getClient().removeListener(networkListener);
        voxelWorld.dispose();
        hud.dispose();
        pauseOverlay.dispose();
        modelBatch.dispose();
        if (playerModel != null) playerModel.dispose();
    }

    private void handleReceivedMessages() {
        // Nothing here anymore, chunks are fetched via listener
    }

    private void requestNeededChunks() {
        int playerChunkX = (int) Math.floor(physicsState.getX() / Chunk.SIZE_X);
        int playerChunkZ = (int) Math.floor(physicsState.getZ() / Chunk.SIZE_Z);

        int currentChunkDistance = PauseOverlay.getChunkLoadDistance();

        for (int cx = playerChunkX - currentChunkDistance; cx <= playerChunkX + currentChunkDistance; cx++) {
            for (int cz = playerChunkZ - currentChunkDistance; cz <= playerChunkZ + currentChunkDistance; cz++) {
                if (!voxelWorld.getWorld().hasChunk(cx, cz)) {
                    ServerConnection.getInstance().getClient().sendUDP(new ChunkRequestMessage(cx, cz));
                }
            }
        }
    }
}
