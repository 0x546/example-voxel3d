package ee.taltech.examplegame.game;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

import constant.BlockConstants;

import static constant.Constants.*;

import ee.taltech.examplegame.shared.world.Chunk;
import ee.taltech.examplegame.shared.world.World;

/**
 * Builds LibGDX Meshes from a 3D voxel array.
 * Separates opaque geometry from transparent geometry.
 */
public class VoxelMeshBuilder {

    public VoxelMeshBuilder() {
        // Utility class constructor is explicitly empty. Build state is instantiated per #buildMesh call.
    }

    /**
     * Container for the resulting opaque and water meshes.
     */
    public record MeshPair(Mesh opaqueMesh, Mesh waterMesh, Mesh transparentMesh) {}

    /**
     * Context object passed during mesh building to avoid long parameter lists.
     * Contains block data and target buffers.
     */
    private static class BuildContext {
        final Chunk chunk;
        final World world;
        final MeshBuffer opaque;
        final MeshBuffer water;
        final MeshBuffer transparent;

        BuildContext(Chunk chunk, World world, MeshBuffer opaque, MeshBuffer water, MeshBuffer transparent) {
            this.chunk = chunk;
            this.world = world;
            this.opaque = opaque;
            this.water = water;
            this.transparent = transparent;
        }
    }

    /**
     * Face definitions with precomputed vertex positions (relative to voxel),
     * normals and neighbor offsets.
     */
    private enum Face {
        TOP    (new int[] {0, 1, 0}, new float[] {0,1,1,  1,1,1,  1,1,0,  0,1,0}, new float[] {0,1,0}),
        BOTTOM (new int[] {0,-1, 0}, new float[] {0,0,0,  1,0,0,  1,0,1,  0,0,1}, new float[] {0,-1,0}),
        LEFT   (new int[] {-1,0,0}, new float[] {0,0,1,  0,1,1,  0,1,0,  0,0,0}, new float[] {-1,0,0}),
        RIGHT  (new int[] {1,0,0}, new float[] {1,0,0,  1,1,0,  1,1,1,  1,0,1}, new float[] {1,0,0}),
        FRONT  (new int[] {0,0,1}, new float[] {1,0,1,  1,1,1,  0,1,1,  0,0,1}, new float[] {0,0,1}),
        BACK   (new int[] {0,0,-1}, new float[] {0,0,0,  0,1,0,  1,1,0,  1,0,0}, new float[] {0,0,-1});

        final int[] offset; // dx,dy,dz
        final float[] verts; // 4 * (x,y,z)
        final float[] normal; // (nx,ny,nz)

        Face(int[] offset, float[] verts, float[] normal) {
            this.offset = offset;
            this.verts = verts;
            this.normal = normal;
        }
    }

