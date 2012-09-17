package network.platform;

import util.Parameters;
import platform.plugins.installables.network.routing.IRoutingService;
import platform.plugins.installables.network.routing.ReplyForRouteMessage;
import platform.plugins.installables.network.routing.NoRouteException;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import model.interfaces.network.INetworkBroadcastSender;
import network.AddressesChecker;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.NetworkAddress;
import java.util.Vector;

// Classe qui recoit les demandes d'emission de messages
// par le superviseur et les traite

/**
 * Sends on network the messages of the platform.<br>
 * This class offers method to :<br>
 * Send data directly to a host on the same network<br>
 * Send data to a host using a route finding mechanism in order to find a relay platform if the host is not directly accessible<br>
 * Send data by broadcast or multicast when no specific adressee if known
 *
 * @author Dalmau
 */
public class PlatformMessagesEmitter extends Thread {

    private AddressesChecker addressesChecker;
    private Vector<INetworkBroadcastSender> emetteursBroadcastLances;
    private NetworkEmitterContainer emitterContainer;
    private NetworkReceptorContainer receptorContainer;
    private Vector<MessageToSend> aEnvoyer;
    private boolean enMarche;
    private String myID;
    private ContextManager gestionnaireContexte;

    /**
     * Creates the PF message emitter.
     * @param ac The platform addresses checher (knows alla the addresses of the host)
     * @param nec The network emitter container of the platform
     */
    public PlatformMessagesEmitter(AddressesChecker ac, NetworkEmitterContainer nec) {
        addressesChecker = ac;
        myID = ac.getHostIdentifier();
        emitterContainer = nec;
        receptorContainer = null; // pas encore accessible lors de la creation du PlatformMessagesEmitter
        emetteursBroadcastLances = emitterContainer.getBroadcastSenders();
        aEnvoyer = new Vector<MessageToSend>();
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        enMarche = false;
    }

