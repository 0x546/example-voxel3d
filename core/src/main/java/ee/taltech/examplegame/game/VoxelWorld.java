package ee.taltech.examplegame.game;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;

public class VoxelWorld {

    private final Array<ModelInstance> instances = new Array<>();
    private final Array<Model> models = new Array<>();

    public VoxelWorld() {
        ModelBuilder modelBuilder = new ModelBuilder();

        // Create different models for different block types
        Model grassModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(BlockType.GRASS.color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(grassModel);

        Model dirtModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(BlockType.DIRT.color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(dirtModel);

        Model stoneModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(BlockType.STONE.color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(stoneModel);

        // Add wood model for trees (index 3)
        Model woodModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(BlockType.WOOD.color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(woodModel);

        // Add leaves model for trees (index 4)
        Model leavesModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(BlockType.LEAVES.color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(leavesModel);

        // Generate a 24x24 world with random height
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) {
                // Smoother terrain using sine waves
                float noise = (float) (Math.sin(x * 0.2f) * 2.5f + Math.cos(z * 0.3f) * 2.5f
                        + Math.sin((x + z) * 0.1f) * 1.5f);
                int height = (int) (6 + noise);

                for (int y = 0; y < height; y++) {
                    Model model;
                    if (y == height - 1) {
                        if (y < 5)
                            model = dirtModel; // Beach/Shore
                        else
                            model = grassModel;
                    } else if (y > height - 3) {
                        model = dirtModel;
                    } else {
                        model = stoneModel;
                    }

                    ModelInstance instance = new ModelInstance(model);
                    instance.transform.setToTranslation(x, y, z);
                    instances.add(instance);
                }

                // Chance to grow a tree
                if (height >= 6 && MathUtils.random() < 0.05f) {
                    growTree(x, height, z);
                }
            }
        }

        // Add water plane
        Model waterModel = modelBuilder.createBox(24f, 1f, 24f,
                new Material(ColorAttribute.createDiffuse(0, 0.5f, 1f, 0.6f),
                        new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(GL20.GL_SRC_ALPHA,
                                GL20.GL_ONE_MINUS_SRC_ALPHA)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models.add(waterModel);
        ModelInstance waterInstance = new ModelInstance(waterModel);
        waterInstance.transform.setToTranslation(11.5f, 3.5f, 11.5f);
        instances.add(waterInstance);
    }

    private void growTree(int x, int y, int z) {
        // Log
        for (int i = 0; i < 4; i++) {
            ModelInstance log = new ModelInstance(models.get(3)); // Wood model (index 3 based on creation order)
            log.transform.setToTranslation(x, (float) y + i, z);
            instances.add(log);
        }
        // Leaves
        for (int lx = -2; lx <= 2; lx++) {
            for (int ly = 3; ly <= 5; ly++) {
                for (int lz = -2; lz <= 2; lz++) {
                    if ((Math.abs(lx) + Math.abs(lz) > 2 && ly > 4)
                        || (lx == 0 && lz == 0 && ly < 5))
                        continue;

                    ModelInstance leaf = new ModelInstance(models.get(4)); // Leaves model
                    leaf.transform.setToTranslation((float) x + lx, (float) y + ly, (float) z + lz);
                    instances.add(leaf);
                }
            }
        }
    }

    public void render(ModelBatch batch, Environment environment) {
        batch.render(instances, environment);
    }

    public void dispose() {
        for (Model model : models) {
            model.dispose();
        }
    }
}
