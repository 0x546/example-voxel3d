package ee.taltech.examplegame.game;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

import static constant.Colors.DIRT;
import static constant.Colors.GRASS;
import static constant.Colors.LEAVES;
import static constant.Colors.PLAYER;
import static constant.Colors.STONE;
import static constant.Colors.WATER;
import static constant.Colors.WOOD;
import static constant.Colors.SAND;
import static constant.Colors.BRICK;
import static constant.Colors.GLASS;
import static constant.Constants.WATER_LEVEL;
import static constant.Constants.WATER_WAVE_AMPLITUDE;
import static constant.Constants.WATER_WAVE_SPEED;
import ee.taltech.examplegame.shared.world.Chunk;
import ee.taltech.examplegame.shared.world.World;
import lombok.Getter;

/**
 * High-level class that uses VoxelMeshBuilder logic to generate, mesh and render the voxel
 * world dynamically based on chunks.
 */
public class ProceduralVoxelWorld implements Disposable {

    private final ShaderProgram shader;
    private final ShaderProgram waterShader;

    @Getter
    private final World world;

    private final Map<String, VoxelMeshBuilder.MeshPair> chunkMeshes = new HashMap<>();

    @Getter
    private float time = 0f;

    public ProceduralVoxelWorld() {
        this.world = new World();

        // opaque shader
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Shaders.VERT, Shaders.FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("Shader", shader.getLog());
        }

