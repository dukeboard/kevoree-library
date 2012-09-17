package network.connectors.ip;

import network.connectors.DataAcquitmentManager;
import network.connectors.DataAcquitmentSemaphore;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import model.interfaces.network.INetworkServer;
import util.Parameters;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.NetworkAddress;

/**
 * Server to receive acknowleges from connectors on IP
 *
 * @author Dalmau
 */
public class IPConnectorACKReceptor extends Thread implements INetworkServer {

    private Socket dial;
    private boolean actif;
    private DataAcquitmentManager listeAcquitements;
    private ObjectInputStream lire;
    private String connector;
    private boolean enMarche;
    private ContextManager gestionnaireContexte;
    private String remoteHostID;
    private NetworkAddress  remoteHostAddress;

    /**
     * Create a server to receive acknowleges from connectors.
     * This thread is run when a sync message is received from a cnew vreated connector
     * @param dialogue socket used by this server
     */
    public IPConnectorACKReceptor(Socket dialogue) {
        dial = dialogue;
        remoteHostAddress = new NetworkAddress(dialogue.getInetAddress().getHostAddress());
        actif = true;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        listeAcquitements = (DataAcquitmentManager)ServicesRegisterManager.platformWaitForService(Parameters.CONNECTORS_ACKNOWLEDGE);
        try { // recuperer l'acquitement du SYNC
            lire = new ObjectInputStream(dial.getInputStream());
            String synchro = (String)lire.readObject();
            connector = synchro.split(";")[0];
            remoteHostID = synchro.split(";")[1];
            try {
                KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                dns.addReference(remoteHostID, remoteHostAddress, true);
            }
            catch (ServiceClosedException sce) { }
             // s'inscrire dans la liste des acquitements
            (listeAcquitements.findConnexionDesignation(connector)).setACKClient(this);
            (listeAcquitements.findConnexionDesignation(connector)).acquite();
        }
        catch (IOException ioe) {
            System.err.println("IP connectors error when receiving SYNC ACK message");
        }
        catch (ClassNotFoundException  ace) {
            System.err.println("IP connectors error when receiving SYNC ACK message: class not found");
        }
        start();
    }

    /**
     * Stops the server
     */
    public void stopThread() {
        actif = false;
        try { 
            lire.close();
            dial.close();
        }
        catch (IOException e) {
            System.err.println("Can't close IP ACK connection");
        }
    }

    /**
     * The servers waits for acknowlegements.
     * When an acknowlegement is received the semaphore for sending a new data is opened.
     */
    @Override
    public void run() {
        enMarche = true;
        while (actif) {
            try {
                lire.readObject();
                try {
                    KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    dns.addReference(remoteHostID, remoteHostAddress, true);
                }
                catch (ServiceClosedException sce) { }
                DataAcquitmentSemaphore semaphore = listeAcquitements.findConnexionDesignation(connector);
                // si le semaphore n'existe pas on ignore l'acquitement
                // Ce cas peut se produire lorsqu'un connecteur est supprime
                // et que le cote emetteur est supprime avant le cote recepteur
                if (semaphore != null) semaphore.acquite();
            }
            catch (StreamCorruptedException  ace) {
                gestionnaireContexte.signalEvent(new ContextInformation("IP connectors error when receiving ACK message: Stream corrupted"));
            }
            catch (InvalidClassException  ace) {
                gestionnaireContexte.signalEvent(new ContextInformation("IP connectors error when receiving ACK message: invalid class found"));
            }
            catch (OptionalDataException  ace) {
                gestionnaireContexte.signalEvent(new ContextInformation("IP connectors error when receiving ACK message: optional data"));
            }
            catch (ClassNotFoundException  ace) {
                gestionnaireContexte.signalEvent(new ContextInformation("IP connectors error when receiving ACK message: class not found"));
            }
            catch (IOException ioe) {
                // Ce cas n'est pas une erreur il peut se produire si l'autre extremite a ete supprimee
                // en cours d'emission (normalement ce connecteur va etre supprime aussi)
            }
        }
        enMarche = false;
    }

}
