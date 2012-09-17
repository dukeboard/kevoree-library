package network.platform.ip;

import model.interfaces.network.INetworkBroadcastSender;
import model.interfaces.network.INetworkTraffic;
import network.platform.NetworkPlatformMessage;
import util.Parameters;
import java.util.Vector;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.io.IOException;
import util.NetworkAddress;
import platform.servicesregister.ServicesRegisterManager;
import platform.context.ContextManager;
import platform.context.ContextInformation;

/**
 * Send NetworkMessages on UDP multicast for the platform
 *
 * @author Dalmau
 */

public class IPPlatformBroadcastMessagesSender extends Thread implements INetworkBroadcastSender, INetworkTraffic {

    private Vector<NetworkPlatformMessage> messagesEnAttente; // messages du superviseur
    private boolean enMarche, arrete, termine;
    private NetworkAddress monAdresse;
    private ContextManager gestionnaireContexte;
    private int sendedData;

    /**
     * Creates the multicast sender
     * @param adr the address of the fost on this network
     */
    public IPPlatformBroadcastMessagesSender(NetworkAddress adr) {
        monAdresse = adr;
        messagesEnAttente = new Vector<NetworkPlatformMessage>();
        enMarche = true;
        arrete = false;
        sendedData = 0;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Sends a message on UDP multicast. This message is put in a queue and sent by the sender's thread.
     * @param tr the message to send
     */
    public synchronized void posterMessage(NetworkPlatformMessage tr) {
        messagesEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }

    /**
     * Sends a message on UDP multicast. This message is put in a queue and sent by the sender's thread.
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
     * Returns the local host address on the network on which this sender works
     * @return the local host address on the network on which this sender works
     */
    public NetworkAddress getSenderAddress() {
        return monAdresse;
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
        if (envoi.getExpeditorAddress().equals("")) envoi.setExpeditorAddress(monAdresse.getNormalizedAddress());
        int compteEssaiRestants = 3;
        DatagramSocket socketReponse  = null;
        while (compteEssaiRestants != 0) {
            try {
                socketReponse = new DatagramSocket();
                try {
                    byte[] buf = envoi.toByteArray();
                    sendedData = sendedData+buf.length;
                    if (buf.length>Parameters.MAX_BROADCAST_MESSAGE_SIZE) {
                        System.err.println("PF Broadcast sender: message size too long, message not sent");
                        compteEssaiRestants = 0;
                    }
                    else {
                        DatagramPacket packet;
                        packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(Parameters.UDP_BROADCAST_GROUP_PF), Parameters.PORT_UDP_BROADCAST_PF);
                        socketReponse.send(packet);
                        socketReponse.close();
                        compteEssaiRestants = 0;
                    }
                }
                catch (PortUnreachableException ioe) {
                    if (socketReponse !=  null) socketReponse.close();
                    if (arrete) compteEssaiRestants =0;
                    else {
                        compteEssaiRestants--;
                        if (compteEssaiRestants == 0) {
                            gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF broadcast: port unreachable"));
                        }
                    }
                }
                catch (SecurityException e) {
                    compteEssaiRestants = 0;
                    if (!arrete) gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF broadcast: securitu"));
                }
                catch (SocketException e) {
                    if (socketReponse !=  null) socketReponse.close();
                    if (arrete) compteEssaiRestants =0;
                    else {
                        compteEssaiRestants--;
                        if (compteEssaiRestants == 0) {
                            gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF broadcast: socket"));
                        }
                    }
                }
                catch (IOException e) {
                    if (socketReponse !=  null) socketReponse.close();
                    if (arrete) compteEssaiRestants =0;
                    else {
                        compteEssaiRestants--;
                        if (compteEssaiRestants == 0) {
                            gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF broadcast: IO"));
                        }
                    }
                }
            }
            catch (SocketException se) {
                if (arrete) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        gestionnaireContexte.signalEvent(new ContextInformation("Error creating platform message sender broadcast socket"));
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
     * Returns  the number of bytes sent on network by the PF in broadcast since the last call of this method.
     * @return  the number of bytes sent on network by the PF in broadcast since the last call of this method.
     */
    public int getDataSize() {
        int ret = sendedData;
        sendedData = 0;
        return ret;
    }

}
