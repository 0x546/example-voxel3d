package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

import static constant.Colors.DIRT;
import static constant.Colors.GRASS;
import static constant.Colors.LEAVES;
import static constant.Colors.PLAYER;
import static constant.Colors.STONE;
import static constant.Colors.WATER;
import static constant.Colors.WOOD;
import static constant.Constants.WATER_LEVEL;
import static constant.Constants.WORLD_DEPTH;
import static constant.Constants.WORLD_HEIGHT;
import static constant.Constants.WORLD_WIDTH;
import ee.taltech.examplegame.shared.game.TerrainGenerator;
import ee.taltech.examplegame.shared.game.TreeGenerator;

/**
 * High-level class that uses shared TerrainGenerator and TreeGenerator,
 * along with VoxelMeshBuilder logic to generate, mesh and render the voxel
 * world.
 */
public class ProceduralVoxelWorld implements Disposable {

    private final Mesh opaqueMesh;
    private final Mesh waterMesh;
    private final ShaderProgram shader;
    private final int[][][] blocks;

    public ProceduralVoxelWorld() {
        blocks = new int[WORLD_WIDTH][WORLD_HEIGHT][WORLD_DEPTH];
        // generate
        TerrainGenerator tg = new TerrainGenerator(WORLD_WIDTH, WORLD_HEIGHT, WORLD_DEPTH, WATER_LEVEL);
        tg.generate(blocks);
        TreeGenerator treeGen = new TreeGenerator(WORLD_WIDTH, WORLD_HEIGHT, WORLD_DEPTH);
        treeGen.growTrees(blocks);

        // build meshes
        VoxelMeshBuilder builder = new VoxelMeshBuilder(WORLD_WIDTH, WORLD_HEIGHT, WORLD_DEPTH);
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
        shader.setUniformf("u_mat_grass", GRASS.r(), GRASS.g(), GRASS.b());
        shader.setUniformf("u_mat_dirt", DIRT.r(), DIRT.g(), DIRT.b());
        shader.setUniformf("u_mat_stone", STONE.r(), STONE.g(), STONE.b());
        shader.setUniformf("u_mat_wood", WOOD.r(), WOOD.g(), WOOD.b());
        shader.setUniformf("u_mat_leaves", LEAVES.r(), LEAVES.g(), LEAVES.b());
        shader.setUniformf("u_mat_water", WATER.r(), WATER.g(), WATER.b());
        shader.setUniformf("u_mat_player", PLAYER.r(), PLAYER.g(), PLAYER.b());

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
