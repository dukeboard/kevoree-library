package platform.plugins.installables.application;

import util.NetworkAddress;
import network.platform.NetworkPlatformMessage;

/**
 * Stores the important information of a platform reply:<br>
 * The messsage and the address of the sender.
 * @author Dalmau
 */
public class PlatformReplyMessage {

    private String contenu;
    private NetworkAddress expediteur;

    /**
     * Creates a message for a BC which sent a command to a PF and receives a reply
     * @param msg the internal PF message received as reply
     */
    public PlatformReplyMessage(NetworkPlatformMessage msg) {
        contenu = msg.getContent();
        expediteur = new NetworkAddress(msg.getAddress());
    }

    /**
     * Returns the content of the reply
     * @return the content of the reply
     */
    public String getContent() { return contenu; }

    /**
     * Returns the address of the PF that has replied
     * @return the address of the PF that has replied
     */
    public NetworkAddress getAddress() { return expediteur; }
}
