package ee.taltech.examplegame.screen.overlay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;
import ee.taltech.examplegame.network.ServerConnection;
import message.GameLeaveMessage;
import message.PlayerRespawnMessage;

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

        var resumeButton = getButton(20, "Resume", resumeAction::run);
        var respawnButton = getButton(20, "Respawn", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new PlayerRespawnMessage());
            resumeAction.run();
        });
        var exitButton = getButton(20, "Disconnect", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new GameLeaveMessage());
            exitAction.run();
        });

        table.add(resumeButton).padBottom(20).width(200);
        table.row();
        table.add(respawnButton).padBottom(20).width(200);
        table.row();
        table.add(exitButton).width(200);

        stage.addActor(table);
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

    public Stage getStage() {
        return stage;
    }
}
