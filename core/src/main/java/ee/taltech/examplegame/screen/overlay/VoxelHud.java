package ee.taltech.examplegame.screen.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class VoxelHud {
    private final ShapeRenderer shapeRenderer;

    public VoxelHud() {
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render() {
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.updateMatrices();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);

        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;
        float size = 10;

        // Draw crosshair
        shapeRenderer.line(centerX - size, centerY, centerX + size, centerY);
        shapeRenderer.line(centerX, centerY - size, centerX, centerY + size);

        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
