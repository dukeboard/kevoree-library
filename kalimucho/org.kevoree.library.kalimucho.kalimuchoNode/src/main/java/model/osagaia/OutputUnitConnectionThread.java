package model.osagaia;

/**
 * Thread which connects the Output Unit to a connector<br>
 * Waits util the connector is available
 *
 * @author Dalmau
 */
public class OutputUnitConnectionThread extends Thread {

    private OutputUnit us;
    private String connecteur;

    /**
     * Thread construction
     *
     * @param u Output Unit to connect
     * @param nom name of the connector to connect with
     */
    public OutputUnitConnectionThread(OutputUnit u, String nom) {
        us = u;
        connecteur = nom;
    }

    /**
     * Wait until the connector is available
     */
    @Override
    public void run() {
        us.connection(connecteur);
    }

}
