package network.connectors.ip;

import java.net.Socket;
import java.io.ObjectInputStream;
import util.Parameters;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.ObjectOutputStream;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import network.connectors.ConnectorsMailboxes;
import network.connectors.ConnectorMaiboxMessage;
import model.interfaces.network.INetworkServer;
import model.interfaces.network.INetworkTraffic;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.SizeCountOutputStream;
import network.connectors.EncapsulatedSample;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import util.NetworkAddress;

// Classe des threads assurant les receptions reseau pour les connecteurs deportes

/**
 * Server to receive data from all connectors on IP.
 * When run this server creates a specific mailbox for the new created connector.
 * Then it puts the received data in the mailbox of the connector.
 *
 * @author Dalmau
 */
public class IPConnectorDataReceptor extends Thread implements INetworkServer, INetworkTraffic {
    private Socket dialogue = null;
    private String connecteur;
    private ConnectorsMailboxes bal;
    private boolean actif;
    private ObjectInputStream lireEch;
    private boolean enMarche;
    private ContextManager gestionnaireContexte;
    private int receivedData;
    private IPConnectorACKSender ackSender;
    private String remoteHostID;
    private NetworkAddress  remoteHostAddress;

    /**
     * Create a server to receive data from all connectors.
     * @param dial socket used by this server
     */
    public IPConnectorDataReceptor(Socket dial) {
        dialogue = dial;
        remoteHostAddress = new NetworkAddress(dialogue.getInetAddress().getHostAddress());
        try { 
            lireEch = new ObjectInputStream(dialogue.getInputStream());
            String synchro = (String)lireEch.readObject();
            connecteur = synchro.split(";")[0];
            remoteHostID = synchro.split(";")[1];
            try {
                KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                dns.addReference(remoteHostID, remoteHostAddress, true);
            }
            catch (ServiceClosedException sce) { }
        }
        catch (ClassNotFoundException ent) {
            if (actif) System.err.println("IP connectors data receptor : SYNC class not found error");
        }
        catch (IOException e) {
            System.err.println("Can't open IP connection whith connector: "+connecteur);
        }
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        bal = (ConnectorsMailboxes)ServicesRegisterManager.platformWaitForService(Parameters.CONNECTORS_DATA_MAILBOX);
        ackSender = new IPConnectorACKSender(dialogue, connecteur);
        bal.createMailbox(connecteur, ackSender, this);
        ConnectorMaiboxMessage depot = bal.findMailbox(connecteur);
        depot.deposerMessage(null);
        actif = true;
        receivedData = 0;
        start();
    }

    /**
     * Stops the server
     */
    public void stopThread() {
        actif = false;
        try {
            lireEch.close();
            dialogue.close();
        }
        catch (IOException e) {
            System.err.println("Can't close IP connection whith connector: "+connecteur);
        }
    }

    /**
     * The server receive data for connectors and put it into the appropriate mailbox.
     */
    @Override
    public void run() {
        enMarche = true;
        while(actif) { // le thread peut etre arrete par stop()
            try { // recuperer un echantillon et le deposer dans la BeL du connecteur
                EncapsulatedSample ech = (EncapsulatedSample)lireEch.readObject();
//                receivedData = receivedData+getSampleSize(ech);
                receivedData = receivedData+ech.size()+EncapsulatedSample.EXTRA_SIZE;
                try {
                    KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    dns.addReference(remoteHostID, remoteHostAddress, true);
                }
                catch (ServiceClosedException sce) { }
                ConnectorMaiboxMessage depot = bal.findMailbox(connecteur);
                // si la boete n'existe pas on perd l'echantillon
                // Ce cas peut se produire lorsqu'un connecteur est supprime
                // et que le cote recepteur est supprime avant le cote emetteur
                if (depot != null) depot.deposerMessage(ech);
            }
            catch (StreamCorruptedException  ace) {
                if (actif) gestionnaireContexte.signalEvent(new ContextInformation(connecteur+" error when receiving data on IP: stream corrupted"));
            }
            catch (InvalidClassException  ace) {
                if (actif) gestionnaireContexte.signalEvent(new ContextInformation(connecteur+" error when receiving data on IP: invalid class"));
            }
            catch (OptionalDataException  ace) {
                if (actif) gestionnaireContexte.signalEvent(new ContextInformation(connecteur+" error when receiving data on IP: optional data"));
            }
            catch (ClassNotFoundException ent) {
                if (actif) gestionnaireContexte.signalEvent(new ContextInformation(connecteur+" error when receiving data on IP: class not found"));
            }
            catch (IOException ent) {
                // Ce cas n'est pas une erreur il peut se produire si l'autre extremite a ete supprimee
                // en cours d'emission (normalement ce connecteur va etre supprime aussi)
            }
        }
        enMarche = false;
    }

    /**
     * Returns the number of bytes received on network by this connector's data receptor since the last call of this method.
     * @return the number of bytes received on network by this connector's data receptor since the last call of this method.
     */
    public int getDataSize() {
        int ret = receivedData;
        receivedData = 0;
        return ret;
    }

    private int getSampleSize(EncapsulatedSample s) {
        int taille;
        SizeCountOutputStream bos = new SizeCountOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(s);
            taille = bos.getSize();
        }
        catch (IOException ioe) {
            taille = 0;
        }
        return taille;
    }

}
