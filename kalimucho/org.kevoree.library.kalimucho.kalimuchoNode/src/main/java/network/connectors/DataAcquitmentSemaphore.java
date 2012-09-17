package network.connectors;

import model.interfaces.network.INetworkServer;

/**
 * This class manages the semaphore for data synchronisation for connectors.
 *
 * @author Dalmau
 */
public class DataAcquitmentSemaphore {

    private String nom;
    private boolean acquite;
    private INetworkServer recACK;

    /**
     * Creates the socket to send data for a connector.
     * 
     * @param name name of the message's owner
     */
    public DataAcquitmentSemaphore(String name) {
        nom = name;
        acquite = true;
    }

    /**
     * Close the socket to send data for a connector.
     *
     */
    public void close() {
        acquite(); // pour debloquer le thread s'il attend le ACK
        recACK.stopThread();
    }

    /**
     * Set sent message acquited.
     * 
     */
    public synchronized void acquite() {
        acquite = true;
        notifyAll();
    }

    /**
     * Set sent message not acquited.
     *
     */
    public synchronized void NotAcquited() {
        acquite = false;
    }

    /**
     * Wait for message acquited.
     * 
     */
    public synchronized void waitForACK() {
        while (!acquite) {
            try { wait(); }
            catch (InterruptedException ie) { return; }
        }
    }

    /**
     * Get the name of the owner of this connexion.
     * 
     * @return name of the owner of this connexion.
     */
    public String getName() {
        return nom;
    }

    /**
     * Sets the receptor for acknowlegments.
     * @param cli associated receptor for acknowlegments
     */
    public void setACKClient(INetworkServer cli) {
        recACK = cli;
    }

}
