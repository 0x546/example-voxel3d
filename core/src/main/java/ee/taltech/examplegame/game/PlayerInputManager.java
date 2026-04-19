package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;

import ee.taltech.examplegame.constant.InputConstants;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.shared.message.PlayerMovementMessage;
import lombok.Getter;

/**
 * Handles gathering user input and sending movement messages to the server.
 * Optimizes network usage by only sending updates when the input state actually changes.
 */
public class PlayerInputManager {

    private long lastSendTime = 0;
    private static final long SEND_INTERVAL_MS = 100;

    private float lastYaw = -1;
    private float lastPitch = -1;
    private float lastForward = 0;
    private float lastSideways = 0;
    private boolean lastJump = false;
    private boolean lastSneak = false;
    private boolean lastFly = false;

    // Exposed current state
    @Getter
    private float moveForward = 0;
    @Getter
    private float moveSideways = 0;
    @Getter
    private boolean jump = false;
    @Getter
    private boolean sneak = false;
    @Getter
    private boolean fly = false;
    @Getter
    private boolean pausePressed = false;
    @Getter
    private boolean actionPressed = false;

    // Small threshold for rotation changes to avoid spamming tiny mouse movements
    private static final float ROTATION_THRESHOLD = 0.01f;

    public void update(float yaw, float pitch) {
        // Check if anything changed
        boolean rotationChanged = Math.abs(yaw - lastYaw) > ROTATION_THRESHOLD ||
                                 Math.abs(pitch - lastPitch) > ROTATION_THRESHOLD;
        boolean movementChanged = moveForward != lastForward ||
                                 moveSideways != lastSideways ||
                                 jump != lastJump ||
                                 sneak != lastSneak ||
                                 fly != lastFly;

        long currentTime = System.currentTimeMillis();
        boolean intervalElapsed = currentTime - lastSendTime > SEND_INTERVAL_MS;

        if (rotationChanged || movementChanged || intervalElapsed) {
            PlayerMovementMessage message = new PlayerMovementMessage(
                yaw, pitch, moveForward, moveSideways, jump, sneak, fly
            );

            ServerConnection.getInstance().getClient().sendUDP(message);

            // Update last state
            lastYaw = yaw;
            lastPitch = pitch;
            lastForward = moveForward;
            lastSideways = moveSideways;
            lastJump = jump;
            lastSneak = sneak;
            lastFly = fly;
            lastSendTime = currentTime;
        }
    }

    public void updateNoSend(boolean paused) {
        gatherInput(paused);
    }

    public void stopMovement(float yaw, float pitch) {
        moveForward = 0;
        moveSideways = 0;
        jump = false;
        sneak = false;

        // Update last state to prevent immediate re-send upon unpausing if keys are still held
        lastForward = 0;
        lastSideways = 0;
        lastJump = false;
        lastSneak = false;
        lastYaw = yaw;
        lastPitch = pitch;

        PlayerMovementMessage message = new PlayerMovementMessage(
            yaw, pitch, 0, 0, false, false, fly
        );
        ServerConnection.getInstance().getClient().sendUDP(message);
    }

    private void gatherInput(boolean paused) {
        moveForward = 0;
        moveSideways = 0;
        jump = false;
        sneak = false;

        if (!paused) {
            if (Gdx.input.isKeyJustPressed(InputConstants.KEY_FLY)) {
                fly = !fly;
            }

            if (Gdx.input.isKeyPressed(InputConstants.KEY_FORWARD)) moveForward += 1;
            if (Gdx.input.isKeyPressed(InputConstants.KEY_BACKWARD)) moveForward -= 1;
            if (Gdx.input.isKeyPressed(InputConstants.KEY_LEFT)) moveSideways -= 1;
            if (Gdx.input.isKeyPressed(InputConstants.KEY_RIGHT)) moveSideways += 1;

            jump = Gdx.input.isKeyPressed(InputConstants.KEY_JUMP);
            sneak = Gdx.input.isKeyPressed(InputConstants.KEY_SNEAK);
        }

        pausePressed = Gdx.input.isKeyJustPressed(InputConstants.KEY_PAUSE);
        actionPressed = Gdx.input.isButtonJustPressed(InputConstants.MOUSE_ACTION);
    }
}
