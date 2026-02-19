package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.examplegame.game.ProceduralVoxelWorld;
import ee.taltech.examplegame.screen.overlay.VoxelHud;

public class VoxelScreen extends ScreenAdapter {

    private final Game game;
    private final PerspectiveCamera camera;
    private final Environment environment;
    private final ProceduralVoxelWorld voxelWorld;
    private final VoxelHud hud;

    private float pitch = 0;
    private float yaw = 0;
    private static float mouseSensitivity = 0.2f;

    public VoxelScreen(Game game) {
        this.game = game;

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 20, 12);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        yaw = -135f; // Initial direction
        pitch = -30f;

        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        voxelWorld = new ProceduralVoxelWorld();
        hud = new VoxelHud();
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        updateCamera();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f); // Sky blue
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        voxelWorld.render(camera);

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
        float speed = 10f * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= 2f;

        // FIX: Calculate movement based on Flat YAW, ignoring PITCH.
        // This prevents getting stuck when looking straight up or down.
        float dx = (float) Math.sin(Math.toRadians(yaw)); // LibGDX sin takes radians
        float dz = (float) Math.cos(Math.toRadians(yaw)); // LibGDX cos takes radians

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.position.x -= dx * speed;
            camera.position.z -= dz * speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.position.x += dx * speed;
            camera.position.z += dz * speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            // Strafe left (perpendicular to forward)
            camera.position.x -= dz * speed;
            camera.position.z += dx * speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            // Strafe right
            camera.position.x += dz * speed;
            camera.position.z -= dx * speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            camera.position.y += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            camera.position.y -= speed;
        }

        camera.update();
    }

    private void updateCamera() {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
            float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

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
