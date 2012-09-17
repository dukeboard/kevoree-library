package network.platform.ip;

import model.interfaces.network.INetworkTraffic;
import model.interfaces.network.INetworkBroadcastSender;
import network.platform.NetworkPlatformMessage;
import network.AddressesChecker;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.NetworkAddress;
import util.Parameters;
import java.util.Vector;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.net.UnknownHostException;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.SizeCountOutputStream;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import java.net.InetSocketAddress;

/**
 * Send NetworkMessages for the platform when no multicast/broadcast is available.<br>
 * The messages are sent to a referent platform which relay them.
 * @author Dalmau
 */
public class IPPlatformDirectMessagesSender extends Thread implements INetworkBroadcastSender, INetworkTraffic {

    private Vector<NetworkPlatformMessage> messagesEnAttente; // messages du superviseur
    private boolean enMarche, arrete, termine;
    private NetworkAddress referent;
    private NetworkAddress monAdresse;
    private ContextManager gestionnaireContexte;
    private int sendedData;

    /**
     * Sends messages to a specific host.
     * This kind of sender is used when no broadcast or multicast is availble on this network.
     * @param ref the specific host to send messages to
     */
    public IPPlatformDirectMessagesSender(NetworkAddress ref) {
        referent = ref;
        messagesEnAttente = new Vector<NetworkPlatformMessage>();
        enMarche = true;
        arrete = false;
        sendedData = 0;
        AddressesChecker addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        monAdresse = addressesChecker.getMyFirstAddress(referent.getType());
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Sends a message to the specific host. This message is put in a queue and sent by the sender's thread.
     * @param tr the message to send
     */
    public synchronized void posterMessage(NetworkPlatformMessage tr) {
        messagesEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }

    /**
     * Sends a message to the specific host. This message is put in a queue and sent by the sender's thread.
     * This message is completed by the address of the sending host on this network
     * @param tr message to complete and to send
     */
    public synchronized void posterMessageIncomplet(NetworkPlatformMessage tr) {
        NetworkPlatformMessage env = tr.clone();
        env.addContent(env.getContent()+monAdresse.getNormalizedAddress());
        messagesEnAttente.addElement(env);
        notifyAll(); // debloquer le thread d'envoi de messages
    }

    /**
     * Stops the sender. The senders goes on running until there are no more messages to send, then stops
     */
    public synchronized void stopThread() {
        arrete = true;
        notifyAll();
    }

    /**
     * Returns the specific host address on which this sender sends
     * @return the specific host address on which this sender sends
     */
    public NetworkAddress getSenderAddress() {
        return referent;
    }

    /**
     * Waits until there are no more messages to send
     */
    public synchronized void waitForBufferEmpty() {
        while (!termine) {
            try { wait(); }
            catch (InterruptedException ie) {}
        }
    }

    // Methode semaphore de traitement des demandes d'emission de messages par la PF
    private synchronized void traiterDemandes() {
        while (messagesEnAttente.size() == 0) {
            try {
                if (arrete) {
                    enMarche = false;
                    return;
                }
                else wait(); // si pas de message se bloquer
            }
            catch (InterruptedException ie) { }
        }
        // on a un message a envoyer (deblocage du thread)
        NetworkPlatformMessage envoi = messagesEnAttente.elementAt(0);
        messagesEnAttente.removeElementAt(0); // enlever le message
        int compteEssaiRestants = 3;
        Socket env = null;
        while (compteEssaiRestants != 0) {
            try { // essayer d'etablir la connexion IP
                env = new Socket();
                env.connect(new InetSocketAddress(referent.getNormalizedAddress(), Parameters.PORT_IP_COMMANDS_PF), Parameters.IP_SOCKET_CONNEXION_TIME_OUT);
                try { env.setTcpNoDelay(true); }
                catch (SocketException ie) {
                    System.err.println("Can't put socket in TCP no delay mode");
                }
                // creation du flux d'objets pour emettre par reseau
                ObjectOutputStream ecrire = new ObjectOutputStream(env.getOutputStream());
                if (envoi.getExpeditorAddress().equals("")) envoi.setExpeditorAddress(monAdresse.getNormalizedAddress());
                if (envoi.getExpeditorPort() == 0) envoi.setExpeditorPort(Parameters.PORT_IP_COMMANDS_PF);
                sendedData = sendedData+getMessageSize(envoi);
                ecrire.writeObject(envoi);
                ecrire.flush(); ecrire.close();
                env.close();
                compteEssaiRestants = 0;
            }
            catch (SecurityException e) {
                if (arrete) compteEssaiRestants =0;
                else {
                    try {
                        KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                        dns.removeReference(referent);
                    }
                    catch (ServiceClosedException sce) { }
                    compteEssaiRestants = 0;
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: security"));
                }
            }
            catch (SocketException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(referent);
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: socket"));
                    }
                }
            }
            catch (NotSerializableException e) {
                compteEssaiRestants = 0;
                if (!arrete) gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: not serializable class"));
            }
            catch (UnknownHostException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(referent);
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: unknown host"));
                    }
                }
            }
            catch (InvalidClassException e) {
                compteEssaiRestants = 0;
                if (!arrete) gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: invalid class"));
            }
            catch (IOException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(referent);
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF pseudo broadcast message: IO"));
                    }
                }
            }
        }
    }

    /**
     * Wait for messages to send and send them
     */
    @Override
    public void run() {
        // boucle de traitement des messages a envoyer
        termine = false;
        while (enMarche) {
            traiterDemandes();
        }
        terminer();
    }

    private synchronized void terminer() {
        termine = true;
        notifyAll();
    }

    /**
     * Returns  the number of bytes sent on network by the PF to the specific host since the last call of this method.
     * @return  the number of bytes sent on network by the PF to the specific host since the last call of this method.
     */
    public int getDataSize() {
        int ret = sendedData;
        sendedData = 0;
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
