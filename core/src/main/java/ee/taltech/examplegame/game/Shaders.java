package ee.taltech.examplegame.game;

/**
 * This class contains the GLSL shader code for rendering the voxel world.
 * It uses procedural techniques to determine block colors based on their material ID and world position,
 * eliminating the need for a texture atlas.
 * The vertex shader encodes the material ID in a texture coordinate,
 * while the fragment shader decodes it and applies noise-based variations to create visual interest.
 * Water blocks are rendered with a special blue tint and transparency.
 */
public final class Shaders {
    private Shaders() {
        /* This utility class should not be instantiated */
    }

    /**
     * The vertex shader passes world position, normal, and material ID (encoded in a_texCoord1.x) to the fragment shader.
     */
    public static final String VERT = """
        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0; // Unused in this specific shader, but required by Mesh
        attribute vec2 a_texCoord1; // Holds material ID in .x

        uniform mat4 u_projView;

        varying vec3 v_worldPos;
        varying vec3 v_normal;
        varying float v_matId;

        void main() {
            v_worldPos = a_position;
            v_normal = normalize(a_normal);

            // Unpack material ID, which was encoded as a float in the range [0, 255] in a_texCoord1.x
            // Small offset is added to ensure it rounds correctly when converted back to int in the fragment shader.
            v_matId = (a_texCoord1.x * 255.0) + 0.1;

            gl_Position = u_projView * vec4(a_position, 1.0);
        }
        """;

    /**
     * Rather than using a texture atlas, this shader computes colors
     * procedurally based on the material ID and world position.
     */
    public static final String FRAG = """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec3 v_worldPos;
        varying vec3 v_normal;
        varying float v_matId;

        uniform vec3 u_lightDir;

        // Material Colors
        uniform vec3 u_mat_grass;
        uniform vec3 u_mat_dirt;
        uniform vec3 u_mat_stone;
        uniform vec3 u_mat_wood;
        uniform vec3 u_mat_leaves;
        uniform vec3 u_mat_water;

        // --- NOISE FUNCTIONS ---

        // Simple pseudo-random hash
        float hashf(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        // Value noise for surface variation
        float noiseSmall(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);

            float a = hashf(i);
            float b = hashf(i + vec2(1.0, 0.0));
            float c = hashf(i + vec2(0.0, 1.0));
            float d = hashf(i + vec2(1.0, 1.0));

            vec2 u = f * f * (3.0 - 2.0 * f);

            return mix(a, b, u.x) * (1.0 - u.y) + mix(c, d, u.x) * u.y;
        }

        // --- COLOR LOGIC ---

        vec3 proceduralColor(int id, vec3 pos) {
            // Apply slight noise to coordinate to break tiling
            float small = noiseSmall(pos.xz * 0.6) * 0.08;

            if (id == 1) { // GRASS
                // Mix slightly different greens
                return clamp(u_mat_grass + vec3(small * 0.8, small, small * 0.6), 0.0, 1.0);
            }
            else if (id == 2) { // DIRT
                return clamp(u_mat_dirt + vec3(small * 0.6), 0.0, 1.0);
            }
            else if (id == 3) { // STONE
                return clamp(u_mat_stone + vec3(small - 0.04), 0.0, 1.0);
            }
            else if (id == 4) { // WOOD
                // Create ring-like pattern for wood
                float ring = fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453);
                return clamp(u_mat_wood + vec3(ring * 0.06), 0.0, 1.0);
            }
            else if (id == 5) { // LEAVES
                float n = noiseSmall(pos.xz * 1.2);
                return clamp(u_mat_leaves + vec3(n * 0.12), 0.0, 1.0);
            }
            else if (id == 6) { // WATER
                return clamp(u_mat_water, 0.0, 1.0);
            }

            // Fallback (Magenta for error)
            return vec3(1.0, 0.0, 1.0);
        }

        void main() {
            // Convert interpolated float back to integer ID
            int id = int(v_matId);

            // Get base color
            vec3 col = proceduralColor(id, v_worldPos);

            // Simple diffuse lighting
            float diff = max(dot(normalize(v_normal), normalize(-u_lightDir)), 0.0);

            // Ambient + Diffuse
            vec3 finalColor = col * (0.22 + 0.78 * diff);

            if (id == 6) {
                // Water Special Rendering:
                // Mix with a blueish tint and make it transparent
                finalColor = mix(finalColor, vec3(0.1, 0.3, 0.5), 0.25);
                gl_FragColor = vec4(finalColor, 0.6); // Alpha 0.6
            } else {
                gl_FragColor = vec4(finalColor, 1.0);
            }
        }
        """;
}
