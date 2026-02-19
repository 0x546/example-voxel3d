package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Disposable;

/**
 * High-level class that uses TerrainGenerator, TreeGenerator, VoxelMeshBuilder and ProceduralMaterials
 * to generate, mesh and render the voxel world with water and no grass under water.
 */
public class ProceduralVoxelWorld implements Disposable {

    // material IDs (shared across files)
    public static final int MAT_AIR = 0;
    public static final int MAT_GRASS = 1;
    public static final int MAT_DIRT = 2;
    public static final int MAT_STONE = 3;
    public static final int MAT_WOOD = 4;
    public static final int MAT_LEAVES = 5;
    public static final int MAT_WATER = 6;

    private static final int W = 24;
    private static final int H = 32;
    private static final int D = 24;
    private static final int WATER_LEVEL = 16;

    private final Mesh opaqueMesh;
    private final Mesh waterMesh;
    private final ShaderProgram shader;

    public ProceduralVoxelWorld() {
        int[][][] blocks = new int[W][H][D];
        // generate
        TerrainGenerator tg = new TerrainGenerator(W,H,D,WATER_LEVEL);
        tg.generate(blocks);
        TreeGenerator treeGen = new TreeGenerator(W,H,D);
        treeGen.growTrees(blocks);

        // build meshes
        VoxelMeshBuilder builder = new VoxelMeshBuilder(W,H,D);
        VoxelMeshBuilder.MeshPair mp = builder.build(blocks);
        opaqueMesh = mp.opaqueMesh();
        waterMesh = mp.waterMesh();

        // shader
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Shaders.VERT, Shaders.FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("Shader", shader.getLog());
        }
    }

    public void render(Camera camera) {
        // Common GL state
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);

        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_lightDir", -0.5f, -1f, -0.3f);

        // base colors
        shader.setUniformf("u_mat_grass", 0.22f, 0.7f, 0.2f);
        shader.setUniformf("u_mat_dirt", 0.45f, 0.25f, 0.05f);
        shader.setUniformf("u_mat_stone", 0.5f, 0.5f, 0.5f);
        shader.setUniformf("u_mat_wood", 0.45f, 0.28f, 0.12f);
        shader.setUniformf("u_mat_leaves", 0.15f, 0.6f, 0.15f);
        shader.setUniformf("u_mat_water", 0.1f, 0.35f, 0.55f);

        // Render opaque geometry first
        if (opaqueMesh != null) opaqueMesh.render(shader, GL20.GL_TRIANGLES);

        // Render water with blending after opaque
        if (waterMesh != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            // optionally reduce depth mask to allow nicer blending; for simplicity keep depth write on
            waterMesh.render(shader, GL20.GL_TRIANGLES);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void dispose() {
        if (opaqueMesh != null) opaqueMesh.dispose();
        if (waterMesh != null) waterMesh.dispose();
        if (shader != null) shader.dispose();
    }
}