        // water shader
        waterShader = new ShaderProgram(Shaders.WATER_VERT, Shaders.WATER_FRAG);
        if (!waterShader.isCompiled()) {
            Gdx.app.error("WaterShader", waterShader.getLog());
        }
    }

    public void addChunk(Chunk chunk) {
        world.addChunk(chunk);
        rebuildMesh(chunk.getChunkX(), chunk.getChunkZ());

        // Rebuild neighbors to update faces
        rebuildMesh(chunk.getChunkX() - 1, chunk.getChunkZ());
        rebuildMesh(chunk.getChunkX() + 1, chunk.getChunkZ());
        rebuildMesh(chunk.getChunkX(), chunk.getChunkZ() - 1);
        rebuildMesh(chunk.getChunkX(), chunk.getChunkZ() + 1);
    }

    public void setBlock(int x, int y, int z, int blockType) {
        world.setBlock(x, y, z, blockType);

        int chunkX = (int) Math.floor((float) x / Chunk.SIZE_X);
        int chunkZ = (int) Math.floor((float) z / Chunk.SIZE_Z);
        rebuildMesh(chunkX, chunkZ);

        // Rebuild neighbors if modified on boundary
        int localX = x - (chunkX * Chunk.SIZE_X);
        int localZ = z - (chunkZ * Chunk.SIZE_Z);
        if (localX == 0) rebuildMesh(chunkX - 1, chunkZ);
        if (localX == Chunk.SIZE_X - 1) rebuildMesh(chunkX + 1, chunkZ);
        if (localZ == 0) rebuildMesh(chunkX, chunkZ - 1);
        if (localZ == Chunk.SIZE_Z - 1) rebuildMesh(chunkX, chunkZ + 1);
    }

    private void rebuildMesh(int cx, int cz) {
        Chunk chunk = world.getChunk(cx, cz);
        if (chunk == null) return;

        VoxelMeshBuilder builder = new VoxelMeshBuilder();
        VoxelMeshBuilder.MeshPair mp = builder.build(chunk, world);

        String key = cx + "," + cz;
        if (chunkMeshes.containsKey(key)) {
            VoxelMeshBuilder.MeshPair old = chunkMeshes.get(key);
            if (old.opaqueMesh() != null) old.opaqueMesh().dispose();
            if (old.waterMesh() != null) old.waterMesh().dispose();
            if (old.transparentMesh() != null) old.transparentMesh().dispose();
        }
        chunkMeshes.put(key, mp);
    }

    public void render(Camera camera, boolean underwater) {
        float delta = Gdx.graphics.getDeltaTime();
        time += delta;

        // Common GL state
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);

        renderOpaquePass(camera, underwater);
        renderTransparentPass();
        renderWaterPass(camera, underwater);
    }

    private void renderOpaquePass(Camera camera, boolean underwater) {
        // ---- Opaque pass ----
        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_lightDir", -0.5f, -1f, -0.3f);
        shader.setUniformi("u_underwater", underwater ? 1 : 0);
        shader.setUniformf("u_waterLevel", WATER_LEVEL);
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_waveAmp", WATER_WAVE_AMPLITUDE);
        shader.setUniformf("u_waveSpeed", WATER_WAVE_SPEED);

        // base colors
        shader.setUniformf("u_mat_grass", GRASS.r(), GRASS.g(), GRASS.b());
        shader.setUniformf("u_mat_dirt", DIRT.r(), DIRT.g(), DIRT.b());
        shader.setUniformf("u_mat_stone", STONE.r(), STONE.g(), STONE.b());
        shader.setUniformf("u_mat_wood", WOOD.r(), WOOD.g(), WOOD.b());
        shader.setUniformf("u_mat_leaves", LEAVES.r(), LEAVES.g(), LEAVES.b());
        shader.setUniformf("u_mat_water", WATER.r(), WATER.g(), WATER.b());
        shader.setUniformf("u_mat_player", PLAYER.r(), PLAYER.g(), PLAYER.b());
        shader.setUniformf("u_mat_sand", SAND.r(), SAND.g(), SAND.b());
        shader.setUniformf("u_mat_brick", BRICK.r(), BRICK.g(), BRICK.b());
        shader.setUniformf("u_mat_glass", GLASS.r(), GLASS.g(), GLASS.b());

        for (VoxelMeshBuilder.MeshPair meshPair : chunkMeshes.values()) {
            if (meshPair.opaqueMesh() != null && meshPair.opaqueMesh().getNumVertices() > 0)
                meshPair.opaqueMesh().render(shader, GL20.GL_TRIANGLES);
        }
    }

    private void renderTransparentPass() {
        if (chunkMeshes.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Keep shader bound (same as opaque) but with blending
        shader.bind();

        for (VoxelMeshBuilder.MeshPair meshPair : chunkMeshes.values()) {
            if (meshPair.transparentMesh() != null && meshPair.transparentMesh().getNumVertices() > 0)
                meshPair.transparentMesh().render(shader, GL20.GL_TRIANGLES);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderWaterPass(Camera camera, boolean underwater) {
        // ---- Water pass ----
        // Always render the water surface. When underwater, disable culling so the
        // top surface is visible from below. The fragment shader discards non-top
        // faces underwater to prevent z-fighting at water-solid boundaries.
        if (chunkMeshes.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (underwater) {
            Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        }

        waterShader.bind();
        waterShader.setUniformMatrix("u_projView", camera.combined);
        waterShader.setUniformf("u_lightDir", -0.5f, -1f, -0.3f);
        waterShader.setUniformf("u_time", time);
        waterShader.setUniformf("u_waveAmp", WATER_WAVE_AMPLITUDE);
        waterShader.setUniformf("u_waveSpeed", WATER_WAVE_SPEED);
        waterShader.setUniformf("u_cameraPos", camera.position.x, camera.position.y, camera.position.z);
        waterShader.setUniformi("u_underwater", underwater ? 1 : 0);
        waterShader.setUniformf("u_mat_water", WATER.r(), WATER.g(), WATER.b());

        for (VoxelMeshBuilder.MeshPair meshPair : chunkMeshes.values()) {
            if (meshPair.waterMesh() != null && meshPair.waterMesh().getNumVertices() > 0)
                meshPair.waterMesh().render(waterShader, GL20.GL_TRIANGLES);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (underwater) {
            Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        }
    }

    @Override
    public void dispose() {
        for (VoxelMeshBuilder.MeshPair meshPair : chunkMeshes.values()) {
            if (meshPair.opaqueMesh() != null) meshPair.opaqueMesh().dispose();
            if (meshPair.waterMesh() != null) meshPair.waterMesh().dispose();
            if (meshPair.transparentMesh() != null) meshPair.transparentMesh().dispose();
        }
        if (shader != null) shader.dispose();
        if (waterShader != null) waterShader.dispose();
    }
}
