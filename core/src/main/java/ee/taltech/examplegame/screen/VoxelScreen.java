package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.examplegame.game.VoxelWorld;
import ee.taltech.examplegame.screen.overlay.VoxelHud;

public class VoxelScreen extends ScreenAdapter {

    private final Game game;
    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final VoxelWorld voxelWorld;
    private final VoxelHud hud;

    private float pitch = 0;
    private float yaw = 0;
    private static float mouseSensitivity = 0.2f;

    public VoxelScreen(Game game) {
        this.game = game;

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(16, 10, 16);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        yaw = -135f; // Initial direction
        pitch = -30f;

        Gdx.input.setCursorCatched(true);

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        voxelWorld = new VoxelWorld();
        hud = new VoxelHud();
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        updateCamera();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f); // Sky blue
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        voxelWorld.render(modelBatch, environment);
        modelBatch.end();

        hud.render();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (Gdx.input.isCursorCatched()) {
                Gdx.input.setCursorCatched(false);
            } else {
                game.setScreen(new TitleScreen(game));
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Gdx.input.setCursorCatched(true);
        }
    }

    private void handleInput(float delta) {
        float speed = 10f * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
            speed *= 2f;

        Vector3 tmp = new Vector3();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            tmp.set(camera.direction).set(tmp.x, 0, tmp.z).nor().scl(speed);
            camera.position.add(tmp);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            tmp.set(camera.direction).set(tmp.x, 0, tmp.z).nor().scl(-speed);
            camera.position.add(tmp);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(-speed);
            camera.position.add(tmp);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(speed);
            camera.position.add(tmp);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            camera.position.y += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            camera.position.y -= speed;
        }
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
        modelBatch.dispose();
        voxelWorld.dispose();
        hud.dispose();
    }
}