    /**
     * Constructs meshes for opaque and water blocks. Iterates over all voxels and
     * processes visible faces.
     */
    public MeshPair build(Chunk chunk, World world) {
        MeshBuffer opaqueBuffer = new MeshBuffer();
        MeshBuffer waterBuffer = new MeshBuffer();
        MeshBuffer transparentBuffer = new MeshBuffer();

        BuildContext ctx = new BuildContext(chunk, world, opaqueBuffer, waterBuffer, transparentBuffer);

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    processVoxel(ctx, x, y, z);
                }
            }
        }

        return new MeshPair(opaqueBuffer.createMesh(), waterBuffer.createMesh(), transparentBuffer.createMesh());
    }

    /**
     * Process voxel at (x,y,z): determine material and add visible faces.
     */
    private void processVoxel(BuildContext ctx, int localX, int y, int localZ) {
        int mat = ctx.chunk.getBlocks()[localX][y][localZ];
        if (mat == BlockConstants.MAT_AIR)
            return;

        boolean selfIsWater = (mat == BlockConstants.MAT_WATER);
        boolean selfIsTransparent = (mat == BlockConstants.MAT_GLASS);

        MeshBuffer target;
        if (selfIsWater) {
            target = ctx.water;
        } else {
            if (selfIsTransparent) target = ctx.transparent;
            else target = ctx.opaque;
        }

        int worldX = ctx.chunk.getChunkX() * Chunk.SIZE_X + localX;
        int worldZ = ctx.chunk.getChunkZ() * Chunk.SIZE_Z + localZ;

        for (Face face : Face.values()) {
            if (shouldDrawFace(ctx.world, worldX, y, worldZ, face, mat)) {
                target.addFace(worldX, y, worldZ, face, mat);
            }
        }
    }

    /**
     * Visibility rules:
     * 1. If neighbor is out of bounds => visible.
     * 2. If neighbor is air => visible.
     * 3. If self is water and neighbor is not water => visible (and vice versa).
     */
    private boolean shouldDrawFace(World world, int x, int y, int z, Face face, int selfMat) {
        int nx = x + face.offset[0];
        int ny = y + face.offset[1];
        int nz = z + face.offset[2];

        // World boundary (Y only)
        if (ny < 0 || ny >= Chunk.SIZE_Y)
            return true;

        int nMat = world.getBlock(nx, ny, nz);

        // Not generated yet => assume visible for now, or don't draw. Usually safer to draw boundary.
        if (nMat == -1) return true;

        // Neighbor is air => visible
        if (nMat == BlockConstants.MAT_AIR)
            return true;

        // Glass against Glass => hidden
        if (selfMat == BlockConstants.MAT_GLASS && nMat == BlockConstants.MAT_GLASS)
            return false;

        // Water against Water => hidden
        if (selfMat == BlockConstants.MAT_WATER && nMat == BlockConstants.MAT_WATER)
            return false;

        // Transparent vs opaque => visible
        return nMat == BlockConstants.MAT_WATER || nMat == BlockConstants.MAT_GLASS;
    }

    // ==================================================================================
    // MeshBuffer
    // ==================================================================================
    private static class MeshBuffer {
        private final FloatArray verts = new FloatArray();
        private final ShortArray inds = new ShortArray();

        private static final float[] STATIC_UVS = {0f,0f, 1f,0f, 1f,1f, 0f,1f};

        /**
         * Adds a quad for the given face at voxel (x,y,z) with material ID.
         * Encodes position, normal, UVs and material into the vertex data.
         */
        public void addFace(int x, int y, int z, Face face, int matId) {
            int startVertex = verts.size / FLOATS_PER_VERTEX;

            if (startVertex + 4 > MAX_VERTICES) {
                System.err
                        .println("[VoxelMeshBuilder] Vertex limit reached — skipping face at " + x + "," + y + "," + z);
                return;
            }

            float matEnc = matId / MAT_ID_ENCODING_FACTOR;
            float[] quad = face.verts;
            float[] normal = face.normal;

            for (int i = 0; i < 4; i++) {
                // Position
                verts.add(x + quad[i * 3]);
                verts.add(y + quad[i * 3 + 1]);
                verts.add(z + quad[i * 3 + 2]);

                // Normal
                verts.add(normal[0]);
                verts.add(normal[1]);
                verts.add(normal[2]);

                // UV
                verts.add(STATIC_UVS[i * 2]);
                verts.add(STATIC_UVS[i * 2 + 1]);

                // Material
                verts.add(matEnc);
                verts.add(0f);
            }

            short base = (short) startVertex;
            inds.add(base);
            inds.add((short) (base + 1));
            inds.add((short) (base + 2));
            inds.add((short) (base + 2));
            inds.add((short) (base + 3));
            inds.add(base);
        }

        /**
         * Creates a LibGDX Mesh from the accumulated vertex and index data.
         */
        public Mesh createMesh() {
            if (verts.size == 0) {
                return new Mesh(true, 0, 0,
                        new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                        new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord1"));
            }

            Mesh mesh = new Mesh(true, verts.size / FLOATS_PER_VERTEX, inds.size,
                    new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                    new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                    new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                    new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord1"));

            mesh.setVertices(verts.items, 0, verts.size);
            mesh.setIndices(inds.items, 0, inds.size);
            return mesh;
        }
    }
}
