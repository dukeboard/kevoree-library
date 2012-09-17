package network.platform;

import java.util.HashMap;
import network.NetworkEmitterContainer;
import platform.servicesregister.ServicesRegisterManager;
import util.Parameters;

/**
 * Stores messages received for the PF
 * Each service of the PF can register itself in order to have a mailbox
 * The PlatformMessageReceptor collects all incoming messages and dispatch them into registered mailboxes
 * @author Dalmau
 */
public class PlatformMessagesReceptor {

    private HashMap<String, PlatformMessagesMailboxService> inscrits;
    private PlatformMessagesEmitter emetteurDeMessages;

    /**
     * Creates a PlatformMessagesReceptor, prepares the list of open mailboxes.
     */
    public PlatformMessagesReceptor() {
        inscrits = new HashMap<String, PlatformMessagesMailboxService>();
        NetworkEmitterContainer nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        emetteurDeMessages = nec.getPlatformMessagesEmitter();
    }

    /**
     * Depose a message into a mailbox (the owner data in the message indicates in which mailbox the message is stored)
     * @param m message to depose
     */
    public void deposerMessage(NetworkPlatformMessage m) {
        if (m.isLocal()) {
            String pour = m.getOwner();
            PlatformMessagesMailboxService boite = inscrits.get(pour);
            if (boite != null) boite.deposeMessage(m);
            else {
                // Ce cas n'est pas une erreur un message peut arriver pour un service non installe => il est ignore
                // System.err.println("Received message for unknown service: "+pour);
            }
        }
        else {
            System.out.println("Routing platform message to: "+m.getFinalAddress());
            emetteurDeMessages.relayMessage(m);
        }
    }

    /**
     * Get a message from a mailbox
     * @param pour the mailbox owner name
     * @return a message if present or null if none
     */
    public NetworkPlatformMessage retirerMessage(String pour) {
        PlatformMessagesMailboxService boite = inscrits.get(pour);
        if (boite != null) return boite.getMessage();
        else return null;
    }

    /**
     * Get a message from a mailbox like retirerMessage but this method can be interrupted
     * by an InterruptedException. This method is dedicated to BC in order to send messages to
     * local or distant PF. The InterruptedException is used when the BC has to be stopped while trying to send a message.
     * @param pour the mailbox owner name
     * @return a message if present or null if none
     * @throws InterruptedException
     */
    public NetworkPlatformMessage retirerMessageInterruptible(String pour) throws InterruptedException {
        PlatformMessagesMailboxService boite = inscrits.get(pour);
        if (boite != null) return inscrits.get(pour).getMessageInterruptible();
        else return null;
    }

    /**
     * Stops a thread waiting for a message
     * @param pour the name of the service waiting for a message
     */
    public void arreterAttenteMessage(String pour) {
        PlatformMessagesMailboxService boite = inscrits.get(pour);
        if (boite != null) boite.deposeMessage(null);
    }

    /**
     * Registers a service or a BC as having a mailbox
     * @param nom name of the service or the BC
     */
    public void inscription(String nom) {
        PlatformMessagesMailboxService boite = inscrits.get(nom);
        if (boite == null) inscrits.put(nom, new PlatformMessagesMailboxService());
    }

    /**
     * Unregisters a service or a BC having a mailbox
     * @param nom name of the service or the BC
     */
    public void desinscription(String nom) {
        if (inscrits.get(nom) != null) {
            inscrits.get(nom).stop();
            inscrits.remove(nom);
        }
    }

    /**
     * Stops all the mailbox. This method is used when stopping Kalimucho
     */
    public void stop() {
        for (String cle : inscrits.keySet()) {
            inscrits.get(cle).stop();
        }
    }
}
