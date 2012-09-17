package model.interfaces.network;

import network.connectors.EncapsulatedSample;

/**
 * General interface for all network senders of data for connectors.
 * @author Dalmau
 */
public interface IConnectorDataSender {

    /**
     * Sends data for a connector
     * @param connector name of the connector
     * @param contenu sample to send
     */
    public void sendMessage(String connector, EncapsulatedSample contenu);
    /**
     * Stops the data sender for a connector (when the connector is removed)
     */
    public void close();

}
