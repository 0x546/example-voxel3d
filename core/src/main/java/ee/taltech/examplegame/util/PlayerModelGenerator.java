package ee.taltech.examplegame.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.Texture;

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

        // Material colors
        Material skinMat = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.6f, 0.5f, 1f))); // Skin tone

        // Procedural Textures
        Texture shirtTex = TextureGenerator.generateNoiseTexture(64, 64, Color.CYAN);
        Texture pantsTex = TextureGenerator.generateNoiseTexture(64, 64, Color.BLUE);

        Material shirtMat = new Material(TextureAttribute.createDiffuse(shirtTex));
        Material pantsMat = new Material(TextureAttribute.createDiffuse(pantsTex));

        Material shoeMat = new Material(ColorAttribute.createDiffuse(Color.GRAY));

        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        // Head
        modelBuilder.node().id = "head";
        modelBuilder.node().translation.set(0, 1.6f, 0);
        modelBuilder.part("head", GL20.GL_TRIANGLES, attr, skinMat)
                .box(0.4f, 0.4f, 0.4f);

        // Body
        modelBuilder.node().id = "body";
        modelBuilder.node().translation.set(0, 1.1f, 0);
        modelBuilder.part("body", GL20.GL_TRIANGLES, attr, shirtMat)
                .box(0.4f, 0.6f, 0.2f);

        // Left Arm
        modelBuilder.node().id = "left_arm";
        modelBuilder.node().translation.set(-0.35f, 1.1f, 0);
        modelBuilder.part("left_arm", GL20.GL_TRIANGLES, attr, skinMat)
                .box(0.2f, 0.6f, 0.2f);

        // Right Arm
        modelBuilder.node().id = "right_arm";
        modelBuilder.node().translation.set(0.35f, 1.1f, 0);
        modelBuilder.part("right_arm", GL20.GL_TRIANGLES, attr, skinMat)
                .box(0.2f, 0.6f, 0.2f);

        // Left Leg
        modelBuilder.node().id = "left_leg";
        modelBuilder.node().translation.set(-0.15f, 0.5f, 0);
        modelBuilder.part("left_leg", GL20.GL_TRIANGLES, attr, pantsMat)
                .box(0.2f, 0.6f, 0.2f);

        // Left Shoe
        modelBuilder.node().id = "left_shoe";
        modelBuilder.node().translation.set(-0.15f, 0.1f, 0);
        modelBuilder.part("left_shoe", GL20.GL_TRIANGLES, attr, shoeMat)
                .box(0.2f, 0.2f, 0.2f);

        // Right Leg
        modelBuilder.node().id = "right_leg";
        modelBuilder.node().translation.set(0.15f, 0.5f, 0);
        modelBuilder.part("right_leg", GL20.GL_TRIANGLES, attr, pantsMat)
                .box(0.2f, 0.6f, 0.2f);

        // Right Shoe
        modelBuilder.node().id = "right_shoe";
        modelBuilder.node().translation.set(0.15f, 0.1f, 0);
        modelBuilder.part("right_shoe", GL20.GL_TRIANGLES, attr, shoeMat)
                .box(0.2f, 0.2f, 0.2f);

        return modelBuilder.end();
    }
}
