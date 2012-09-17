package network.connectors;

import model.interfaces.network.IConnectorACKSender;

/**
 * Acces facility to the mailbox for storing messages for the connectors.<br>
 * Offers methods to produce and consume samples.<br>
 * Consummation uses a semaphore in order to suspend the process when there is no sample.<br>
 * When a message is read the mailbox automaticaly send an acknowlegment.
 *
 * @author Dalmau
 */

public class ConnectorMaiboxMessage {

    private EncapsulatedSample echantillon;
    private boolean actif;
    private boolean present;
    private IConnectorACKSender envoi;

    /**
     * Create the mailbox which send acknowledgments
     * @param env  the acknowlege sender (according to the kind of network associated to the connector)
     */
    public ConnectorMaiboxMessage(IConnectorACKSender env) {
        envoi = env;
        actif = true;
        present = false;
    }

    /**
     * Stops the message store => threads waiting for message are unlocked
     * in order to allow them to terminate when a connector is removed.
     */
    public synchronized void stop() {
        envoi.close();
        actif = false;
        notifyAll();
    }

    // Methode appelee par le consommateur pour retirer un element du buffer
    /**
     * Get a sample from the message<br>
     * Waits until a message is available. This method is used by the Connectors' reception threads
     * to get samples and put them in the IU of the connector.
     *
     * @return the message
     */
    public synchronized EncapsulatedSample retirerMessage() {
        while ((!present) && actif) { // se bloquer si le buffer est vide
            try { 
                wait();
            }
            catch (InterruptedException ie) { }
        }
        if (actif) {
            EncapsulatedSample recEch = echantillon;
            // renvoyer l'acquitement de cet echantillon
            envoi.sendACK();
            present = false; // pret pour en recevoir un autre
            return recEch; // element retire du buffer
        }
        else return null;
    }

    // Methode utilisee par le recepteur reseau pour deposer un echantillon dans le buffer

    /**
     * Depose a message in the mailbox, this method is used by the network receptor when data is
     * received for a connector. It unlocks threads waiting foir a message.
     * @param ech sample
     */
    public synchronized void deposerMessage(EncapsulatedSample ech) {
        echantillon = ech;
        present = true;
        notifyAll(); // il y avait peut-etre un consommateur bloque
    }

}
