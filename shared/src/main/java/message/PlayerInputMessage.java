package message;

import lombok.Data;

@Data
public class PlayerInputMessage {
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sneak;
    private float yaw;
    private float pitch;
}
