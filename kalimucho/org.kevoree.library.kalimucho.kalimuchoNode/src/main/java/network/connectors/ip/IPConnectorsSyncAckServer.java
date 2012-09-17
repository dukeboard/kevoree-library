package network.connectors.ip;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import util.Parameters;
import java.io.IOException;
import util.NetworkAddress;
import model.interfaces.network.INetworkServer;

/**
 * Receives on IP the replies to a sync message of a new creeated connector.
 *
 * @author Dalmau
 */

// Classe des threads utilises par les connecteurs qui emettent par IP

public class IPConnectorsSyncAckServer extends Thread implements INetworkServer {
    private ServerSocket conn;
    private boolean enMarche;

    /**
     * Create the thread that sends data for all connectors
     * @param adr  address on which this server runs
     */
    public IPConnectorsSyncAckServer(NetworkAddress adr) {
        try { // essayer d'etablir la connexion IP
            conn = new ServerSocket();
            conn.bind(new InetSocketAddress(adr.getNormalizedAddress(), Parameters.PORT_IP_ACK_CONNECTORS));
        }
        catch (IOException e) {
            System.err.println("Can't open connection for connectors' ACK service");
        }
        start();
    }

    /**
     * The server waits for sync messages of new created connectors.<br>
     * When a sunc message is received, creates a server to receive acknowledgments
     * for the new connector.
     */
    @Override
    public void run() {
        // Attend des connexions de clients (synchro) lance un thread de dialogue avec le client
        enMarche = true;
        while (enMarche) {
            try {
                if (conn != null) {
                    Socket dialogue = conn.accept();
                    new IPConnectorACKReceptor(dialogue);
                }
            }
            catch (SecurityException ioe) {
                if (enMarche) System.err.println("IP connectors error when receiving ACK message");
           }
            catch (IOException ioe) {
                if (enMarche) System.err.println("IP connectors error when receiving ACK message");
           }
        }
    }

    /**
     * Stops the server for receiving sync messages for connectors
     *  (when a connector is removed).
     */
    public void stopThread() {
        // Arret du serveur de reception des messages de synchro des connecteurs
        enMarche = false;
        try { conn.close(); }
        catch (IOException ioe) {
            System.err.println("Can't stop server for connectors' synchronisation");
        }
    }

}
