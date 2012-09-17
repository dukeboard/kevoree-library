package network;

/**
 * Container that manages all the objects used by the platform to send data over any accessible network.
 * @author Dalmau
 */

import util.NetworkAddress;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import network.connectors.DataAcquitmentManager;
import network.platform.ip.IPPlatformMessagesSender;
import network.platform.ip.IPPlatformBroadcastMessagesSender;
import network.platform.ip.IPPlatformDirectMessagesSender;
import network.platform.PlatformMessagesEmitter;
import network.connectors.ip.IPConnectorDataSender;
import model.interfaces.network.IConnectorDataSender;
import model.interfaces.network.INetworkSender;
import model.interfaces.network.INetworkBroadcastSender;
import model.interfaces.network.INetworkTraffic;
import java.util.Vector;

// Conteneur des services de gestion de l'emetteur reseau de l'hote
/**
 * Create the emitter manager and registers it as a service.<br>
 * Create the service for connectors' acknowlegements and registers it as a service.<br>
 * Create the service which manages emissions for the connectors
 * and registers it as a service.
 * Create the service which manages emissions for the platform
 * and registers it as a service.
 *
 * @author Dalmau
 */
public class NetworkEmitterContainer {

    private Vector<NetworkAddress> mesAdresses;
    private Vector<INetworkSender> sendersLances;
    private Vector<INetworkBroadcastSender> sendersBroadcastLances;
    private NetworkEmissionService emetteurReseau;
    private DataAcquitmentManager listeAcquitements;
    private PlatformMessagesEmitter emetteurDeMessages;
    private AddressesChecker addressesChecker;

    /**
     * Construction of a network emiter container, registered as a service<br>
     * Create the emitter manager and registers it as a service.<br>
     * Create the service for connectors' acknowlegements and registers it as a service.<br>
     * Create the service which manages emissions for the connectors
     * and registers it as a service.
     * Create the service which manages emissions for the platform
     * and registers it as a service.
     */
    public NetworkEmitterContainer() {
        sendersLances = new Vector<INetworkSender>();
        sendersBroadcastLances = new Vector<INetworkBroadcastSender>();
        addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        mesAdresses = addressesChecker.getAllAddresses();

        // creer le service de creation/suppression des threads d'emission reseau pour les connecteurs
        emetteurReseau = new NetworkEmissionService(); 
        listeAcquitements = new DataAcquitmentManager(); // creer le service d'acquitement reseau
        try { // regarder si le service n'a pas deja ete cree
            ServicesRegisterManager.lookForService(Parameters.CONNECTORS_ACKNOWLEDGE);
        }
        catch (ServiceClosedException sce) { // si ce n'est pas le cas on le cree
            try { // enregistrer le service de gestion des emetteurs reseau pour les connecteurs
                ServicesRegisterManager.registerService(Parameters.CONNECTORS_ACKNOWLEDGE, listeAcquitements);
            }
            catch (ServiceInUseException  mbiue) {
                System.err.println("Network acquitment manager service created twice");
            }
        }

        // mise en place des services selon les types de reseau
        for (int i=0; i<mesAdresses.size(); i++) {
            switch (mesAdresses.elementAt(i).getType()) {
                case NetworkAddress.TYPE_IPV4 : // envoi par IP
                    // creation du thread pour gerer les emissions normales de la PF
                    INetworkSender empfip = new IPPlatformMessagesSender(mesAdresses.elementAt(i));
                    sendersLances.addElement(empfip);
                    // creation des threads pour gerer les emissions en broadcast de la PF
                    INetworkBroadcastSender empfbcst;
                    if (Parameters.BROADCAST_AVAILABLE) empfbcst = new IPPlatformBroadcastMessagesSender(mesAdresses.elementAt(i));
                    else empfbcst = new IPPlatformDirectMessagesSender(new NetworkAddress(Parameters.REFERENT_SERVER_IP_ADDRESS));
                    sendersBroadcastLances.addElement(empfbcst);
                    break;
                default :
                    break;
            }
        }

        try { // enregistrer ce conteneur
            ServicesRegisterManager.registerService(Parameters.NETWORK_EMISSIONS_CONTAINER, this);
        }
        catch (ServiceInUseException  siue) {
            System.err.println("Network emission container service created twice");
        }

        // ATTENTION l'emetteur de messages doit etre cree en dernier car il a besoin du thread d'emission pour la PF
        emetteurDeMessages = new PlatformMessagesEmitter(addressesChecker, this);
        emetteurDeMessages.startThread();
    }

