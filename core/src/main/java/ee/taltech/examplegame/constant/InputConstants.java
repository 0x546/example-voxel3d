package ee.taltech.examplegame.constant;

import com.badlogic.gdx.Input;

public class InputConstants {
    private InputConstants() {
        /* This utility class should not be instantiated */
    }

    public static final int KEY_FORWARD = Input.Keys.W;
    public static final int KEY_BACKWARD = Input.Keys.S;
    public static final int KEY_LEFT = Input.Keys.A;
    public static final int KEY_RIGHT = Input.Keys.D;
    public static final int KEY_JUMP = Input.Keys.SPACE;
    public static final int KEY_SNEAK = Input.Keys.SHIFT_LEFT;
    public static final int KEY_FLY = Input.Keys.F;
    public static final int KEY_PAUSE = Input.Keys.ESCAPE;
    public static final int MOUSE_ACTION = Input.Buttons.LEFT;
}
