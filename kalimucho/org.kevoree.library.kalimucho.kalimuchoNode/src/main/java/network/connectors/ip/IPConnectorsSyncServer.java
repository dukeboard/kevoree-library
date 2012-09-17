package network.connectors.ip;

/**
 *
 * @author Dalmau
 */
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import util.NetworkAddress;
import model.interfaces.network.INetworkServer;

// Classe des threads assurant les receptions reseau pour les connecteurs deportes

/**
 * Server that receive on IP the SYNC message of a newly created connector.
 *
 * @author Dalmau
 */
public class IPConnectorsSyncServer extends Thread implements INetworkServer {
    private ServerSocket conn=null; // connexion par socket
    private boolean enMarche;
    private IPConnectorDataReceptor dataReceptor;
    /**
     * Create the server and the mailbox manager for connectors incomming data.
     * @param adr  address on which this server runs
     * @param port port number used by this server
     */
    public IPConnectorsSyncServer(NetworkAddress adr, int port) {
        dataReceptor = null;
        try { // ouverture de connexion IP
            conn = new ServerSocket();
            conn.bind(new InetSocketAddress(adr.getNormalizedAddress(), port));
        }
        catch (IOException e) {
            System.err.println("Can't open input IP connection for connectors on port number: "+port);
        }
        start();
    }

    /**
     * The server receives the synchronisation message when the connector is created
     * and create a client for this connector.
     */
    @Override
    public void run() {
        enMarche = true;
        while(enMarche) { // le thread peut etre arrete par stopThread()
            try { 
                if (conn != null) {
                    Socket dialogue = conn.accept(); // socket pour recevoir les donn?es de ce connecteur
                    dataReceptor = new IPConnectorDataReceptor(dialogue);
                }
            }
            catch (SecurityException ioe) {
                if (enMarche) System.err.println("IP connectors receptor : error receiving object");
            }
            catch (IOException ent) {
                // Ce cas n'est pas une erreur il peut se produire si l'autre extremite a ete supprimee
                // en cours d'emission (normalement ce connecteur va etre supprime aussi)
                if (enMarche) System.err.println("IP connectors receptor : error receiving object");
           }
        }
    }

    /**
     * Stops the server that receive data for connectors
     */
    public void stopThread() {
        // Arret du serveur reseau qui recoit les donnes des connecteurs
        if (dataReceptor !=null) dataReceptor.stopThread();
        enMarche = false;
        try { conn.close(); }
        catch (IOException ioe) {
            System.err.println("Can't stop server for connectors' data reception");
        }
    }

}