    /**
     * Returns the service to create/remove threads for distributed connectors that send data
     * @return the service to create/remove threads for distributed connectors that send data
     */
    public NetworkEmissionService getNetworkEmissionService() { return emetteurReseau; }

    /**
     * Creates a sender for connector's data according to the type of network indicated by the parameter
     * @param adr address of the host to send data to
     * @return the created sender for connector's data
     */
    public IConnectorDataSender createConnectorDataSender(NetworkAddress adr) {
            switch (adr.getType()) {
            case NetworkAddress.TYPE_IPV4: // envoi par IP V4
                return new IPConnectorDataSender(adr);
            default: // non encore utilise
                return null;
        }
    }

    /**
     * Returns the service to send data for the platform on network
     * @return the service to send data for the platform on network
     */
    public PlatformMessagesEmitter getPlatformMessagesEmitter() { return emetteurDeMessages; }
    /**
     * Returns the service to send data for the platform on a specified network
     * @param type type of network
     * @return the service to send data for the platform on a specified network
     */
     public INetworkSender findPlatformSenderFor(int type) {
        boolean trouve = false;
        int i = 0;
        while ((i<sendersLances.size()) && (!trouve)) {
            if (sendersLances.elementAt(i).getSenderAddress().getType() == type) trouve = true;
            else i++;
        }
        if (trouve) return sendersLances.elementAt(i);
        else {
            System.err.println("PF cant send message for this type of network: "+type);
            return null;
        }
     }

     /**
      * Returns the port number associated to the platform according on the type of network given by the parameter
      * @param type type of network
      * @return the port number associated to the platform
      */
     public int getPlatformCommandPortNumber(int type) {
        switch (type) {
            case NetworkAddress.TYPE_IPV4 :
                return Parameters.PORT_IP_COMMANDS_PF;
            default: // non encore utilise
                return 0;
        }

     }
     /**
     * Returns the list of actually running broadcast or multicast senders.
     * If the are no broadcast or multicast senders, this list includes the direct senders used when no broadacast is possible.
     * @return the list of actually running broadcast/multicast or direct senders
     */
    public Vector<INetworkBroadcastSender> getBroadcastSenders() { return sendersBroadcastLances; }

    /**
     * Stops the server 
     */
    public void stop() {
        // Arreter le service d'emission IP pour les connecteurs
        // car il cree un serveur reseau de reception des synchros de connecteurs
        emetteurDeMessages.stopThread();
        for (int i=0; i<sendersBroadcastLances.size(); i++) {
            sendersBroadcastLances.elementAt(i).stopThread();
            sendersBroadcastLances.elementAt(i).waitForBufferEmpty();
            sendersBroadcastLances.removeElementAt(i);
        }
        for (int i=0; i<sendersLances.size(); i++) {
            sendersLances.elementAt(i).stopThread(); // reception pour les plugins de la PF
            sendersLances.elementAt(i).waitForBufferEmpty();
        }
    }

    /**
     * Returns the actual total number of bytes sended on network by the platform since last measure
     * @return the actual total number of bytes sended on network by the platform since last measure
     */
    public int getNetworkPlatformTraffic() {
        int traffic = 0;
        for (int i=0; i<sendersLances.size(); i++) {
            if (sendersLances.elementAt(i) instanceof INetworkTraffic) traffic = traffic+((INetworkTraffic)sendersLances.elementAt(i)).getDataSize();
        }
        for (int i=0; i<sendersBroadcastLances.size(); i++) {
            if (sendersBroadcastLances.elementAt(i) instanceof INetworkTraffic) traffic = traffic+((INetworkTraffic)sendersBroadcastLances.elementAt(i)).getDataSize();
        }
        return traffic;
    }

    /**
     * Returns  the actual total number of bytes sended on network by the connectors since last measure
     * @return the actual total number of bytes sended on network by the connectors since last measure
     */
    public int getNetworkConnectorsTraffic() {
        return emetteurReseau.getNetworkTraffic();
    }
}
