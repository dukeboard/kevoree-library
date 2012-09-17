package model.interfaces.network;

/**
 *
 * @author Dalmau
 */
public interface INetworkTraffic {

    /**
     * Get the size of data sended or received through network.
     * This interface is implemented by all data senders ans servers on the network in order
     * to allow the platform to get a measure of network traffic.
     * @return the size of data sended or received through network
     */
    public int getDataSize();

}
