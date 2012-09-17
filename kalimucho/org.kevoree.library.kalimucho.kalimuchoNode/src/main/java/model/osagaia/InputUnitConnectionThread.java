package model.osagaia;

/**
 * Thread which connects the Input Unit to a connector<br>
 * Waits util the connector is available
 *
 * @author Dalmau
 */
public class InputUnitConnectionThread extends Thread {

    private InputUnit ue;
    private String connecteur;

    /**
     * Thread construction
     *
     * @param u Input Unit to connect
     * @param nom name of the connector to connect with
     */
    public InputUnitConnectionThread(InputUnit u, String nom) {
        ue = u;
        connecteur = nom;
    }

    /**
     * Wait until the connector is available
     */
    @Override
    public void run() {
        ue.connection(connecteur);
    }

}
