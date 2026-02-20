package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import ee.taltech.examplegame.network.ServerConnection;
import message.PlayerMovementMessage;

/**
 * Handles gathering user input and sending movement messages to the server.
 * Optimizes network usage by only sending updates when the input state actually changes.
 */
public class PlayerInputManager {

    private float lastYaw = -1;
    private float lastPitch = -1;
    private float lastForward = 0;
    private float lastSideways = 0;
    private boolean lastJump = false;
    private boolean lastSneak = false;

    // Small threshold for rotation changes to avoid spamming tiny mouse movements
    private static final float ROTATION_THRESHOLD = 0.01f;

    public void update(float yaw, float pitch) {
        float moveForward = 0;
        float moveSideways = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveForward += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveForward -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveSideways -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveSideways += 1;

        boolean jump = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean sneak = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        // Check if anything changed
        boolean rotationChanged = Math.abs(yaw - lastYaw) > ROTATION_THRESHOLD || 
                                 Math.abs(pitch - lastPitch) > ROTATION_THRESHOLD;
        boolean movementChanged = moveForward != lastForward || 
                                 moveSideways != lastSideways || 
                                 jump != lastJump || 
                                 sneak != lastSneak;

        if (rotationChanged || movementChanged) {
            PlayerMovementMessage message = new PlayerMovementMessage(
                yaw, pitch, moveForward, moveSideways, jump, sneak
            );
            
            ServerConnection.getInstance().getClient().sendUDP(message);

            // Update last state
            lastYaw = yaw;
            lastPitch = pitch;
            lastForward = moveForward;
            lastSideways = moveSideways;
            lastJump = jump;
            lastSneak = sneak;
        }
    }
}
