package platform.plugins.installables.application;

import network.platform.PlatformMessagesReceptor;
import platform.servicesregister.ServicesRegisterManager;
import network.platform.PlatformMessagesEmitter;
import platform.servicesregister.ServiceClosedException;
import platform.servicesregister.ServiceInUseException;
import model.interfaces.platform.IPlatformPlugin;
import util.Parameters;
import model.osagaia.BCModel;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import util.NetworkAddress;
import network.platform.NetworkPlatformMessage;
import network.AddressesChecker;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Plugin for allowing BCs to send command to local or distant platforms
 * and get the replies from these platforms.
 *
 * @author Dalmau
 */
public class BCCommandSenderPlugin implements IPlatformPlugin {

    private PlatformMessagesReceptor boiteDeReponses;
    private PlatformMessagesEmitter envoi;
    private AddressesChecker addressesChecker;
    private NetworkReceptorContainer rec;
    private boolean arret;
    private boolean timeOut;

    /**
     * Construction :<br>
     * Creates a mailbox manager for BCs to find the replies.<br>
     * Starts a server on a special port number to get the replies from platforms.
     */
    public BCCommandSenderPlugin() {
        NetworkEmitterContainer nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        envoi = nec.getPlatformMessagesEmitter();
        // Mise en place des mecanismes permettant aux plugins de la PF de recevoir les reponses
        // aux commandes qu'ils envoient aux autres PF ou la PF locale
        rec = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        boiteDeReponses = rec.getPlatformMessagesReceptor();
        // creation d'un thread pour gerer les receptions IP pour les plugins
        addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        try { // enregistrer le service d'acces aux commandes de PF pour les BC
            ServicesRegisterManager.registerService(Parameters.APPLICATION_COMMAND_SERVICE, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("BCCommandSenderPlugin service created twice");
        }
    }

    /**
     * Starts the plugin: waiting for replies and putting them in mailboxes for BCs
     */
    public void startPlugin() {
        arret = false;
    }

    /**
     * Stops the main loop of the plugin.
     */
    public void stopPlugin() {
        try {
            ServicesRegisterManager.removeService(Parameters.APPLICATION_COMMAND_SERVICE);
        }
        catch (ServiceClosedException sce) {}
        try { ServicesRegisterManager.removeService(Parameters.APPLICATION_COMMAND_SERVICE); }
        catch (ServiceClosedException sce) { }
        arret = true;
    }

    /**
     * Send a command to a Kalimucho platform
     * @param demandeur the BC which sends the command
     * @param cmd command to send
     * @param destinataire address of the PF to send this message to
     * @throws InterruptedException if the BC is stopped while sending a command
     */
    public void sendCommand(BCModel demandeur, String cmd, NetworkAddress destinataire) throws InterruptedException {
        synchronized (demandeur) {
            if (destinataire != null) {
                if (destinataire.isKnown()) { // l'adresse est correcte
                    if (addressesChecker.isPresentAddress(destinataire)) { // la PF designee est la PF locale
                        sendLocalCommand(demandeur, cmd);
                    }
                    else { // la PF designee est distante
                        NetworkPlatformMessage msg = new NetworkPlatformMessage(Parameters.SUPERVISOR, destinataire.getNormalizedAddress());
                        msg.addContent(cmd);
                        msg.setReplyTo(demandeur.getName());
                        envoi.postMessage(msg); // message envoye par reseau au superviseur distant
                    }
                }
            }
        }
    }

    /**
     * Send a command to the local Kalimucho platform
     * @param demandeur the BC which sends the command
     * @param cmd command to send
     * @throws InterruptedException  if the BC is stopped while sending a command
     */
    public void sendLocalCommand(BCModel demandeur, String cmd) throws InterruptedException {
        synchronized (demandeur) {
            NetworkPlatformMessage msg = new NetworkPlatformMessage(Parameters.SUPERVISOR, "localhost");
            msg.addContent(cmd);
            msg.setReplyTo(demandeur.getName());
            msg.setFinalAddress("local");
            rec.getPlatformMessagesReceptor().deposerMessage(msg); // message directement depose dans la BaL du superviseur
        }
    }

    /**
     * Used by a BC to register itself as a listener for platforms replies
     * @param ecouteur the BC which wants to get platforms' replies
     */
    public synchronized void addReplyListener(String ecouteur) {
        boiteDeReponses.inscription(ecouteur);
    }

    /**
     * Used by a BC to unregister itself as a listener for platforms replies
     * @param ecouteur the BC which wants to unregister itself
     */
    public synchronized void removeReplyListener(String ecouteur) {
        boiteDeReponses.desinscription(ecouteur);
    }

    /**
     * Used by the BCs to get the replies from platforms
     * @param demandeur  the BC which waits for reply
     * @param attente time out (in ms) for waiting for reply
     * @return the platform's replie
     * @throws InterruptedException if the BC is stopped by the platform
     */
    public PlatformReplyMessage getPlatformReply(BCModel demandeur, int attente) throws InterruptedException {
        synchronized (demandeur) {
            String own = demandeur.getName();
            NetworkPlatformMessage recu = null;
            Timer delai = new Timer();
            timeOut = false;
            delai.schedule(new TimeOut(own), attente);
            recu = boiteDeReponses.retirerMessageInterruptible(own);
            delai.cancel();
            if (recu != null) return new PlatformReplyMessage(recu);
            else return null;
        }
    }

    private class TimeOut extends TimerTask {
        private String pour;
        public TimeOut(String own) {
            pour = own;
        }
        public void run() {
            timeOut = true;
            cancel();
            boiteDeReponses.arreterAttenteMessage(pour);
        }
    }

}
