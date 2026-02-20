package network;

import com.esotericsoftware.kryo.Kryo;
import message.PlayerShootingMessage;
import message.dto.BulletState;
import message.dto.Direction;
import message.dto.PlayerState;

import java.util.ArrayList;

public class KryoHelper {
    private KryoHelper() {
        /* This utility class should not be instantiated */
    }

    public static void registerClasses(Kryo kryo) {
        // all classes that you want to send over the network
        // must be registered here. To make the handling of these classes
        // easier they are all stored in the "messages" package
        kryo.register(message.GameJoinMessage.class);
        kryo.register(message.PlayerMovementMessage.class);
        kryo.register(Direction.class);
        kryo.register(ArrayList.class);
        kryo.register(message.GameStateMessage.class);
        kryo.register(PlayerState.class);
        kryo.register(BulletState.class);
        kryo.register(PlayerShootingMessage.class);
        kryo.register(message.PlayerInputMessage.class);
    }
}
