package ee.taltech.examplegame.screen.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import constant.BlockConstants;
import constant.Colors;
import lombok.Setter;

public class VoxelHud {
    private final ShapeRenderer shapeRenderer;
    @Setter private int selectedBlock = BlockConstants.MAT_DIRT;
    private int[] blocks = {BlockConstants.MAT_DIRT, BlockConstants.MAT_STONE, BlockConstants.MAT_WOOD};

    public VoxelHud() {
        this.shapeRenderer = new ShapeRenderer();
    }

    public void setBuildableBlocks(int[] blocks) {
        this.blocks = blocks;
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

        // Draw action bar
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float barWidth = blocks.length * 50F;
        float startX = (Gdx.graphics.getWidth() - barWidth) / 2f;
        float startY = 20;

        for (int i = 0; i < blocks.length; i++) {
            float blockX = startX + i * 50 + 5;
            float blockY = startY + 5;

            if (blocks[i] == BlockConstants.MAT_GRASS) {
                shapeRenderer.setColor(new Color(Colors.GRASS.r(), Colors.GRASS.g(), Colors.GRASS.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_DIRT) {
                shapeRenderer.setColor(new Color(Colors.DIRT.r(), Colors.DIRT.g(), Colors.DIRT.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_STONE) {
                shapeRenderer.setColor(new Color(Colors.STONE.r(), Colors.STONE.g(), Colors.STONE.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_WOOD) {
                shapeRenderer.setColor(new Color(Colors.WOOD.r(), Colors.WOOD.g(), Colors.WOOD.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_LEAVES) {
                shapeRenderer.setColor(new Color(Colors.LEAVES.r(), Colors.LEAVES.g(), Colors.LEAVES.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_SAND) {
                shapeRenderer.setColor(new Color(Colors.SAND.r(), Colors.SAND.g(), Colors.SAND.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_BRICK) {
                shapeRenderer.setColor(new Color(Colors.BRICK.r(), Colors.BRICK.g(), Colors.BRICK.b(), 1f));
            } else if (blocks[i] == BlockConstants.MAT_GLASS) {
                shapeRenderer.setColor(new Color(Colors.GLASS.r(), Colors.GLASS.g(), Colors.GLASS.b(), 0.5f));
            } else {
                shapeRenderer.setColor(Color.MAGENTA);
            }

            shapeRenderer.rect(blockX, blockY, 40, 40);
        }
        shapeRenderer.end();

        // Highlight selected
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == selectedBlock) {
                float blockX = startX + i * 50 + 5;
                float blockY = startY + 5;
                shapeRenderer.rect(blockX - 2, blockY - 2, 44, 44);
            }
        }
        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
