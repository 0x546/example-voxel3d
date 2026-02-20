package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import ee.taltech.examplegame.game.GameStateManager;
import ee.taltech.examplegame.game.PlayerInputManager;
import ee.taltech.examplegame.game.ProceduralVoxelWorld;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.screen.overlay.VoxelHud;
import ee.taltech.examplegame.util.PlayerModelGenerator;

public class VoxelScreen extends ScreenAdapter {

    private final Game game;
    private final PerspectiveCamera camera;
    private final Environment environment;
    private final ProceduralVoxelWorld voxelWorld;
    private final VoxelHud hud;

    private final GameStateManager gameStateManager;
    private final PlayerInputManager inputManager;
    private final ModelBatch modelBatch;
    private final Model playerModel;
    private final ModelInstance playerInstance; // Reused for rendering
    private final java.util.Map<Integer, Vector3> playerPositions = new java.util.HashMap<>();

    private float pitch = 0;
    private float yaw = 0;

    // Physics state
    private float vx;
    private float vy;
    private float vz;
    private boolean onGround = false;

    public VoxelScreen(Game game) {
        this.game = game;

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
        playerInstance = new ModelInstance(playerModel);
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        updatePhysics(delta);
        inputManager.update(yaw, pitch); // Send movement intention to server
        updateCamera();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f); // Sky blue
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        voxelWorld.render(camera);

        // Render other players
        modelBatch.begin(camera);
        var playerStates = gameStateManager.getLatestGameStateMessage().getPlayerStates();
        if (playerStates != null) {
            int myId = ServerConnection.getInstance().getClient().getID();
            for (var state : playerStates) {
                if (state.getId() == myId) {
                    if (Math.abs(camera.position.x - state.getX()) > PLAYER_SNAP_DISTANCE
                            || Math.abs(camera.position.z - state.getZ()) > PLAYER_SNAP_DISTANCE
                            || Math.abs(camera.position.y - state.getY()) > PLAYER_SNAP_DISTANCE) {
                        camera.position.set(state.getX(), state.getY() + EYE_HEIGHT, state.getZ());
                    }
                    continue;
                }

                // Simple interpolation/smoothing
                Vector3 targetPos = new Vector3(state.getX(), state.getY(), state.getZ());

                if (!playerPositions.containsKey(state.getId())) {
                    playerPositions.put(state.getId(), targetPos);
                }

                Vector3 currentPos = playerPositions.get(state.getId());
                currentPos.lerp(targetPos, PLAYER_INTERPOLATION_SPEED * delta);

                playerInstance.transform.setToTranslation(currentPos);
                playerInstance.transform.rotate(Vector3.Y, state.getYaw());

                // Head tilt
                var headNode = playerInstance.getNode("head");
                if (headNode != null) {
                    headNode.rotation.setEulerAngles(0, state.getPitch(), 0);
                    playerInstance.calculateTransforms();
                }

                modelBatch.render(playerInstance, environment);
            }
        }
        modelBatch.end();

        hud.render();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (Gdx.input.isCursorCatched()) {
                Gdx.input.setCursorCatched(false);
            } else {
                this.dispose();
                game.setScreen(new TitleScreen(game));
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Gdx.input.setCursorCatched(true);
        }
    }


    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
            game.setScreen(new TitleScreen(game));
            return;
        }

        // Apply forces based on input
        float dx = (float) Math.sin(Math.toRadians(yaw));
        float dz = (float) Math.cos(Math.toRadians(yaw));

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            vx -= dx * MOVE_SPEED * delta;
            vz -= dz * MOVE_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            vx += dx * MOVE_SPEED * delta;
            vz += dz * MOVE_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            vx -= dz * MOVE_SPEED * delta;
            vz += dx * MOVE_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            vx += dz * MOVE_SPEED * delta;
            vz -= dx * MOVE_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && onGround) {
            vy = JUMP_VELOCITY;
        }
    }

    private void updatePhysics(float delta) {
        // 1. Gravity
        vy -= GRAVITY * delta;

        // 2. Apply Velocity to potential new position
        // Current feet position
        float x = camera.position.x;
        float y = camera.position.y - EYE_HEIGHT;
        float z = camera.position.z;

        float nextX = x + vx * delta;
        float nextY = y + vy * delta;
        float nextZ = z + vz * delta;

        int[][][] blocks = voxelWorld.getBlocks();

        // 3. Collision Detection
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
            onGround = false;
        } else {
            if (vy < 0)
                onGround = true; // Hit ground
            vy = 0;
        }

        // 4. Damping
        vx *= DAMPING;
        vz *= DAMPING;

        // 5. Bounds Check (Simple world bounds)
        if (blocks != null) {
            x = Math.clamp(x, 0, blocks.length - 1f);
            z = Math.clamp(z, 0, blocks[0][0].length - 1f);
        }

        // Void kill / respawn logic (client side visual only, server handles real
        // death)
        if (y < -10) {
            // Reset to spawn or wait for server correction
            y = 30;
            vy = 0;
        }

        // Update Camera
        camera.position.set(x, y + EYE_HEIGHT, z);
        camera.update();
    }

    private boolean checkCollision(float px, float py, float pz, int[][][] blocks) {
        if (blocks == null)
            return false;

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

    private boolean isSolid(int x, int y, int z, int[][][] blocks) {
        if (x < 0 || x >= blocks.length || z < 0 || z >= blocks[0][0].length)
            return true;
        if (y < 0)
            return true;
        if (y >= blocks[0].length)
            return false;

        int type = blocks[x][y][z];
        return type != 0 && type != 6; // 0=Air, 6=Water.
    }

    private void updateCamera() {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
            float deltaY = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;

            yaw += deltaX;
            pitch += deltaY;

            // Clamp pitch to avoid flipping
            if (pitch > 89f)
                pitch = 89f;
            if (pitch < -89f)
                pitch = -89f;

            camera.direction.set(0, 0, -1);
            camera.direction.rotate(Vector3.Y, yaw);

            Vector3 side = camera.direction.cpy().crs(Vector3.Y).nor();
            camera.direction.rotate(side, pitch);

            camera.up.set(0, 1, 0); // Strictly horizontal level
            camera.update();
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        voxelWorld.dispose();
        hud.dispose();
    }
}
