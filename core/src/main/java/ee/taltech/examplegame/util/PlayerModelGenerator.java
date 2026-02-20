package ee.taltech.examplegame.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;

public class PlayerModelGenerator {
    private PlayerModelGenerator() {
        /* This utility class should not be instantiated */
    }

    /**
     * Generates a simple voxel-style player model.
     * Dimensions are approximately based on 1 unit = 1 meter (or 1 block).
     * Standard height ~1.8m.
     */
    public static Model createPlayerModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        Material skinMat = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.6f, 0.5f, 1f)));
        Texture shirtTex = TextureGenerator.generateNoiseTexture(64, 64, Color.CYAN);
        Texture pantsTex = TextureGenerator.generateNoiseTexture(64, 64, Color.BLUE);
        Material shirtMat = new Material(TextureAttribute.createDiffuse(shirtTex));
        Material pantsMat = new Material(TextureAttribute.createDiffuse(pantsTex));
        Material shoeMat = new Material(ColorAttribute.createDiffuse(Color.GRAY));
        Material eyeWhite = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        Material eyeBlack = new Material(ColorAttribute.createDiffuse(Color.BLACK));
        Material hairMat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.2f, 0.1f, 1f)));
        Material mouthMat = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.4f, 0.4f, 1f)));
        Material noseMat = new Material(ColorAttribute.createDiffuse(new Color(0.7f, 0.5f, 0.4f, 1f)));

        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        // 1. HEAD NODE (Groups all face parts together, pivot is at the neck: 0,0,0)
        modelBuilder.node().id = "head";
        BoxShapeBuilder.build(modelBuilder.part("head_base", GL20.GL_TRIANGLES, attr, skinMat), 0, 0.2f, 0, 0.4f, 0.4f, 0.4f);
        BoxShapeBuilder.build(modelBuilder.part("eye_l", GL20.GL_TRIANGLES, attr, eyeWhite), -0.1f, 0.3f, -0.2f, 0.08f, 0.08f, 0.02f);
        BoxShapeBuilder.build(modelBuilder.part("eye_r", GL20.GL_TRIANGLES, attr, eyeWhite), 0.1f, 0.3f, -0.2f, 0.08f, 0.08f, 0.02f);
        BoxShapeBuilder.build(modelBuilder.part("pupil_l", GL20.GL_TRIANGLES, attr, eyeBlack), -0.1f, 0.3f, -0.211f, 0.04f, 0.04f, 0.01f);
        BoxShapeBuilder.build(modelBuilder.part("pupil_r", GL20.GL_TRIANGLES, attr, eyeBlack), 0.1f, 0.3f, -0.211f, 0.04f, 0.04f, 0.01f);
        BoxShapeBuilder.build(modelBuilder.part("nose", GL20.GL_TRIANGLES, attr, noseMat), 0, 0.22f, -0.21f, 0.06f, 0.08f, 0.05f);
        BoxShapeBuilder.build(modelBuilder.part("mouth", GL20.GL_TRIANGLES, attr, mouthMat), 0, 0.12f, -0.205f, 0.15f, 0.03f, 0.01f);
        BoxShapeBuilder.build(modelBuilder.part("ear_l", GL20.GL_TRIANGLES, attr, skinMat), -0.21f, 0.22f, 0, 0.05f, 0.1f, 0.05f);
        BoxShapeBuilder.build(modelBuilder.part("ear_r", GL20.GL_TRIANGLES, attr, skinMat), 0.21f, 0.22f, 0, 0.05f, 0.1f, 0.05f);
        BoxShapeBuilder.build(modelBuilder.part("hair_top", GL20.GL_TRIANGLES, attr, hairMat), 0, 0.42f, 0, 0.44f, 0.1f, 0.44f);
        BoxShapeBuilder.build(modelBuilder.part("hair_back", GL20.GL_TRIANGLES, attr, hairMat), 0, 0.25f, 0.15f, 0.44f, 0.3f, 0.15f);

        // 2. BODY NODE (Pivot at center)
        modelBuilder.node().id = "body";
        BoxShapeBuilder.build(modelBuilder.part("chest", GL20.GL_TRIANGLES, attr, shirtMat), 0, 0, 0, 0.4f, 0.6f, 0.2f);

        // 3. ARM NODES (Pivot at shoulder)
        modelBuilder.node().id = "left_arm";
        BoxShapeBuilder.build(modelBuilder.part("arm_l", GL20.GL_TRIANGLES, attr, skinMat), 0, -0.3f, 0, 0.2f, 0.6f, 0.2f);
        modelBuilder.node().id = "right_arm";
        BoxShapeBuilder.build(modelBuilder.part("arm_r", GL20.GL_TRIANGLES, attr, skinMat), 0, -0.3f, 0, 0.2f, 0.6f, 0.2f);

        // 4. LEG NODES (Groups pants and shoes, pivot at hip)
        modelBuilder.node().id = "left_leg";
        BoxShapeBuilder.build(modelBuilder.part("thigh_l", GL20.GL_TRIANGLES, attr, pantsMat), 0, -0.3f, 0, 0.2f, 0.6f, 0.2f);
        BoxShapeBuilder.build(modelBuilder.part("shoe_l", GL20.GL_TRIANGLES, attr, shoeMat), 0, -0.65f, 0, 0.2f, 0.1f, 0.2f);

        modelBuilder.node().id = "right_leg";
        BoxShapeBuilder.build(modelBuilder.part("thigh_r", GL20.GL_TRIANGLES, attr, pantsMat), 0, -0.3f, 0, 0.2f, 0.6f, 0.2f);
        BoxShapeBuilder.build(modelBuilder.part("shoe_r", GL20.GL_TRIANGLES, attr, shoeMat), 0, -0.65f, 0, 0.2f, 0.1f, 0.2f);

        // All nodes are roots. No hierarchies to break!
        return modelBuilder.end();
    }
}
