package platform.plugins.installables.network.routing;

import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import java.util.Vector;
import model.interfaces.platform.IPlatformPlugin;
import java.util.TimerTask;
import java.util.Timer;
import util.NetworkAddress;
import network.platform.PingService;
import platform.plugins.PlatformPluginsLauncher;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import network.platform.NetworkPlatformMessage;
import network.platform.PlatformMessagesReceptor;
import network.AddressesChecker;
import platform.plugins.installables.network.DNS.KalimuchoDNS;

/**
 * Service for finding a route to a given host.<br>
 * This service starts by trying a ping to the host.<br>
 * If the ping does not reply then it uses a broadcast/multicast message.<br>
 * The route is limited to one hop (only a relay between two hosts) because Kalimucho only create on hops relays connectors.<br>
 * In reality it is not realing a routing service as usually used in networks but a proxy service.
 * The aim is to find an host that can create a link between two differents networks (IP and zigbee for example).
 * @author Dalmau
 */
public class RoutingService extends Thread implements IPlatformPlugin, IRoutingService {

    private Vector<NetworkPlatformMessage> boiteALettres; // stocke les reponses recues
    private boolean fin; // pour terminer lorsqu'il n'y a pas de route
    private int tentatives; // nombre de tentatives de recherche de route
    private PingService ping;
    private NetworkReceptorContainer rec;
    private NetworkEmitterContainer nec;
    private PlatformMessagesReceptor boiteDeReponses;
    private AddressesChecker addressesChecker;
    private long debutRecherche;
    private boolean actif = false;
    private Object semaphore;

    /**
     * Create the routing service
     */
    public RoutingService() {
        boiteALettres = new Vector<NetworkPlatformMessage>();
        nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        rec = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        boiteDeReponses = rec.getPlatformMessagesReceptor();
        semaphore = new Object();
        addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        // enregistrer le service de routage
        try { 
            ServicesRegisterManager.registerService(Parameters.NETWORK_ROUTING_SERVICE, this);
            try {
                ping = (PingService)ServicesRegisterManager.lookForService(Parameters.NETWORK_PING_SERVICE);
            }
            catch (ServiceClosedException sce) {
                PlatformPluginsLauncher lanceur = (PlatformPluginsLauncher)ServicesRegisterManager.platformWaitForService(Parameters.PLUGINS_LAUNCHER);
                String plug = PingService.class.getName();
                try {
                    lanceur.installPlugin(plug);
                }
                catch (ClassNotFoundException cnfe) {
                    System.err.println("Can't start plugin "+plug+" : class unknown");
                }
                catch (InstantiationException cnfe) {
                    System.err.println("Can't start plugin "+plug+" : class can't be instantiated");
                }
                catch (IllegalAccessException cnfe) {
                    System.err.println("Can't start plugin "+plug+" : Illegal access to the class");
                }
            }
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("IP neighborhood created twice");
        }
    }

