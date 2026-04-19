package ee.taltech.examplegame.server;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.listener.ServerListener;

import java.io.IOException;

import static constant.Constants.PORT_TCP;
import static constant.Constants.PORT_UDP;
import static network.KryoHelper.registerClasses;

/**
 * Launches the server application.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        try {
            Server server = new Server(16 * 1024 * 1024, 4 * 1024 * 1024);
            // register classes that are sent over the network
            // this must be done for every message that is sent over the network
            registerClasses(server.getKryo());
            server.start();
            server.bind(PORT_TCP, PORT_UDP);

            // this will listen for all connections and messages,
            // that are sent to the server by the clients
            // look in the ServerListener class for more information
            server.addListener(new ServerListener());
        } catch (IOException e) {
            Log.error("Server failed to start.", e);
        }
    }
}
