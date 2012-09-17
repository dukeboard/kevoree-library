package network;

import model.interfaces.network.INetworkTraffic;
import util.NetworkAddress;
import network.platform.PlatformMessagesReceptor;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import network.connectors.ConnectorsMailboxes;
import network.platform.ip.IPPlatformReceptionServer;
import network.connectors.ip.IPConnectorsSyncServer;
import network.connectors.ip.IPConnectorsSyncAckServer;
import network.platform.ip.IPPlatformBroadcastReceptionServer;
import java.util.Vector;
import model.interfaces.network.INetworkServer;

// Conteneur des services de gestion du recepteur reseau de l'hote

/**
 * The receptor manager is registered as a service.<br>
 * It creates and manage all the objetcs used by the platform to send data on all available networks.
 *
 * @author Dalmau
 */
public class NetworkReceptorContainer {

    private Vector<NetworkAddress> mesAdresses;
    private NetworkReceptionService recepteurReseau;
    private ConnectorsMailboxes boiteConnecteurs;
    private Vector<INetworkServer> serveursLances;
    private Vector<INetworkServer> serveursBroadcastLances;
    private PlatformMessagesReceptor recepteurPF;

    /**
     * Create the receptor container and registers it as a service.<br>
     * Create the receptor manager and registers it as a service.<br>
     * Create the buffer where messages received for the platform are stored
     *  and registers it as a service.<br>
     * Create the buffer where messages received for the connectors are stored
     *  and registers it as a service.<br>
     * Create the thread which manages receptions for the platform
     *  and registers it as a service.<br>
     * Create the thread which manages receptions for the connectors
     *  and registers it as a service.<br>
     * Create the buffer where messages received for the plugins of the platform are stored
     *  and registers it as a service.<br>
     * Create the thread which manages receptions for the plugins of the platform
     *  and registers it as a service.
     */
    public NetworkReceptorContainer() {
        serveursLances = new Vector<INetworkServer>();
        serveursBroadcastLances = new Vector<INetworkServer>();
        AddressesChecker ac = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        mesAdresses = ac.getAllAddresses();

        // creer le service de creation/suppression des threads de reception reseau pour les connecteurs
        recepteurReseau = new NetworkReceptionService();
        try { // regarder si le service n'a pas deja ete cree
            ServicesRegisterManager.lookForService(Parameters.CONNECTORS_DATA_MAILBOX);
        }
        catch (ServiceClosedException sce) { // si ce n'est pas le cas on le cree
            boiteConnecteurs = new ConnectorsMailboxes();
            try { // enregistrer le service de BaL pour les donnees reeues pour les connecteurs
                ServicesRegisterManager.registerService(Parameters.CONNECTORS_DATA_MAILBOX, boiteConnecteurs);
            }
            catch (ServiceInUseException  mbiue) {
                System.err.println("Connectors mailboxes service for IP created twice");
            }
        }
        // creer la boite a lettre pour les commandes de la PF
        recepteurPF = new PlatformMessagesReceptor();

        // mise en place des services selon les types de reseau
        for (int i=0; i<mesAdresses.size(); i++) {
            switch (mesAdresses.elementAt(i).getType()) {
                case NetworkAddress.TYPE_IPV4 : // envoi par IP
                    // creation d'un thread pour gerer les receptions IP pour les connecteurs
                    IPConnectorsSyncAckServer emetteurIP = new IPConnectorsSyncAckServer(mesAdresses.elementAt(i)); // creer le service d'emission reseau (utilise Parameters.CONNECTORS_ACKNOWLEDGE)
                    serveursLances.addElement(emetteurIP);
                    IPConnectorsSyncServer reccip = new IPConnectorsSyncServer(mesAdresses.elementAt(i), Parameters.PORT_IP_DATA_CONNECTORS);
                    serveursLances.addElement(reccip);
                    // creation d'un thread pour gerer les receptions IP par la PF
                    IPPlatformReceptionServer recpfip = new IPPlatformReceptionServer(mesAdresses.elementAt(i), recepteurPF, Parameters.PORT_IP_COMMANDS_PF);
                    serveursLances.addElement(recpfip);
                    // creation d'un thread pour gerer les receptions en broadcast par la PF
                    if (Parameters.BROADCAST_AVAILABLE) {
                        INetworkServer recpfbcst = new IPPlatformBroadcastReceptionServer(mesAdresses.elementAt(i), recepteurPF, Parameters.PORT_UDP_BROADCAST_PF);
                        serveursBroadcastLances.addElement(recpfbcst);
                    }
                    break;
                default :
                    break;
            }
        }

        try { // enregistrer ce conteneur
            ServicesRegisterManager.registerService(Parameters.NETWORK_RECEPTIONS_CONTAINER, this);
        }
        catch (ServiceInUseException  siue) {
            System.err.println("Network emission container service created twice");
        }
    }

    /**
     * Returns the service to create/remove threads for distributed connectors that receive data
     * @return the service to create/remove threads for distributed connectors that receive data
     */
    public NetworkReceptionService getNetworkReceptionService() { return recepteurReseau; }
    /**
     * Returns the service of mailbox for received messages for the platform
     * @return the service of mailbox for received messages for the platform
     */
    public PlatformMessagesReceptor getPlatformMessagesReceptor() { return recepteurPF; }

    /**
     * Stops the server that receives messages for the platform
     * and the server that receives messages for the plugins of the platform
     */
    public void stop() {
        // arret des serveurs de reception de messages des plateformes
        for (int i=0; i<serveursBroadcastLances.size(); i++) {
            serveursBroadcastLances.elementAt(i).stopThread();
        }
        for (int i=0; i<serveursLances.size(); i++) {
            serveursLances.elementAt(i).stopThread(); // reception pour les plugins de la PF
        }
        recepteurPF.stop();
    }

    /**
     * Returns the actual total number of bytes received on network for the platform since last measure
     * @return the actual total number of bytes received on network for the platform since last measure
     */
    public int getNetworkPlatformTraffic() {
        int traffic = 0;
        for (int i=0; i<serveursLances.size(); i++) {
            if (serveursLances.elementAt(i) instanceof INetworkTraffic) traffic = traffic+((INetworkTraffic)serveursLances.elementAt(i)).getDataSize();
        }
        for (int i=0; i<serveursBroadcastLances.size(); i++) {
            if (serveursBroadcastLances.elementAt(i) instanceof INetworkTraffic) traffic = traffic+((INetworkTraffic)serveursBroadcastLances.elementAt(i)).getDataSize();
        }
        return traffic;
    }

    /**
     * Returns the actual total number of bytes received on network for the connectors since last measure
     * @return the actual total number of bytes received on network for the connectors since last measure
     */
    public int getNetworkConnectorsTraffic() {
        return recepteurReseau.getNetworkTraffic();
    }

}
