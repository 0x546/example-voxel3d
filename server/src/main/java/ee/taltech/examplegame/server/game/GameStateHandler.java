package ee.taltech.examplegame.server.game;

import ee.taltech.examplegame.server.game.object.Player;
import lombok.Getter;
import lombok.Setter;
import ee.taltech.examplegame.shared.message.GameStateMessage;
import ee.taltech.examplegame.shared.message.dto.PlayerState;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GameStateHandler {

    private boolean allPlayersHaveJoined = false;
    private float gameTime = 0;

    public void incrementGameTimeIfPlayersPresent(float delta) {
        if (allPlayersHaveJoined) {
            gameTime += delta;
        }
    }

    public GameStateMessage getGameStateMessage(List<Player> players) {
        // get the state of all players
        var playerStates = new ArrayList<PlayerState>();
        players.forEach(player -> playerStates.add(player.getState()));

        // construct gameStateMessage
        var gameStateMessage = new GameStateMessage();
        gameStateMessage.setPlayerStates(playerStates);
        gameStateMessage.setGameTime(Math.round(gameTime));
        gameStateMessage.setAllPlayersHaveJoined(allPlayersHaveJoined);

        return gameStateMessage;
    }


}
