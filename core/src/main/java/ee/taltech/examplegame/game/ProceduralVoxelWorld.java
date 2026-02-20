package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Disposable;
import ee.taltech.examplegame.shared.game.TerrainGenerator;
import ee.taltech.examplegame.shared.game.TreeGenerator;

/**
 * High-level class that uses shared TerrainGenerator and TreeGenerator,
 * along with VoxelMeshBuilder logic to generate, mesh and render the voxel
 * world.
 */
public class ProceduralVoxelWorld implements Disposable {

    private static final int W = 24;
    private static final int H = 32;
    private static final int D = 24;
    private static final int WATER_LEVEL = 16;

    private final Mesh opaqueMesh;
    private final Mesh waterMesh;
    private final ShaderProgram shader;
    private final int[][][] blocks;

    public ProceduralVoxelWorld() {
        blocks = new int[W][H][D];
        // generate
        TerrainGenerator tg = new TerrainGenerator(W, H, D, WATER_LEVEL);
        tg.generate(blocks);
        TreeGenerator treeGen = new TreeGenerator(W, H, D);
        treeGen.growTrees(blocks);

        // build meshes
        VoxelMeshBuilder builder = new VoxelMeshBuilder(W, H, D);
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

    public int[][][] getBlocks() {
        return blocks;
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
        shader.setUniformf("u_mat_player", 0.8f, 0.2f, 0.2f);

        // Render opaque geometry first
        if (opaqueMesh != null)
            opaqueMesh.render(shader, GL20.GL_TRIANGLES);

        // Render water with blending after opaque
        if (waterMesh != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