    /**
     * Starts the plugin
     */
    public void startPlugin() {
        if (actif) return;
        actif = true;
        ping = (PingService)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_PING_SERVICE);
        boiteDeReponses.inscription(Parameters.NETWORK_ROUTING_SERVICE);
        start();
    }


    /**
     * Stops the server that receives broadcast messages and the server thar receives replies to these broadcast messages
     */
    public void stopPlugin() {
        if (!actif) return;
        actif = false;
        try {
            ServicesRegisterManager.removeService(Parameters.NETWORK_ROUTING_SERVICE);
        }
        catch (ServiceClosedException sce) {}
        boiteDeReponses.stop();
        boiteDeReponses.desinscription(Parameters.NETWORK_ROUTING_SERVICE);
    }

    /**
     * Depose a reply message to send
     * @param message reply message to send
     */
    public synchronized void depose(NetworkPlatformMessage message) {
        // depot d'une reponse recue au routage
        boiteALettres.addElement(message);
        notifyAll();
    }

    private synchronized NetworkPlatformMessage retire(NetworkAddress toFind) {
        // retrait d'une reponse au routage
        boolean trouve = false;
        NetworkPlatformMessage recu = null;
        try {
            while (!trouve) { // attente de reponse a la question posee
                while ((boiteALettres.size() == 0) && (!fin)) {
                    wait(); // attente d'une reponse
                }
                if (!fin) { // le delai d'attente n'est pas ecoule
                    recu = boiteALettres.firstElement();
                    boiteALettres.removeElementAt(0);
                    // verifier qu'il s'agisse bien d'une reponse a la question posee
                    // Ce test est necessaire car des reponses a une question anterieure
                    // peuvent encore arriver.
                    String[] parties = recu.getContent().split(";");
                    if (toFind.equals(new NetworkAddress(parties[0]))) trouve = true;
                    else recu = null;
                }
                else trouve = true; // on s'arrete faute de reponse
            }
            if (recu != null) {
                try {
                    KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    long t1 = System.currentTimeMillis();
                    long tDistant = recu.getDate();
                    long delta = ((debutRecherche + t1)/2) - tDistant; // valeur a ajouter a l'horloge distante pour avoir l'heure locale
                    dns.adjustRemoteHostClockShift(recu.getSenderID(), delta, t1-debutRecherche);
                }
                catch (ServiceClosedException nfe) {}
            }
            return recu; // route recue ou null si fin du delai
        }
        catch (InterruptedException ie) {
            return null;
        }
    }

    /**
     * Performs the treatment of received messages for finding a route
     * @param message received message
     */
    private void traiterDemandes(NetworkPlatformMessage message) {
       String[] parties = message.getContent().split(";");
       NetworkAddress who = new NetworkAddress(parties[1]);
       NetworkAddress what = new NetworkAddress(parties[0]);
       NetworkAddress destinataire = new NetworkAddress(message.getExpeditorAddress());
       NetworkAddress monAdr = addressesChecker.getMyFirstAddress(who.getType());
       if (addressesChecker.isPresentAddress(what)) { // la route demandee est vers moi
            if (who.equals(destinataire)) {
               // repondre au demandeur en indiquant que la route est directe
               NetworkPlatformMessage reponse = new NetworkPlatformMessage();
               reponse.setFinalAddress("local"); // on repond directement au demandeur
               reponse.setFinalPort(0);
               reponse.addContent(message.getContent()+";"+monAdr.getNormalizedAddress()+";direct");
               reponse.setAddress(destinataire.getNormalizedAddress()); // on repond directement au demandeur
               reponse.setPortNumber(0);
               reponse.setOwner(Parameters.NETWORK_ROUTING_SERVICE);
               reponse.setReplyTo(Parameters.NETWORK_ROUTING_SERVICE);
//System.out.println("Sending reply to route: "+reponse.getContent()+" for "+destinataire.getNormalizedAddress()+" to "+reponse.getFinalAddress()+" via "+reponse.getAddress());
               nec.getPlatformMessagesEmitter().postDirectMessage(reponse);
            }
            else {
                // repondre au demandeur en indiquant que la route est indirecte et que
                // l'hote par lequel ce message est arrive sert de relai
                NetworkPlatformMessage reponse = new NetworkPlatformMessage();
                reponse.setFinalAddress(who.getNormalizedAddress()); // on repond au demandeur
                reponse.setFinalPort(0);
                reponse.addContent(message.getContent()+";"+destinataire.getNormalizedAddress()+";indirect");
                reponse.setAddress(destinataire.getNormalizedAddress()); // on repond via l'hote par lequel ce message est arrive
                reponse.setPortNumber(0);
                reponse.setOwner(Parameters.NETWORK_ROUTING_SERVICE);
                reponse.setReplyTo(Parameters.NETWORK_ROUTING_SERVICE);
//System.out.println("Sending reply to route: "+reponse.getContent()+" for "+destinataire.getNormalizedAddress()+" to "+reponse.getFinalAddress()+" via "+reponse.getAddress());
                nec.getPlatformMessagesEmitter().postDirectMessage(reponse);
            }
        }
        else {
            if (who.equals(destinataire)) { // La demande est parvenue directement et l'@ recherche n'est pas celle de cet hote
                // On va voir si on peut servir de relai, dans le cas contraire on renvoie la demande en broadcast
                if (ping.testConnexionTo(what)) { // on a trouve la route vers l'hote recherche par PING
                    // repondre au demandeur en indiquant que la route est indirecte et que
                    // cet hote peut servir de relai
                    NetworkPlatformMessage reponse = new NetworkPlatformMessage();
                    if (who.equals(destinataire)) reponse.setFinalAddress("local"); // la reponse se fait directement car la demande est arrivee directement
                    else reponse.setFinalAddress(who.getNormalizedAddress()); // la reponse se fait via l'hote par lequel cette demande nous est parvenue
                    reponse.setFinalPort(0);
                    reponse.addContent(message.getContent()+";"+monAdr.getNormalizedAddress()+";indirect");
                    reponse.setAddress(destinataire.getNormalizedAddress()); // on envoie la reponse a l'hote par lequel cette demande nous est parvenue
                    reponse.setPortNumber(0);
                    reponse.setOwner(Parameters.NETWORK_ROUTING_SERVICE);
                    reponse.setReplyTo(Parameters.NETWORK_ROUTING_SERVICE);
//System.out.println("Sending reply to route: "+reponse.getContent()+" for "+destinataire.getNormalizedAddress()+" to "+reponse.getFinalAddress()+" via "+reponse.getAddress());
                    nec.getPlatformMessagesEmitter().postDirectMessage(reponse);
                }
                else { // on ne peut pas atteindre l'hote recherche directement
                    message.setExpeditorAdressWhenSending();
                    nec.getPlatformMessagesEmitter().postBroadcastMessage(message); // on redifuse en broadcast
                }
            } // les messages pour lesquels on n'a pas de reponse et qui ne nous sont pas parvenu directement sont ignores
        }
    }

    /**
     * Find a route
     * @param vers to which host
     * @return the founded route
     * @throws NoRouteException if there is no route
     */
    public ReplyForRouteMessage findRoute(NetworkAddress vers) throws NoRouteException {
        synchronized(semaphore) {
            debutRecherche = System.currentTimeMillis();
            // recherche d'une route vers l'@ passee en parametre
            if (addressesChecker.isPresentAddress(vers)) return new ReplyForRouteMessage(vers.getNormalizedAddress(), vers.getNormalizedAddress(), vers.getNormalizedAddress(), "direct");
            System.out.println("Try finding route to: "+vers.getNormalizedAddress()+" with PING");
            if (ping.testConnexionTo(vers)) { // on a trouve la route directement
                ReplyForRouteMessage recu = new ReplyForRouteMessage(addressesChecker.getMyFirstAddress(vers.getType()).getNormalizedAddress(), vers.getNormalizedAddress(), vers.getNormalizedAddress(), "direct");
                System.out.println("Ping found a direct route to: "+vers.getNormalizedAddress());
//System.out.println("TPS recherche route par ping : "+(System.currentTimeMillis()-debutRecherche));
                return recu;
            }
            System.out.println("Try finding route to: "+vers.getNormalizedAddress()+" with broadcast");
            // Si ping n'a pas marche on va s'adresser au serveur pour trouver la route
            boiteALettres.removeAllElements(); // virer les reponses anterieures
            Timer delai = new Timer(); // timer pour eviter d'attendre trop longtemps
            NetworkPlatformMessage recu = null;
            boolean trouve = false; // pas de reponse pour le moment
            tentatives = 0;
            while ((tentatives < Parameters.NUMBER_RETRIES_FOR_ROUTE) && (!trouve)) { // faire une tentative
                // Envoi de la requete sur chaque reseau
                NetworkPlatformMessage envoi = new NetworkPlatformMessage();
                envoi.setFinalAddress("local");
                envoi.addContent(vers.getNormalizedAddress()+";");
                envoi.setOwner(Parameters.NETWORK_ROUTING_SERVICE);
                envoi.setReplyTo(Parameters.NETWORK_ROUTING_SERVICE);
                envoi.setExpeditorAdressWhenSending();
                debutRecherche = System.currentTimeMillis();
                nec.getPlatformMessagesEmitter().postBroadcastIncompleteMessage(envoi);
                // On allonge le temps d'attente a chaque nouvelle tentative
                delai.schedule(new WaitForReply(), Parameters.MAXIMAL_WAIT_FOR_RETRY_ROUTE * (tentatives+1));
                fin = false; // le delai d'attente n'est pas ecoule
                recu = null;
                while ((!fin) && (!trouve)) { // attente reponse ou fon de delai
                    recu = retire(vers); // retirer la reponse
                    if (recu != null) trouve = true; // on a trouve une route
                    delai.cancel(); // arreter le timer
                }
                if (!trouve) { // reessayer une fois
                    delai = new Timer();
                    tentatives++;
                    if (tentatives < Parameters.NUMBER_RETRIES_FOR_ROUTE) {
                        System.out.println("Retrying finding route to: "+vers.getNormalizedAddress());
                    }
                }
            }
            if (trouve) { // on a obtenu une reponse => route trouvee
                String[] parties = recu.getContent().split(";");
                ReplyForRouteMessage reponse = new ReplyForRouteMessage(parties[1], parties[0], parties[2], parties[3]);
                System.out.println("Found route to: "+vers.getNormalizedAddress()+" via: "+reponse.getVia());
//System.out.println("TPS recherche route par broadcast : "+(System.currentTimeMillis()-debutRecherche));
                return reponse; // renvoyer la route trouvee
            }
            else { // si pas de route apres n tentatives lever une exception
                throw new NoRouteException();
            }
        }
    }

    /**
     * Waits for messages concerning the routes.<br>
     * When a message asking for a route is received, the thread tries to reply to it:<br>
     * - a direct route exists if this host is the one for which a route is requested and it receives directly this request<br>
     * - an indirect route exists if this host is the one for which a route is requested and it receives indirectly this request<br>
     * - an indirect route exists if this host is not the one for which a route is requested and it can reach the requested host<br>
     * If it can reply, it relays this request.
     * When a message answering to a route is received, the thread unlock the route requester.
     *
     */
    @Override
    public void run() {
        while (actif) {
            NetworkPlatformMessage recu = boiteDeReponses.retirerMessage(Parameters.NETWORK_ROUTING_SERVICE);
            if (recu != null) {
//System.out.println("*** Routage a recu : "+recu.getContent()+" de "+recu.getExpeditorAddress());
                String msg = recu.getContent();
                if (!msg.contains("direct")) { // message de demande
                    traiterDemandes(recu);
                }
                else { // message de reponse
                    depose(recu);
                }
            }
        }
    }

    private synchronized void arreter() {
        notifyAll(); // debloquer le thread qui attend une route
    }
    
     private class WaitForReply extends TimerTask {
         public void run() { // le delai d'attente est passe
             fin = true;
             arreter(); // debloquer le thread demandeur
         }
     }

}
