package ee.taltech.examplegame.shared.message.dto;

import lombok.Data;

@Data
public class PlayerState {
    private int id;
    private float x;
    private float y;
    private float z;
    private float vx;
    private float vy;
    private float vz;
    private float yaw;
    private float pitch;
    private int lives;
}
