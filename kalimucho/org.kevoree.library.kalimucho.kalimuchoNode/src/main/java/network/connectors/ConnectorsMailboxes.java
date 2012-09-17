package network.connectors;

import model.interfaces.network.IConnectorACKSender;
import java.util.HashMap;
import model.interfaces.network.INetworkServer;
import model.interfaces.network.INetworkTraffic;

/**
 * Manages the mailboxes of the connectors (received data for connectors). Each mailbox is a ConnectorMaiboxMessage.
 *
 * @author Dalmau
 */
public class ConnectorsMailboxes {

    private HashMap<String, ConnectorMaiboxMessage> mailboxes;
    private HashMap<String, INetworkServer> threadsBal;

    /**
     * Create the mailboxes manager for connectors
     */
    public ConnectorsMailboxes() {
        mailboxes = new HashMap<String, ConnectorMaiboxMessage>();
        threadsBal = new HashMap<String, INetworkServer>();
    }

    /**
     * Create a new mailbox (for a new connector).
     * @param name name of the connector
     * @param env The acknowleges sender (according to the network associated to the connector)
     * @param cli client that receives data for this connector
     */
    public synchronized void createMailbox(String name, IConnectorACKSender env, INetworkServer cli) {
        // Creation du thread recepteur de donnees pour ce connecteur
        threadsBal.put(name, cli);
        mailboxes.put(name, new ConnectorMaiboxMessage(env));
        notifyAll();
    }

    /**
     * Finds the mailbox of a given connector
     * @param name name of the connector
     * @return the mailbox associated to this connector
     */
    public synchronized ConnectorMaiboxMessage findMailbox(String name) {
        while (mailboxes.get(name) == null) {
            try { wait(); }
            catch (InterruptedException ie) { return null; }
        }
        return mailboxes.get(name);
    }

    /**
     * Removes the mailbox of a given connector
     * @param name name of the connector
     */
    public synchronized void removeMailbox(String name) {
        if (mailboxes.get(name) != null)  {
            mailboxes.remove(name);
            threadsBal.get(name).stopThread();
            threadsBal.remove(name);
         }
    }

    /**
     * returns the object that measures the traffic for a given connector
     * @param name name of the connector
     * @return the object that measures the traffic for the given connector
     */
    public synchronized INetworkTraffic findDataReceptor(String name) {
        while (threadsBal.get(name) == null) {
            try { wait(); }
            catch (InterruptedException ie) { return null; }
        }
        return (INetworkTraffic)(threadsBal.get(name));
    }

}