    /**
     * Stats the platform emitter. It is a thread that sends the messages deposed by the platform.
     */
    public void startThread() {
        enMarche = true;
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Stops the platform messages emitter
     */
    public void stopThread() {
        enMarche = false;
    }
    
    private synchronized MessageToSend attendreMessage() {
        while ((aEnvoyer.size() == 0) && enMarche) {
            try { wait(); }
            catch (InterruptedException ie) { enMarche = false; }
        }
        if (enMarche) {
            MessageToSend msg = aEnvoyer.firstElement();
            aEnvoyer.removeElementAt(0);
            return msg;
        }
        else return null;
    }

    /**
     * Sends messages deposed by the platform.
     * A messages is deposed indicationg if there is a known direct route to the addresee or if a route as to be found.
     * The thread looks for a route if necessary and sends the message.
     */
    @Override
    public void run() {
        while (enMarche) {
            MessageToSend msgDescr = attendreMessage();
            if (msgDescr != null) {
                NetworkPlatformMessage msg = msgDescr.getMessage();
                if (!msgDescr.isDirect()) { // envoi d'un message avec recherche de route
                    if (msg != null) {
                        if (msg.getAddress().equals("localhost")) postLocalMessage(msg);
                        else {
                            NetworkAddress dest = new NetworkAddress(msg.getAddress()); // adresse du destinataire
                            int port = msg.getPortNumber();
                            try {
                                ReplyForRouteMessage route = findRoute(dest);
                                msg.setAddress(route.getVia());
                                if (route.isDirect()) { // on peut envoyer ce message en direct
                                    msg.setFinalAddress("local"); // message envoye a son destinataire final
                                }
                                else { // envoi du message par relais
                                    msg.setPortNumber(0); // le numero de port sera ajoute avant envoi (port normal de la PF)
                                    msg.setFinalAddress(dest.getNormalizedAddress()); // message devant etre relaye vers dest
                                    msg.setFinalPort(port);
                                }
                                postUrgentDirectMessage(msg);
                            }
                            catch (NoRouteException nre) {
                                gestionnaireContexte.signalEvent(new ContextInformation("Sending Platform message: no route to "+dest.getNormalizedAddress()));
                            }
                        }
                    }
                    else enMarche = false;
                }
                else { // envoi d'un message direct
                    postUrgentDirectMessage(msg);
                }
            }
            else enMarche = false;
        }
        enMarche = false;
    }

    /**
     * Find a route to an addressee
     * @param vers to who the route is to be found
     * @return a route descriptor
     * @throws NoRouteException if there is no route to this addressee
     */
    public synchronized ReplyForRouteMessage findRoute(NetworkAddress vers) throws NoRouteException {
        try { // regarder si le service de routage est en marche
            IRoutingService routage = (IRoutingService)ServicesRegisterManager.lookForService(Parameters.NETWORK_ROUTING_SERVICE);
            return routage.findRoute(vers); // si oui renvoyer la route qu'il donne
        }
        catch(ServiceClosedException sce) { // sinon on essaye avec PING
            try { // regarder si le service de PING est en marche
                PingService ping = (PingService)ServicesRegisterManager.lookForService(Parameters.NETWORK_PING_SERVICE);
                if (ping.testConnexionTo(vers)) { // si PING a marche on a une route directe
                    return new ReplyForRouteMessage(addressesChecker.getMyFirstAddress(vers.getType()).getNormalizedAddress(), vers.getNormalizedAddress(), vers.getNormalizedAddress(), "direct");
                }
                else throw new NoRouteException(); // si PING n'a rien donne on n'a pas de route
            }
            catch(ServiceClosedException scep) { // sinon on considere toujours que la route est directe
               return new ReplyForRouteMessage(addressesChecker.getMyFirstAddress(vers.getType()).getNormalizedAddress(), vers.getNormalizedAddress(), vers.getNormalizedAddress(), "direct");
            }
        }
    }

    /**
     * Post an urgent message without looking for a route. The message is send before all actually waiting messages.
     * @param msg the message to send urgently
     */
    public void postUrgentDirectMessage(NetworkPlatformMessage msg) {
        if (msg.getAddress().equals("localhost")) postLocalMessage(msg); // cas d'envoi d'un message a soi meme
        else { // cas d'envoi d'un message a une autre PF
            NetworkAddress dest = new NetworkAddress(msg.getAddress()); // adresse du destinataire
            msg.setSenderID(myID);
            msg.setExpeditorID(myID);
            emitterContainer.findPlatformSenderFor(dest.getType()).posterMessageUrgent(msg); // envoi selon le type de reseau du destinataire
        }
    }

    /**
     * Post a special PING message (this message is sent before all others waiting messages)
     * @param msg the PING message to post
     */
    public void postPingMessage(NetworkPlatformMessage msg) {
        if (msg.getAddress().equals("localhost")) postLocalMessage(msg); // cas d'envoi d'un message a soi meme
        else { // cas d'envoi d'un message a une autre PF
            NetworkAddress dest = new NetworkAddress(msg.getAddress()); // adresse du destinataire
            msg.setSenderID(myID);
            msg.setExpeditorID(myID);
            emitterContainer.findPlatformSenderFor(dest.getType()).posterMessagePING(msg); // envoi selon le type de reseau du destinataire
        }
    }

    /**
     * Post a slow message without looking for a route. The message is send after all other types of actually waiting messages.
     * @param msg the message to send slowly
     */
    public void postSlowDirectMessage(NetworkPlatformMessage msg) {
        if (msg.getAddress().equals("localhost")) postLocalMessage(msg); // cas d'envoi d'un message a soi meme
        else { // cas d'envoi d'un message a une autre PF
            NetworkAddress dest = new NetworkAddress(msg.getAddress()); // adresse du destinataire
            msg.setSenderID(myID);
            msg.setExpeditorID(myID);
            emitterContainer.findPlatformSenderFor(dest.getType()).posterMessageLent(msg); // envoi selon le type de reseau du destinataire
        }
    }

    // Envoi d'un message complet ou relai de message sans recherche de route
    /**
     * Sends a message when the route is direct
     * @param msg message do send
     */
    public synchronized void postDirectMessage(NetworkPlatformMessage msg) {
        msg.setSenderID(myID);
        msg.setExpeditorID(myID);
        aEnvoyer.addElement(new MessageToSend(msg, true));
        notifyAll();
    }
    
    // Envoi d'un message complet avec recherche de route
    /**
     * Sends a message with finding the route
     * @param msg message to send
     */
    public synchronized void postMessage(NetworkPlatformMessage msg) {
        if (msg.getPortNumber() == Parameters.PORT_IP_DEPLOYMENT_PF) {
            // pas de recherche de route pour envoyer au module de deploiement car il n'implemente pas de service de routage
            postDirectMessage(msg);
        }
        else {
            msg.setSenderID(myID);
            msg.setExpeditorID(myID);
            aEnvoyer.addElement(new MessageToSend(msg, false));
            notifyAll();
        }
    }


    private void postLocalMessage(NetworkPlatformMessage msg) { // envoi d'un message a soi meme
        msg.setSenderID(myID);
        msg.setExpeditorID(myID);
        if (receptorContainer == null) receptorContainer = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        receptorContainer.getPlatformMessagesReceptor().deposerMessage(msg); // message directement depose dans la BaL du service
    }

    /**
     * Broadcasts a message on each accessible network
     * @param msg message to broadcast
     */
    public void postBroadcastMessage(NetworkPlatformMessage msg) {
        msg.setSenderID(myID);
        msg.setExpeditorID(myID);
        for (int i=0; i<emetteursBroadcastLances.size(); i++) {
            emetteursBroadcastLances.elementAt(i).posterMessage(msg);
        }
    }

    /**
     * Broadcasts a message on each accessible network.<br>
     * Before broadcasting it, the message is completed by the local address of the host on the network
     * on which it is broadcasted (this address is added at the end of the content part of the message (String))
     * @param msg message to complete and broadcast
     */
    public void postBroadcastIncompleteMessage(NetworkPlatformMessage msg) {
        msg.setSenderID(myID);
        msg.setExpeditorID(myID);
        for (int i=0; i<emetteursBroadcastLances.size(); i++) {
            emetteursBroadcastLances.elementAt(i).posterMessageIncomplet(msg);
        }
    }

    /**
     * Relays a message to another platform
     * @param msg message to relay
     */
    public void relayMessage(NetworkPlatformMessage msg) {
        msg.setAddress(msg.getFinalAddress()); // on renvoie au destinataire final
        msg.setPortNumber(msg.getFinalPort()); // sur le numero de port indique
        msg.setFinalAddress("local"); // c'est le destinataire final
        msg.setExpeditorID(msg.getSenderID());
        msg.setSenderID(myID);
        NetworkAddress dest = new NetworkAddress(msg.getAddress()); // adresse du destinataire
        emitterContainer.findPlatformSenderFor(dest.getType()).posterMessage(msg); // envoi selon le type de reseau du destinataire
    }

    private class MessageToSend {
        private NetworkPlatformMessage message;
        private boolean direct;

        public MessageToSend(NetworkPlatformMessage msg, boolean dir) {
            message = msg;
            direct = dir;
        }

        public boolean isDirect() { return direct; }
        public NetworkPlatformMessage getMessage() { return message; }
    }
}
