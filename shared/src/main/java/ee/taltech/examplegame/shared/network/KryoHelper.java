package ee.taltech.examplegame.shared.network;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import ee.taltech.examplegame.shared.message.BlockChangeMessage;
import ee.taltech.examplegame.shared.message.ChunkDataMessage;
import ee.taltech.examplegame.shared.message.ChunkRequestMessage;
import ee.taltech.examplegame.shared.message.GameJoinMessage;
import ee.taltech.examplegame.shared.message.GameLeaveMessage;
import ee.taltech.examplegame.shared.message.GameStateMessage;
import ee.taltech.examplegame.shared.message.GenerateWorldMessage;
import ee.taltech.examplegame.shared.message.PlayerMovementMessage;
import ee.taltech.examplegame.shared.message.PlayerRespawnMessage;
import ee.taltech.examplegame.shared.message.ServerStatusRequestMessage;
import ee.taltech.examplegame.shared.message.ServerStatusResponseMessage;
import ee.taltech.examplegame.shared.message.dto.PlayerState;

public class KryoHelper {
    private KryoHelper() {
        /* This utility class should not be instantiated */
    }

    public static void registerClasses(Kryo kryo) {
        // all classes that you want to send over the network
        // must be registered here.
        kryo.register(GameJoinMessage.class);
        kryo.register(ArrayList.class);
        kryo.register(GameStateMessage.class);
        kryo.register(PlayerState.class);
        kryo.register(PlayerMovementMessage.class);
        kryo.register(GameLeaveMessage.class);
        kryo.register(PlayerRespawnMessage.class);

        kryo.register(ee.taltech.examplegame.shared.world.Chunk.class);
        kryo.register(int[][][].class);
        kryo.register(int[][].class);
        kryo.register(int[].class);
        kryo.register(ChunkDataMessage.class);
        kryo.register(ChunkRequestMessage.class);
        kryo.register(GenerateWorldMessage.class);
        kryo.register(BlockChangeMessage.class);
        kryo.register(ServerStatusRequestMessage.class);
        kryo.register(ServerStatusResponseMessage.class);
    }
}
