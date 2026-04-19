package message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the player's movement intention.
 * Instead of raw keypresses, we send the desired movement axes and rotation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerMovementMessage {
    private float yaw;
    private float pitch;

    // Movement axes (-1.0 to 1.0)
    // 1.0 is forward/right, -1.0 is backward/left
    private float moveForward;
    private float moveSideways;

    private boolean jump;
    private boolean sneak;
    private boolean fly;
}
