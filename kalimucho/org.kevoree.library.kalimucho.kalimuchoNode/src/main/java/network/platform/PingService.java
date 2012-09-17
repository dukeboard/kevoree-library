package network.platform;

import util.NetworkAddress;
import util.Parameters;
import java.util.TimerTask;
import java.util.Timer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import platform.plugins.installables.network.DNS.KalimuchoDNS;

/**
 * This service offers a ping mechanism to test if a connexion with a given host is possible directly or not.
 * This service is used to find a route (by the routing service).
 * If ping does not reply then a broadcast/multicast message is send to find a route.
 * @author Dalmau
 */
public class PingService extends Thread {

    private PlatformMessagesReceptor boiteDeReponses;
    private PlatformMessagesEmitter envoi;
    private NetworkReceptorContainer rec;
    static private boolean actif = false;
    private boolean abandon;
    private NetworkPlatformMessage reponseRecue;
    private Object semaphore;

    /**
     * The service for sending and replyiong to ping messages
     */
    public PingService() {
        try {
            NetworkEmitterContainer nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
            envoi = nec.getPlatformMessagesEmitter();
            // Mise en place des mecanismes permettant aux plugins de la PF de recevoir les reponses
            // aux commandes qu'ils envoient aux autres PF ou la PF locale
            rec = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
            boiteDeReponses = rec.getPlatformMessagesReceptor();
            semaphore = new Object();
            ServicesRegisterManager.registerService(Parameters.NETWORK_PING_SERVICE, this);
            actif = true;
            boiteDeReponses.inscription(Parameters.NETWORK_PING_SERVICE);
            start();
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Ping service already created");
        }
    }

    /**
     * Stops the PING service (normally this service is stopped only when the platform stops)
     */
    public void stopThread() {
        if (!actif) return;
        actif = false;
        boiteDeReponses.stop();
        boiteDeReponses.desinscription(Parameters.NETWORK_PING_SERVICE);
    }

    /**
     * Wait for messages about the ping mechanism.<br>
     * When a pig message is received, this thread replies to it.<br>
     * When a ping reply is received, the thread that used the ping mechanism is unlocked.
     */
    @Override
    public void run() {
        while (actif) {
            reponseRecue = boiteDeReponses.retirerMessage(Parameters.NETWORK_PING_SERVICE);
            if (reponseRecue != null) {
                String msg = reponseRecue.getContent();
                if (msg.equals(Parameters.PING_MESSAGE)) {
                    NetworkPlatformMessage env = new NetworkPlatformMessage(Parameters.NETWORK_PING_SERVICE, reponseRecue.getExpeditorAddress());
                    env.addContent(Parameters.PING_REPLY_MESSAGE);
                    envoi.postUrgentDirectMessage(env);
                }
                else {
                    debloquer();
                }
            }
        }
    }
    /**
     * Sends a ping and wait for reply or time out
     * @param adr address to send the ping to
     * @return true if a reply has been received
     */
    public synchronized boolean testConnexionTo(NetworkAddress adr) {
        synchronized(semaphore) {
            NetworkPlatformMessage env = new NetworkPlatformMessage(Parameters.NETWORK_PING_SERVICE, adr.getNormalizedAddress());
            env.addContent(Parameters.PING_MESSAGE);
            long t0 = System.currentTimeMillis();
            abandon = false;
            envoi.postPingMessage(env);
            Timer delai = new Timer();
            delai.schedule(new WaitForTimeOut(), Parameters.MAXIMAL_WAIT_FOR_PING);
            try {
                wait();
                delai.cancel();
                if (abandon) return false;
                else {
                    try {
                        KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                        long t1 = System.currentTimeMillis();
                        long tDistant = reponseRecue.getDate();
                        long delta = ((t0 + t1)/2) - tDistant; // valeur a ajouter a l'horloge distante pour avoir l'heure locale
    //System.out.println("decalage "+delta);
                        dns.adjustRemoteHostClockShift(reponseRecue.getSenderID(), delta, t1-t0);
                    }
                    catch (ServiceClosedException nfe) {}
                    return true;
                }
            }
            catch (InterruptedException ie) {
                delai.cancel();
                return false;
            }
        }
    }

    private synchronized void  debloquer() {
        notifyAll();
    }

    /**
     * This methods stops the ping service for waiting for a reply.
     * It is used when PIG time out occurs or when the pinged host is not reachable (socket error)
     */
    public synchronized void  abandonner() {
        abandon = true;
        notifyAll();
    }

    private class WaitForTimeOut extends TimerTask {
         public void run() {
             abandonner();
         }
     }

}
