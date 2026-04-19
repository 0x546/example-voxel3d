package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.graphics.Color;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.Font;
import message.GameJoinMessage;
import message.GenerateWorldMessage;
import message.ServerStatusRequestMessage;
import message.ServerStatusResponseMessage;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * TitleScreen represents the main menu of the game, where players can choose to start the game or exit.
 * It listens for user input and directs user to different screens, for example TitleScreen -> GameScreen.
 */
public class TitleScreen extends ScreenAdapter {
    private final Stage stage;
    private final Label statusLabel;
    private float pollTimer = 0f;
    private boolean lastInstanceRunning = false;
    private int lastPlayerCount = -1;
    private final TextButton continueButton;
    private final TextButton newGameButton;
    private final Listener networkListener;

    public TitleScreen(Game game) {
        stage = new Stage();

        // NB! important line - will make the stage listen for user input
        // For example when this is not set no hover or click events will be triggered
        Gdx.input.setInputProcessor(stage);

        ServerConnection.getInstance().connect();

        statusLabel = new Label("Connecting to server...", new Label.LabelStyle(Font.getPixelFont(20), Color.BLACK));

        continueButton = getButton(20, "Join Game", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new GameJoinMessage());
            game.setScreen(new VoxelScreen(game));
        });

        newGameButton = getButton(20, "New Game", () -> {
            ServerConnection.getInstance().getClient().sendTCP(new GenerateWorldMessage());
            game.setScreen(new VoxelScreen(game));
        });

        var exitButton = getButton(20, "Exit", () -> Gdx.app.exit());

        // positioning the buttons. you can think of the following as a table (or flexbox) in HTML
        var table = new Table();
        table.setFillParent(true);
        table.add(statusLabel).padBottom(20).row();
        table.add(exitButton);

        stage.addActor(table);

        networkListener = new Listener() {
            @Override
            public void connected(Connection connection) {
                connection.sendTCP(new ServerStatusRequestMessage());
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof ServerStatusResponseMessage response) {
                    Gdx.app.postRunnable(() -> updateUI(response, table, exitButton));
                }
            }
        };
        ServerConnection.getInstance().getClient().addListener(networkListener);

        // If already connected, manual request
        if (ServerConnection.getInstance().getClient().isConnected()) {
            ServerConnection.getInstance().getClient().sendTCP(new ServerStatusRequestMessage());
        }
    }

    private void updateUI(ServerStatusResponseMessage response, Table table, TextButton exitButton) {
        if (response.isInstanceRunning() == lastInstanceRunning
            && response.getPlayerCount() == lastPlayerCount) {
            return; // No UI change needed
        }
        lastInstanceRunning = response.isInstanceRunning();
        lastPlayerCount = response.getPlayerCount();

        table.clearChildren();
        table.add(statusLabel).padBottom(20).row();

        if (response.isInstanceRunning()) {
            statusLabel.setText("Players in game: " + response.getPlayerCount());
            table.add(continueButton).padBottom(20).row();

            if (response.getPlayerCount() == 0) {
                newGameButton.setText("Reset World");
                table.add(newGameButton).padBottom(20).row();
            }
        } else {
            statusLabel.setText("No active instance.");
            newGameButton.setText("New Game");
            table.add(newGameButton).padBottom(20).row();
        }

        table.add(exitButton);
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

        pollTimer += delta;
        if (pollTimer >= 1.0f) {
            pollTimer = 0f;
            if (ServerConnection.getInstance().getClient().isConnected()) {
                ServerConnection.getInstance().getClient().sendTCP(new ServerStatusRequestMessage());
            }
        }

        // draw the buttons
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        ServerConnection.getInstance().getClient().removeListener(networkListener);
        stage.dispose();
        super.dispose();
    }
}
