package model.interfaces.network;

/**
 * Interface of network senders of acknowledge messages for connectors.
 * This interface is used by the connectors' mailboxes to send an acknowledge when data is got by the connector.
 * @author Dalmau
 */
public interface IConnectorACKSender {

    /**
     * Send an acknowledge message. This method is used by the mailbox when the connector reads a sample
     */
    public void sendACK();
    /**
     * Stops the acknowledges sender
     */
    public void close();
}
