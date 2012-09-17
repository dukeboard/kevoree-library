package network.connectors;

import model.interfaces.network.INetworkTraffic;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.NetworkAddress;
import util.Parameters;
import platform.plugins.installables.network.DNS.KalimuchoDNS;

// Classe des threads assurant les receptions reseau pour les connecteurs deportes

/**
 * A thread waiting for messages on the mailbox of a connector and sending them to
 * the input of this connector.<br>
 * There is a thread of this class for each connector which receives
 * data by network.<br>
 * This thread terminates when it finds a disconnection of the connector
 * or when stopped by the platform.
 *
 * @author Dalmau
 */
public class ConnectorReceptionThread extends Thread {

    private model.korrontea.InputUnit serveur; // UE du connecteur qui reeoit
    private boolean enMarche; // indique si ce thread est en marche
    private boolean actif; // indique si ce thread doit etre arrete
    private String monNom;
    private ConnectorMaiboxMessage buffer;
    private ConnectorsMailboxes bal;
    private NetworkAddress exp;
    private KalimuchoDNS dns;

    /**
     * Construction of a thread for a connector
     *
     * @param nomconnecteur name of the connector
     * @param adr  address for sending data of this connector

     */
    public ConnectorReceptionThread(String nomconnecteur, NetworkAddress adr) {
        monNom = nomconnecteur;
        exp = adr;
        bal = (ConnectorsMailboxes)ServicesRegisterManager.platformWaitForService(Parameters.CONNECTORS_DATA_MAILBOX);
        try {
            dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
        }
        catch (ServiceClosedException sce) { dns = null; }
        actif = true; // le thread est pret a fonctionner
        model.korrontea.ControlUnit accesUE = (model.korrontea.ControlUnit)ServicesRegisterManager.platformWaitForService(monNom);
        serveur = accesUE.getIU();
    }

    /**
     * Stop the thread when the connector is removed
     */
    public synchronized void stopThread() { // arret du thread
        actif = false;
        buffer.stop();
        bal.removeMailbox(monNom);
    }

    /**
     * Wait for this thread to terminate
     */
    public void waitUntilStopped() {
        while (enMarche) {} // attente de terminaison du run
    }

    /**
     * Waits for messages in the mailbox and give them to the connector
     */
    @Override
    public void run() {
        buffer = bal.findMailbox(monNom); // bloque tant que le boete n'a pas ete creee par reception de la synchro
        buffer.retirerMessage();
        enMarche = true;
        while(actif) { // le thread peut etre arrete par stopThread()
            EncapsulatedSample ech = null;
            ech = buffer.retirerMessage(); // envoie l'acquitement
            if (actif) { // si le thread n'a pas ete arrete pendant la reception
                if (dns != null) ech.setRemoteDate(dns, exp);
                else ech.setLocalDate();
                try { // deposer l'echantillon dans l'UE du connecteur
                    model.korrontea.ControlUnit accesUE = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(monNom);
                    serveur = accesUE.getIU();
                    ech.setExpeditor(exp.getNormalizedAddress());
                    serveur.deposeSample(ech);
                }
                catch (ServiceClosedException ace) { // non connecte au connecteur
                    actif = false;
                }
            }
        }
        enMarche = false;
    }

    /**
     * Returns the object that measure quantity of received data on network for this connector
     * @return the object that measure quantity of received data on network for this connector
     */
    public INetworkTraffic getRecepteur() {
        return bal.findDataReceptor(monNom);
    }

    /**
     * Calculates the average traffic on network of the connector
     * @param debit the actual number of bytes received by this connector since last measure
     */
    public void calculeDebitMoyen(int debit) {
       try { // essayer de recuperer un echantillon dans l'US du connecteur
           model.korrontea.ControlUnit accesUC = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(monNom);
           accesUC.setDebit(debit);
       }
       catch (ServiceClosedException ace) { // on n'est plus connecte au connecteur
       }
    }

}
