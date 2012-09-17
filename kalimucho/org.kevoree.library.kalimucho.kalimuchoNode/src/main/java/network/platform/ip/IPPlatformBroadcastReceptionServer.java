package network.platform.ip;

import model.interfaces.network.INetworkServer;
import model.interfaces.network.INetworkTraffic;
import network.platform.PlatformMessagesReceptor;
import network.platform.NetworkPlatformMessage;
import util.NetworkAddress;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import network.AddressesChecker;

/**
 * Receive multicasted NetworkMessages on UDP for the platform
 * @author Dalmau
 */
public class IPPlatformBroadcastReceptionServer extends Thread implements INetworkServer, INetworkTraffic {

    private MulticastSocket socket;
    private InetAddress group;
    private boolean arret;
    private PlatformMessagesReceptor messagesRecus; // buffer de depot des messages de la PF
    private NetworkAddress monAdresse;
    private int numPort;
    private ContextManager gestionnaireContexte;
    private int receivedData;
    private KalimuchoDNS dns;
    private String myID;

    /**
     * Server that receives messages on an UDP multicast socket an gives them to the PF message receptor
     * @param adr the addres if this host on the network on which this server works
     * @param b the PF message to receptor to send the received messages to
     * @param p the UDP port number on which the server listen
     */
    public IPPlatformBroadcastReceptionServer(NetworkAddress adr, PlatformMessagesReceptor b, int p) {
        monAdresse = adr;
        messagesRecus = b;
        numPort = p;
        AddressesChecker addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        myID = addressesChecker.getHostIdentifier();
        receivedData = 0;
        try {
            socket = new MulticastSocket(numPort);
            group = InetAddress.getByName(Parameters.UDP_BROADCAST_GROUP_PF);
            socket.joinGroup(group);
        }
        catch (IOException ioe) {
            socket = null;
            group = null;
            System.err.println("Error creating broadcast IP server for class finder");
        }
        arret = false;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Stops the broadacst server for the platform
     */
    public void stopThread() {
        arret = true;
        try {
            socket.leaveGroup(group);
            socket.close();
        }
        catch (IOException ioe) {
            System.err.println("Error stopping broadcast IP server for class finder");
        }
    }

    /**
     * Wait for multicast messages an give them to the PF message receptor.
     * Because multicast messages are received by the sender, messages received from this host are deleted.
     */
    @Override
    public void run() {
        DatagramPacket packet;
        while (!arret) {
            try {
                byte[] buf = new byte[Parameters.MAX_BROADCAST_MESSAGE_SIZE];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                // message recu = (classe, demandeur, relais)
                NetworkPlatformMessage message = new NetworkPlatformMessage(packet.getData());
                receivedData = packet.getLength();
                // la condition suivante sert a simuler une non connectivite avec un hote
                if ((!packet.getAddress().getHostAddress().contains(Parameters.NETWORK_EXCLUSION)) || (message.getSenderID().equals("Deployment"))) {
                    if (!message.getSenderID().equals(myID)) {
                        // je ne suis pas emetteur de ce broadcast => traiter
                        message.setAddress(message.getExpeditorAddress());
                        try {
                            dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.addReference(message.getSenderID(), packet.getAddress().getHostAddress(), true);
                            if (!(new NetworkAddress(message.getExpeditorAddress()).equals(new NetworkAddress(packet.getAddress().getHostAddress()))))
                               dns.addReference(message.getExpeditorID(), message.getExpeditorAddress(), false);
                        }
                        catch (ServiceClosedException sce) { }
                        messagesRecus.deposerMessage(message); // le deposer dans le buffer pour la PF
                    }
                }
            }
            catch (SecurityException ioe) {
                if (!arret) {
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF broadcast message: security"));
                }
            }
            catch (SocketTimeoutException ioe) {
                if (!arret) {
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF broadcast message: time out"));
                }
            }
            catch (SocketException ioe) {
                if (!arret) {
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF broadcast message: socket"));
                }
            }
            catch (IOException ioe) {
                if (!arret) {
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when receiving PF broadcast message: IO"));
                }
            }
        }
    }

    /**
     * Returns  the number of bytes received on network by the PF in broadcast since the last call of this method.
     * @return  the number of bytes received on network by the PF in broadcast since the last call of this method.
     */
    public int getDataSize() {
        int ret = receivedData;
        receivedData = 0;
        return ret;
    }

}
