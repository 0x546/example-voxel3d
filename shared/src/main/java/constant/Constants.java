package constant;

public class Constants {
    private Constants() {
        /* This utility class should not be instantiated */
    }

    // --- networking constants ---
    public static final int PORT_TCP = 54555;
    public static final int PORT_UDP = 54777;
    // this should be changed depending on where the server is hosted
    public static final String SERVER_IP = "localhost";

    // --- physical player constants ---
    public static final float GRAVITY = 25f;
    public static final float JUMP_VELOCITY = 10f;
    public static final float MOVE_SPEED = 16f;
    public static final float DAMPING = 0.9f;
    public static final float WATER_GRAVITY = 6f;        // much slower sinking
    public static final float SWIM_UP_SPEED = 3f;        // space bar ascent
    public static final float WATER_MOVE_SPEED = 12f;     // slower horizontal movement
    public static final float WATER_DAMPING = 0.85f;     // stronger drag in water
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float EYE_HEIGHT = 1.6f; // Camera height from feet
    public static final float PLAYER_SPAWN_X = 12f;
    public static final float PLAYER_SPAWN_Y = 25f;
    public static final float PLAYER_SPAWN_Z = 12f;
    public static final float VOID_LEVEL = -10f;

    // --- world generation constants ---
    public static final int WORLD_WIDTH = 24;
    public static final int WORLD_HEIGHT = 32;
    public static final int WORLD_DEPTH = 24;
    public static final int WATER_LEVEL = 16;
    public static final int DIRT_LAYER_THICKNESS = 3;

    // --- tree generation constants ---
    public static final int TRUNK_HEIGHT = 4;
    public static final int LEAVES_START_OFFSET = 1;
    public static final int LEAVES_TOP_OFFSET = 1;
    public static final int LEAF_RADIUS = 2;
    public static final int CLEAR_RADIUS = 2;
    public static final float PLANT_CHANCE = 0.05f;

    // --- game constants ---
    public static final int GAME_TICK_RATE = 60;
    public static final int PLAYER_COUNT_IN_GAME = 4;
    public static final int PLAYER_LIVES_COUNT = 50;

    // --- shooting constants ---
    public static final float BULLET_SPEED = 40f;
    public static final long BULLET_TIMEOUT_IN_MILLIS = 500L;

    // --- rendering constants ---
    public static final float MOUSE_SENSITIVITY = 0.2f;
    public static final float CAMERA_NEAR = 0.1f;
    public static final float CAMERA_FAR = 300f;
    public static final float CAMERA_FOV = 90f;

    // --- water shader constants ---
    public static final float WATER_WAVE_AMPLITUDE = 0.05f;
    public static final float WATER_WAVE_SPEED = 0.5f;

    // --- voxel mesh builder constants ---
    public static final int MAX_VERTICES = 32000;
    public static final int FLOATS_PER_VERTEX = 10;
    public static final float MAT_ID_ENCODING_FACTOR = 255f;

    // --- player sync constants ---
    public static final float PLAYER_INTERPOLATION_SPEED = 30f;
    public static final float PLAYER_SNAP_DISTANCE = 1.0f;

}
