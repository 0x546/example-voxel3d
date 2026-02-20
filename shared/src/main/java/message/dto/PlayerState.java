package message.dto;

import lombok.Data;

@Data
public class PlayerState {
    private int id;
    private float x;
    private float y;
    private float z;
    private float yaw;
    private float pitch;
    private int lives;
}
