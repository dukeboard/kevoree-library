package model.interfaces.network;

/**
 * General interface for all the networks' servers on a network
 *
 * @author Dalmau
 */
public interface INetworkServer {

    /**
     * Stop the server and close the network connexion
     */
    public void stopThread();

}
