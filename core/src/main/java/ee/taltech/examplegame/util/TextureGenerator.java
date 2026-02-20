package ee.taltech.examplegame.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class TextureGenerator {
    private TextureGenerator() {
        /* This utility class should not be instantiated */
    }

    public static Texture generateNoiseTexture(int width, int height, Color baseColor) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float noise = MathUtils.random(-0.1f, 0.1f);
                Color c = new Color(baseColor);
                c.r = MathUtils.clamp(c.r + noise, 0f, 1f);
                c.g = MathUtils.clamp(c.g + noise, 0f, 1f);
                c.b = MathUtils.clamp(c.b + noise, 0f, 1f);

                pixmap.drawPixel(x, y, Color.rgba8888(c));
            }
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
