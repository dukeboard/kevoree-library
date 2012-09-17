package model.interfaces.network;

import network.platform.NetworkPlatformMessage;
import util.NetworkAddress;

/**
 * General interface for all messages senders on a network.
 * There are three kinds of messages:
 *  - urgent message that are sent first
 *  - normal messages that are sent when no urgent message is pending
 *  - slow messages that are sent when neither urgent neither normal messages are pending
 * @author Dalmau
 */
public interface INetworkBroadcastSender {

    /**
     * Sends a normal message. This message is put in a queue and sent by the sender's thread when no urgent message is pending.
     * @param tr the message to send
     */
    public void posterMessage(NetworkPlatformMessage tr);

    /**
     * Sends a message. This message is put in a queue and sent by the sender's thread when no urgent message is pending.
     * This message is completed by the address of the sending host on this network
     * @param tr message to complete and to send
     */
    public void posterMessageIncomplet(NetworkPlatformMessage tr);

    /**
     * Stops the sender. The senders goes on running until there are no more messages to send, then stops
     */
    public void stopThread();

    /**
     * Returns the local host address on the network on which this sender works
     * @return the local host address on the network on which this sender works
     */
    public NetworkAddress getSenderAddress();

    /**
     * The requesting thread is locked on a semaphore until there are no more messages to send
     */
    public void waitForBufferEmpty();
}
