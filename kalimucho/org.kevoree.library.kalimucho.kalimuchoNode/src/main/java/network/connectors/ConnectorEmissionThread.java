package network.connectors;

import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.NetworkAddress;
import network.NetworkEmitterContainer;
import model.interfaces.network.IConnectorDataSender;
import model.interfaces.network.INetworkTraffic;

/**
 * Thread which takes the messages from a connector output to send them by network.<br>
 * There is thread of this class for each connector which sends data by network.<br>
 * This thread terminates when it finds a disconnection of the connector
 * or when stopped by the platform.
 *
 * @author Dalmau
 */

// Classe des threads utilises par les US des connecteurs qui emettent par la reseau

public class ConnectorEmissionThread extends Thread {

    private IConnectorDataSender emetteur;
    private String monNom; // nom de ce thread
    private NetworkAddress adresse; // adresse oe envoyer
    private boolean actif; // indique si le thread doit etre arrete
    private model.korrontea.OutputUnit serveur; // US du connecteur qui envoie
    private boolean enMarche; // indique si le thread est en marche
    private DataAcquitmentManager listeConnexions;

    /**
     * Construction of the thread
     *
     * @param n name of the connector using this thread
     * @param adr address for sending data of this connector

     */
    public ConnectorEmissionThread(String n, NetworkAddress adr) {
        monNom=n;
        adresse = adr;
        actif = true;
        listeConnexions = (DataAcquitmentManager)ServicesRegisterManager.platformWaitForService(Parameters.CONNECTORS_ACKNOWLEDGE);
        listeConnexions.addConnexionDesignation(monNom);
        NetworkEmitterContainer nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        emetteur = nec.createConnectorDataSender(adr);
        model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.platformWaitForService(monNom);
        serveur = accesUS.getOU();
        serveur.consumerConnection(this, -1); // indiquer a l'US du connecteur que le thread est connecte
    }

    /**
     * Stop the thread when the connector is removed
     */
    public void stopThread() {
        actif=false; // pour arreter le thread
        serveur.consumerDisconnection();
        listeConnexions.removeConnexionDesignation(monNom);
        emetteur.close();
    }

    /**
     * Wait for this thread to terminate
     */
    public void waitUntilStopped() {
        while (enMarche) {} // attente de terminaison du thread
    }

    /**
     * Send messages for the connector
     */
    @Override
    public void run() {
       // envoyer un message de synchro avec le nom du connecteur
       emetteur.sendMessage(monNom, null);
       enMarche = true;
       while (actif) { // boucle d'envoi des messages du connecteur
           EncapsulatedSample ech=null;
           model.korrontea.ControlUnit accesUS;
           try { // essayer de recuperer un echantillon dans l'US du connecteur
               accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(monNom); 
               serveur = accesUS.getOU();
               ech = serveur.getSample();
               // on arrive ici soit
               //   parce qu'on a recupere un echantillon => l'emettre
               //   parce qu'on a ete arrete => se terminer
           }
           catch (ServiceClosedException ace) { // on n'est plus connecte au connecteur
               accesUS = null;
               actif=false; // si le connecteur n'existe plus se terminer
           }
           if (actif) { // si on n'a pas ete arrete on peut emettre l'echantillon
                DataAcquitmentSemaphore ack = listeConnexions.findConnexionDesignation(monNom);
                ack.waitForACK(); // attendre d'avoir le droit d'emettre
                if (actif) { // si le connecteur n'a pas ete detruit entre temps, emettre
                    emetteur.sendMessage(monNom, ech);
                }
           }
       }
       enMarche = false;
    }

    /**
     * Returns the object that measure quantity of sent data on network for this connector
     * @return the object that measure quantity of sent data on network for this connector
     */
    public INetworkTraffic getEmetteur() {
        return (INetworkTraffic)emetteur;
    }

    /**
     * Calculates the average traffic on network of the connector
     * @param debit the actual number of bytes sent by this connector since last measure
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
