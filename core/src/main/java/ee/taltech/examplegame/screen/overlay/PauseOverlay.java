package ee.taltech.examplegame.screen.overlay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;

import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import constant.Constants;
import ee.taltech.examplegame.network.ServerConnection;
import message.GameLeaveMessage;
import message.PlayerRespawnMessage;
import lombok.Getter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

@Getter
public class PauseOverlay {

    private final Stage stage;

    public PauseOverlay(Runnable resumeAction, Runnable exitAction) {
        this.stage = new Stage();

        // Background dark overlay
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0.5f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        Image background = new Image(texture);
        background.setFillParent(true);
        stage.addActor(background);

        Table table = new Table();
        table.setFillParent(true);

        // Chunk Distance button
        TextButton[] distanceButtonRef = new TextButton[1];
        var distanceButton = getButton(20, "Chunk Distance: " + getChunkLoadDistance(), () -> {
            int currentDist = getChunkLoadDistance() + 1;
            if (currentDist > 12) currentDist = 1;
            setChunkLoadDistance(currentDist);
            if (distanceButtonRef[0] != null) {
                distanceButtonRef[0].setText("Chunk Distance: " + currentDist);
            }
        });
        distanceButtonRef[0] = distanceButton;

        var resumeButton = getButton(20, "Resume", resumeAction::run);
        var respawnButton = getButton(20, "Respawn", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new PlayerRespawnMessage());
            resumeAction.run();
        });
        var exitButton = getButton(20, "Disconnect", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new GameLeaveMessage());
            exitAction.run();
        });

        table.add(resumeButton).padBottom(20).width(250);
        table.row();
        table.add(respawnButton).padBottom(20).width(250);
        table.row();
        table.add(distanceButton).padBottom(20).width(250);
        table.row();
        table.add(exitButton).width(250);

        stage.addActor(table);
    }

    public static int getChunkLoadDistance() {
        Preferences prefs = Gdx.app.getPreferences("ExampleGamePrefs");
        return prefs.getInteger("chunkDist", Constants.CHUNK_LOAD_DISTANCE);
    }

    private static void setChunkLoadDistance(int distance) {
        Preferences prefs = Gdx.app.getPreferences("ExampleGamePrefs");
        prefs.putInteger("chunkDist", distance);
        prefs.flush();
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
