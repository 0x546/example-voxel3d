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
        uniform int u_underwater;
        uniform float u_waterLevel;
        uniform float u_time;
        uniform float u_waveAmp;
        uniform float u_waveSpeed;

        // Material Colors
        uniform vec3 u_mat_grass;
        uniform vec3 u_mat_dirt;
        uniform vec3 u_mat_stone;
        uniform vec3 u_mat_wood;
        uniform vec3 u_mat_leaves;
        uniform vec3 u_mat_water;
        uniform vec3 u_mat_sand;
        uniform vec3 u_mat_brick;
        uniform vec3 u_mat_glass;

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

        vec3 proceduralColor(int id, vec3 pos, vec3 normal) {
            vec2 uv;
            if (abs(normal.y) > 0.5) uv = pos.xz;
            else if (abs(normal.x) > 0.5) uv = vec2(pos.z, pos.y);
            else uv = pos.xy;

            // Pixelated blocky noise for defined texture look
            vec2 pixelUv = floor(uv * 16.0);
            float bn = hashf(pixelUv);

            if (id == 1) { // GRASS
                float grit = (bn - 0.5) * 0.15;
                vec3 col = u_mat_grass;

                if (abs(normal.y) < 0.5) { // side faces
                    float localY = fract(pos.y);
                    float hCoord = (abs(normal.x) > 0.5) ? pos.z : pos.x;
                    float hPixel = floor(hCoord * 16.0);
                    float edgeNoise = hashf(vec2(hPixel, 0.0));
                    float marginEnd = 0.5 + edgeNoise * 0.3;
                    if (localY < marginEnd) {
                        col = u_mat_dirt;
                    } else if (localY < marginEnd + 0.0625) {
                        col *= 0.8; // subtle drop shadow
                    }
                } else if (normal.y < -0.5) {
                    col = u_mat_dirt; // bottom face
                }
                return clamp(col + vec3(grit), 0.0, 1.0);
            }
            else if (id == 2) { // DIRT
                float grit = (bn - 0.5) * 0.2;
                return clamp(u_mat_dirt + vec3(grit), 0.0, 1.0);
            }
            else if (id == 3) { // STONE
                float grit = (bn - 0.5) * 0.25;
                // Add a few subtle larger patterns
                float vein = hashf(floor(uv * 4.0));
                grit += (vein - 0.5) * 0.15;
                return clamp(u_mat_stone + vec3(grit), 0.0, 1.0);
            }
            else if (id == 4) { // WOOD
                if (abs(normal.y) > 0.5) {
                    vec2 uv = pos.xz;

                    vec2 localUV = fract(uv);
                    float dist = length(localUV - vec2(0.5));
                    float ring = fract(dist * 8.0 + noiseSmall(pos.xz * 4.0) * 0.3);
                    return clamp(u_mat_wood + vec3(ring * 0.08), 0.0, 1.0);
                } else {
                    float bark = noiseSmall(vec2(uv.x * 12.0, uv.y * 1.5));
                    float dark = noiseSmall(vec2(uv.x * 5.0, uv.y * 0.5));
                    return clamp(u_mat_wood * (0.7 + dark * 0.3) + vec3(bark * 0.15), 0.0, 1.0);
                }
            }
            else if (id == 5) { // LEAVES
                float n = noiseSmall(pos.xz * 1.2);
                return clamp(u_mat_leaves + vec3(n * 0.12), 0.0, 1.0);
            }
            else if (id == 6) { // WATER
                return clamp(u_mat_water, 0.0, 1.0);
            }
            else if (id == 8) { // SAND
                float s = noiseSmall(pos.xz * 3.1) * 0.05;
                return clamp(u_mat_sand + vec3(s), 0.0, 1.0);
            }
            else if (id == 9) { // BRICK
                vec2 uv;
                if (abs(normal.y) > 0.5) uv = pos.xz;
                else if (abs(normal.x) > 0.5) uv = vec2(pos.z, pos.y);
                else uv = pos.xy;

                // Slightly offset position to prevent z-fighting at exact block boundaries
                vec2 safeUV = uv + 0.001;

                vec2 brickUv = safeUV * vec2(2.0, 4.0);
                // Offset alternative rows
                float rowOffset = step(0.5, fract(brickUv.y * 0.5)) * 0.5;
                brickUv.x += rowOffset;

                vec2 bUV = fract(brickUv);

                float mortar = step(bUV.x, 0.05) + step(bUV.y, 0.1);
                mortar = clamp(mortar, 0.0, 1.0);

                float bNoise = noiseSmall(safeUV * 10.0) * 0.1;
                vec3 brickColor = u_mat_brick + vec3(bNoise);
                vec3 mortarColor = vec3(0.6, 0.6, 0.6) + vec3(bNoise);

                return clamp(mix(brickColor, mortarColor, mortar), 0.0, 1.0);
            }
            else if (id == 10) { // GLASS
                vec2 localUv = fract(uv);
                // Framed border and diagonal shine
                float frame = step(localUv.x, 0.0625) + step(1.0 - 0.0625, localUv.x) +
                              step(localUv.y, 0.0625) + step(1.0 - 0.0625, localUv.y);
                float highlight = step(localUv.x - localUv.y, -0.6) * step(localUv.x, 0.3);
                vec3 col = u_mat_glass + vec3(clamp(frame + highlight, 0.0, 1.0) * 0.6);
                return clamp(col, 0.0, 1.0);
            }

            // Fallback (Magenta for error)
            return vec3(1.0, 0.0, 1.0);
        }

        void main() {
            // Convert interpolated float back to integer ID
            int id = int(v_matId);

            // Get base color
            vec3 col = proceduralColor(id, v_worldPos, normalize(v_normal));

            // Simple diffuse lighting
            float diff = max(dot(normalize(v_normal), normalize(-u_lightDir)), 0.0);

            // Ambient + Diffuse
            vec3 finalColor = col * (0.22 + 0.78 * diff);

            if (id == 6) {
                // Water Special Rendering:
                // Mix with a blueish tint and make it transparent
                finalColor = mix(finalColor, vec3(0.1, 0.3, 0.5), 0.25);
                gl_FragColor = vec4(finalColor, 0.6); // Alpha 0.6
            } else if (id == 10) {
                // Glass Special Rendering
                gl_FragColor = vec4(finalColor, 0.4);
            } else {
                // --- Underwater tinting for solid blocks ---
                // Calculate wave height at this horizontal position
                float t = u_time * u_waveSpeed;
                float w1 = sin(v_worldPos.x * 1.8 + t) * u_waveAmp;
                float w2 = sin(v_worldPos.z * 2.3 + t * 0.7 + 1.3) * u_waveAmp * 0.5;
                float w3 = sin((v_worldPos.x + v_worldPos.z) * 3.7 + t * 1.13 + 2.7) * u_waveAmp * 0.3;
                float surfaceY = u_waterLevel - 0.12 + w1 + w2 + w3;

                // Apply tint if fragment is below water surface
                if (v_worldPos.y < surfaceY) {
                    finalColor = mix(finalColor, u_mat_water, 0.35);
                }
                gl_FragColor = vec4(finalColor, 1.0);
            }
        }
        """;

    // ===========================================================================
    //  Water-specific shaders – animated waves, Fresnel, specular
    // ===========================================================================

    /**
     * Water vertex shader: displaces Y with two overlapping sine waves,
     * passes world position, perturbed normal, and camera-space depth
     * to the fragment shader.
     */
    public static final String WATER_VERT = """
        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0;
        attribute vec2 a_texCoord1;

        uniform mat4 u_projView;
        uniform float u_time;
        uniform float u_waveAmp;
        uniform float u_waveSpeed;

        varying vec3 v_worldPos;
        varying vec3 v_normal;
        varying float v_depth;

        // Per-position hash for breaking regularity
        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        void main() {
            vec3 pos = a_position;
            vec3 norm = a_normal;

            // Only displace TOP faces (normal pointing up) — side/bottom faces
            // stay in place so the water-dirt boundary doesn't vibrate.
            if (a_normal.y > 0.5) {
                // Lower the base surface by 12% so wave peaks stay within the block
                pos.y -= 0.12;

                // Three overlapping sine waves at irrational frequency ratios
                float t = u_time * u_waveSpeed;
                float wave1 = sin(pos.x * 1.8 + t) * u_waveAmp;
                float wave2 = sin(pos.z * 2.3 + t * 0.7 + 1.3) * u_waveAmp * 0.5;
                float wave3 = sin((pos.x + pos.z) * 3.7 + t * 1.13 + 2.7) * u_waveAmp * 0.3;

                // Hash-based jitter per block to break repeating pattern
                float jitter = (hash(floor(pos.xz)) - 0.5) * u_waveAmp * 0.4;

                pos.y += wave1 + wave2 + wave3 + jitter;

                // Perturb normal from wave derivatives
                float dx = cos(pos.x * 1.8 + t) * 1.8 * u_waveAmp
                         + cos((pos.x + pos.z) * 3.7 + t * 1.13 + 2.7) * 3.7 * u_waveAmp * 0.3;
                float dz = cos(pos.z * 2.3 + t * 0.7 + 1.3) * 2.3 * u_waveAmp * 0.5
                         + cos((pos.x + pos.z) * 3.7 + t * 1.13 + 2.7) * 3.7 * u_waveAmp * 0.3;
                norm = normalize(vec3(-dx, 1.0, -dz));
            }

            v_worldPos = pos;
            v_normal   = norm;

            vec4 clipPos = u_projView * vec4(pos, 1.0);
            v_depth = clipPos.z;

            gl_Position = clipPos;
        }
        """;

    /**
     * Water fragment shader: Fresnel transparency, specular sun highlights,
     * animated color variation, separate tinting when viewed from underwater.
     */
    public static final String WATER_FRAG = """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec3 v_worldPos;
        varying vec3 v_normal;
        varying float v_depth;

        uniform vec3 u_lightDir;
        uniform vec3 u_cameraPos;
        uniform float u_time;
        uniform int u_underwater;
        uniform vec3 u_mat_water;

        // Simple hash for shimmer
        float hashf(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            float a = hashf(i);
            float b = hashf(i + vec2(1.0, 0.0));
            float c = hashf(i + vec2(0.0, 1.0));
            float d = hashf(i + vec2(1.0, 1.0));
            vec2 u = f * f * (3.0 - 2.0 * f);
            return mix(a, b, u.x) * (1.0 - u.y) + mix(c, d, u.x) * u.y;
        }

        void main() {
            vec3 N = normalize(v_normal);
            vec3 V = normalize(u_cameraPos - v_worldPos);
            vec3 L = normalize(-u_lightDir);

            // When underwater, only render top faces (water-air boundary).
            // Discard side/bottom faces to prevent z-fighting with solid blocks.
            if (u_underwater == 1 && N.y < 0.3) {
                discard;
            }

            // --- Base water color with animated variation ---
            float n1 = noise(v_worldPos.xz * 0.5 + u_time * 0.15);
            float n2 = noise(v_worldPos.xz * 1.2 - u_time * 0.08);
            vec3 deepColor   = u_mat_water * 0.6;
            vec3 shallowColor = u_mat_water + vec3(0.05, 0.15, 0.1);
            vec3 baseColor = mix(deepColor, shallowColor, n1 * 0.5 + 0.5);
            baseColor += (n2 - 0.5) * 0.06;

            // --- Diffuse lighting ---
            float diff = max(dot(N, L), 0.0);
            vec3 litColor = baseColor * (0.3 + 0.7 * diff);

            // --- Specular highlight (Blinn-Phong) ---
            vec3 H = normalize(L + V);
            float spec = pow(max(dot(N, H), 0.0), 64.0);
            litColor += vec3(1.0, 0.95, 0.8) * spec * 0.7;

            // --- Fresnel-based alpha ---
            float fresnel = 1.0 - max(dot(N, V), 0.0);
            fresnel = pow(fresnel, 2.0);
            float alpha = mix(0.35, 0.85, fresnel);

            if (u_underwater == 1) {
                // Viewed from below: lighter tint, softer alpha
                litColor = mix(litColor, vec3(0.15, 0.4, 0.5), 0.3);
                alpha = mix(0.4, 0.7, fresnel);
            }

            // Subtle edge darkening based on depth
            float depthFade = clamp(v_depth * 0.005, 0.0, 0.3);
            litColor *= (1.0 - depthFade);

            gl_FragColor = vec4(litColor, alpha);
        }
        """;
}
