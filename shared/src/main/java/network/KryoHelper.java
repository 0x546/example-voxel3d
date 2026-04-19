package network;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;

public class KryoHelper {
    private KryoHelper() {
        /* This utility class should not be instantiated */
    }

    public static void registerClasses(Kryo kryo) {
        // all classes that you want to send over the network
        // must be registered here.
        kryo.register(message.GameJoinMessage.class);
        kryo.register(ArrayList.class);
        kryo.register(message.GameStateMessage.class);
        kryo.register(message.dto.PlayerState.class);
        kryo.register(message.PlayerMovementMessage.class);
        kryo.register(message.GameLeaveMessage.class);
        kryo.register(message.PlayerRespawnMessage.class);

        kryo.register(ee.taltech.examplegame.shared.world.Chunk.class);
        kryo.register(int[][][].class);
        kryo.register(int[][].class);
        kryo.register(int[].class);
        kryo.register(message.ChunkDataMessage.class);
        kryo.register(message.ChunkRequestMessage.class);
        kryo.register(message.GenerateWorldMessage.class);
    }
}
