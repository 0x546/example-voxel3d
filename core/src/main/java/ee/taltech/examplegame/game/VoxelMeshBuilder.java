package ee.taltech.examplegame.game;

import constant.BlockConstants;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

/**
 * Builds LibGDX Meshes from a 3D voxel array.
 * Separates opaque geometry from transparent geometry.
 */
public class VoxelMeshBuilder {

    // --- Configuration ---
    private static final int FLOATS_PER_VERTEX = 10; // Pos(3) + Norm(3) + UV(2) + Mat(2)
    private static final int MAX_VERTICES = 32000; // Safe limit for Short indices

    private final int width;
    private final int height;
    private final int depth;

    public VoxelMeshBuilder(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    /**
     * Container for the resulting opaque and water meshes.
     */
    public record MeshPair(Mesh opaqueMesh, Mesh waterMesh) {}

    /**
     * Context object passed during mesh building to avoid long parameter lists.
     * Contains block data and target buffers.
     */
    private static class BuildContext {
        final int[][][] blocks;
        final MeshBuffer opaque;
        final MeshBuffer water;

        BuildContext(int[][][] blocks, MeshBuffer opaque, MeshBuffer water) {
            this.blocks = blocks;
            this.opaque = opaque;
            this.water = water;
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
    public MeshPair build(int[][][] blocks) {
        MeshBuffer opaqueBuffer = new MeshBuffer();
        MeshBuffer waterBuffer = new MeshBuffer();

        BuildContext ctx = new BuildContext(blocks, opaqueBuffer, waterBuffer);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    processVoxel(ctx, x, y, z);
                }
            }
        }

        return new MeshPair(opaqueBuffer.createMesh(), waterBuffer.createMesh());
    }

    /**
     * Process voxel at (x,y,z): determine material and add visible faces.
     */
    private void processVoxel(BuildContext ctx, int x, int y, int z) {
        int mat = ctx.blocks[x][y][z];
        if (mat == BlockConstants.MAT_AIR)
            return;

        boolean selfIsWater = (mat == BlockConstants.MAT_WATER);
        MeshBuffer target = selfIsWater ? ctx.water : ctx.opaque;

        for (Face face : Face.values()) {
            if (shouldDrawFace(ctx.blocks, x, y, z, face, selfIsWater)) {
                target.addFace(x, y, z, face, mat);
            }
        }
    }

    /**
     * Visibility rules:
     * 1. If neighbor is out of bounds => visible.
     * 2. If neighbor is air => visible.
     * 3. If self is water and neighbor is not water => visible (and vice versa).
     */
    private boolean shouldDrawFace(int[][][] blocks, int x, int y, int z, Face face, boolean selfIsWater) {
        int nx = x + face.offset[0];
        int ny = y + face.offset[1];
        int nz = z + face.offset[2];

        // World boundary => visible
        if (nx < 0 || nx >= width || ny < 0 || ny >= height || nz < 0 || nz >= depth)
            return true;

        int nMat = blocks[nx][ny][nz];

        // Neighbor is air => visible
        if (nMat == BlockConstants.MAT_AIR)
            return true;

        boolean neighborIsWater = (nMat == BlockConstants.MAT_WATER);

        // Visible if different fluid/solid state (water vs non-water).
        return selfIsWater != neighborIsWater;
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

            float matEnc = matId / 255f;
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
