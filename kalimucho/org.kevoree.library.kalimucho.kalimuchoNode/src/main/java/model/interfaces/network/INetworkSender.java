package model.interfaces.network;

import network.platform.NetworkPlatformMessage;

/**
 *
 * @author Dalmau
 */
public interface INetworkSender extends INetworkBroadcastSender {


    /**
     * Sends an urgent message. This message is put in a queue and sent by the sender's thread first.
     * @param tr the message to send
     */
    public void posterMessageUrgent(NetworkPlatformMessage tr);
    /**
     * Sends a slow message. This message is put in a queue and sent by the sender's thread when neither urgent neither normal messages are pending.
     * @param tr the message to send
     */
    public void posterMessageLent(NetworkPlatformMessage tr);

    /**
     * Sends a PING message. This message is sent in priority (before all others waiting messages)
     * @param tr the message to send
     */
    public void posterMessagePING(NetworkPlatformMessage tr);

}
