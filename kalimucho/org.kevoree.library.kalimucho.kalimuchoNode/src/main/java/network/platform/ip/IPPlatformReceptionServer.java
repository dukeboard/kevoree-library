package network.platform.ip;

import network.platform.NetworkPlatformMessage;
import model.interfaces.network.INetworkTraffic;
import util.Parameters;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.Socket;
import network.platform.PlatformMessagesReceptor;
import util.NetworkAddress;
import java.net.InetSocketAddress;
import model.interfaces.network.INetworkServer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.SizeCountOutputStream;
import platform.plugins.installables.network.DNS.KalimuchoDNS;

// classe du thread qui gere la reception des messages pour la PF

/**
 * Thread that receive on IP messages for the PF and put them into a mailbox.
 *
 * @author Dalmau
 */
public class IPPlatformReceptionServer extends Thread  implements INetworkServer, INetworkTraffic {
    private ServerSocket conn=null; // connexion par socket
    private Socket lien = null;
    private int numPort; // numero du port utilise par les PF
    private PlatformMessagesReceptor messagesRecus; // buffer de depot des messages de la PF
    private boolean enMarche;
    private ContextManager gestionnaireContexte;
    private int receivedData;
    private KalimuchoDNS dns;

    /**
     * Creates the thread that manages messages received for the PF.
     * @param adr  address on which this server runs
     * @param b mailbox to put the messages
     * @param p port number for receiving these messages
     */
    public IPPlatformReceptionServer(NetworkAddress adr, PlatformMessagesReceptor b, int p) {
        numPort = p;
        receivedData = 0;
        try{
            conn = new ServerSocket(); // creation de la socket de service
            conn.bind(new InetSocketAddress(adr.getNormalizedAddress(), numPort));
        } catch (IOException e) {
            System.err.println("Can't open IP connection for the platform on port "+numPort);
        }
        messagesRecus = b;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Server that receives the messages for the PF and puts them in the PF maibox.
     */
    @Override
    public void run() {
        enMarche = true;
        while (enMarche) {
            NetworkPlatformMessage recu = null;
            try {
                if (conn != null) {
                    // attente d'un message reseau pour la PF
                    lien = conn.accept(); // recuperer le message
                    // traitement d'un message pour la PF
                    ObjectInputStream ois = new ObjectInputStream(lien.getInputStream());
                    recu = (NetworkPlatformMessage)ois.readObject(); // message recupere
                    receivedData = receivedData+getMessageSize(recu);
                    // mettre dans le message l'@ de l'expediteur
                    recu.setAddress(recu.getExpeditorAddress());
                    // la condition suivante sert a simuler une non connectivite avec un hote
                    if ((!lien.getInetAddress().getHostAddress().contains(Parameters.NETWORK_EXCLUSION))||(recu.getSenderID().equals("Deployment"))) {
                        try {
                            dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.addReference(recu.getSenderID(), lien.getInetAddress().getHostAddress(), true);
                            if (!(new NetworkAddress(recu.getExpeditorAddress()).equals(new NetworkAddress(lien.getInetAddress().getHostAddress()))))
                               dns.addReference(recu.getExpeditorID(), recu.getExpeditorAddress(), false);
                        }
                        catch (ServiceClosedException sce) { }
                        messagesRecus.deposerMessage(recu); // le deposer dans le buffer pour la PF
                    }
                    lien.close();
                    ois.close();
                }
            }
            catch (SecurityException se) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: security"));
            }
            catch (SocketException se) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: socket"));
            }
            catch (InvalidClassException ent) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: invalid class"));
            }
            catch (StreamCorruptedException ent) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: stream corrupted"));
            }
            catch (OptionalDataException ent) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: optional data"));
            }
            catch (ClassNotFoundException ent) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: class not found"));
            }
            catch (IOException ent) {
                if (enMarche) gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF message: IO"));
            }
        }
    }

    /**
     * Stops the server that receive messages for the platform
     */
    public void stopThread() {
        // Arret du serveur reseau qui recoit les messages pour la PF
        enMarche = false;
        try { conn.close(); }
        catch (IOException ioe) {
            System.err.println("Can't stop server for connectors' synchronisation");
        }
    }

    /**
     * Returns  the number of bytes received on network by the PF since the last call of this method.
     * @return  the number of bytes received on network by the PF since the last call of this method.
     */
    public int getDataSize() {
        int ret = receivedData;
        receivedData = 0;
        return ret;
    }

    private int getMessageSize(NetworkPlatformMessage s) {
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
