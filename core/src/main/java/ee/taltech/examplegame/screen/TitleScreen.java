package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import ee.taltech.examplegame.network.ServerConnection;
import message.GameJoinMessage;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;

/**
 * TitleScreen represents the main menu of the game, where players can choose to start the game or exit.
 * It listens for user input and directs user to different screens, for example TitleScreen -> GameScreen.
 */
public class TitleScreen extends ScreenAdapter {
    private final Stage stage;

    public TitleScreen(Game game) {
        stage = new Stage();

        // NB! important line - will make the stage listen for user input
        // For example when this is not set no hover or click events will be triggered
        Gdx.input.setInputProcessor(stage);

        // menu buttons
        var startButton = getButton(20, "Start 2D", () -> {
            // send a message to the server that the player wants to join the game
            ServerConnection.getInstance().getClient().sendTCP(new GameJoinMessage());
            game.setScreen(new GameScreen(game));
        });
        var startVoxelButton = getButton(20, "Voxel Demo", () -> {
            game.setScreen(new VoxelScreen(game));
        });
        var exitButton = getButton(20, "Exit", () -> Gdx.app.exit());

        // positioning the buttons. you can think of the following as a table (or flexbox) in HTML
        var table = new Table();
        table.setFillParent(true);
        table.add(startButton).padBottom(20);
        table.row();
        table.add(startVoxelButton).padBottom(20);
        table.row();
        table.add(exitButton);

        stage.addActor(table);
    }

    /**
     * Renders the TitleScreen, clearing it and drawing the buttons.
     *
     * @param delta time since last frame.
     */
    @Override
    public void render(float delta) {
        super.render(delta);
        // clear the screen
        Gdx.gl.glClearColor(192 / 255f, 192 / 255f, 192 / 255f, 1); // to get that win 95 look
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // draw the buttons
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
    }
}
