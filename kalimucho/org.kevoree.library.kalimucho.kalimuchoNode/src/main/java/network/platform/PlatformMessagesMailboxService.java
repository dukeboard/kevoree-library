package network.platform;

import java.util.Vector;

/**
 * Service for storing messages for the platform.<br>
 * Offers methods to produce and consume messages.<br>
 * Consummation uses a semaphore in order to suspend the process when there are no messages.
 *
 * @author Dalmau
 */

public class PlatformMessagesMailboxService {

    /**
     * Buffer that holds messages received from the PFs.
     */
    protected Vector<NetworkPlatformMessage> buffer; // buffer qui reeoit les messages reeus par la PF
    private boolean enMarche;

    /**
     * Create the buffer
     */
    public PlatformMessagesMailboxService() {
        buffer = new Vector<NetworkPlatformMessage>(); // contenu du buffer
        enMarche = true;
    }

        // Methode appelee par le consommateur pour retirer un element du buffer
    /**
     * Get a message from the buffer<br>
     * Waits until a message is available
     *
     * @return the message
     */
    public synchronized NetworkPlatformMessage getMessage() {
        while ((buffer.size()==0) && enMarche) {
            try { wait(); }
            catch (InterruptedException ie) {}
        }
        if (enMarche) {
            // recuperer puis enlever un message du buffer
            NetworkPlatformMessage trouve = buffer.firstElement();
            buffer.removeElementAt(0);
            return trouve; // element retire du buffer
        }
        else return null;
    }

    /**
     * Get a message from the buffer<br>
     * Waits until a message is available or until interrupted by an InterruptException
     *
     * @return the message
     * @throws InterruptedException if the BC is stopped by the platform
     */
    public synchronized NetworkPlatformMessage getMessageInterruptible() throws InterruptedException {
        while ((buffer.size()==0) && enMarche) {
            wait();
        }
        if (enMarche) {
            // recuperer puis enlever un message du buffer
            NetworkPlatformMessage trouve = buffer.firstElement();
            buffer.removeElementAt(0);
            return trouve; // element retire du buffer
        }
        else return null;
    }


    // Methode utilisee par le recepteur reseau pour deposer un message dans le buffer
    /**
     * Put a message in the buffer<br>
     * This method is used by the network receptor associated to the platform
     *
     * @param ech Message received to put in the buffer
     */
    public synchronized void deposeMessage(NetworkPlatformMessage ech) {
        buffer.addElement(ech); // deposer l'echantillon
        notifyAll(); // il y avait peut-etre un consommateur bloque
    }

    // Arret du service de stockage des messages pour la PF
    /**
     * Stops the mailbox used by the supervisor to get received messages
     */
    public synchronized void stop() {
        enMarche = false;
        notifyAll(); // il y avait peut-?tre un consommateur bloqu?
    }

}
