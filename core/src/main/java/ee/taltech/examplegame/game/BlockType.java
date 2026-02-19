package ee.taltech.examplegame.game;

import com.badlogic.gdx.graphics.Color;

public enum BlockType {
    AIR(0, Color.CLEAR),
    GRASS(1, Color.GREEN),
    DIRT(2, new Color(0.4f, 0.2f, 0, 1)),
    STONE(3, Color.GRAY),
    WOOD(4, new Color(0.5f, 0.3f, 0.1f, 1)),
    LEAVES(5, Color.FOREST);

    public final int id;
    public final Color color;

    BlockType(int id, Color color) {
        this.id = id;
        this.color = color;
    }

    public static BlockType getById(int id) {
        for (BlockType type : values()) {
            if (type.id == id)
                return type;
        }
        return AIR;
    }
}
