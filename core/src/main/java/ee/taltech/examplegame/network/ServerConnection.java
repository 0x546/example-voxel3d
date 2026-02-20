package ee.taltech.examplegame.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;

import static constant.Constants.PORT_TCP;
import static constant.Constants.PORT_UDP;
import static constant.Constants.SERVER_IP;
import static network.KryoHelper.registerClasses;

/**
 * Handles the connection to the server.
 * This class is a singleton, meaning that only one instance of this class can exist at a time.
 * More about singletons:
 * <a href="https://javadoc.pages.taltech.ee/design_patterns/creational_patterns.html#singel-singleton">...</a>
 */
public class ServerConnection {
    private static ServerConnection instance;
    private final Client client;

    private ServerConnection() {
        client = new Client();

        // register classes that are sent over the network
        registerClasses(client.getKryo());
    }

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect() {
        if (client.isConnected()) {
            return;
        }

        try {
            client.start();
            client.connect(5000, SERVER_IP, PORT_TCP, PORT_UDP);
        } catch (Exception e) {
            Gdx.app.error("ServerConnection", "Failed to connect to server", e);
        }
    }

    public Client getClient() {
        return client;
    }
}
