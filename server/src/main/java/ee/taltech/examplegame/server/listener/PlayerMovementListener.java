package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.examplegame.server.game.object.Player;
import message.PlayerInputMessage;

public class PlayerMovementListener extends Listener {
    private final Player player;

    public PlayerMovementListener(Player player) {
        this.player = player;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof PlayerInputMessage message) {
            player.handleInput(message);
        }
    }
}
