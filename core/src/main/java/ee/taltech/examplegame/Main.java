package ee.taltech.examplegame;

import com.badlogic.gdx.Game;

import ee.taltech.examplegame.screen.TitleScreen;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {

    @Override
    public void create() {
        // display the title screen to the user
        setScreen(new TitleScreen(this));
    }
}
