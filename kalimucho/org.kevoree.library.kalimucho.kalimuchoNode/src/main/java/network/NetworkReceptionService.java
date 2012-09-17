package network;

import util.NetworkAddress;
import java.util.HashMap;
import network.connectors.ConnectorReceptionThread;

/**
 * Receptor manager. Offers methods to:<br>
 * <ul>
 * <li>	Create and run a thread for connectores receptions.
 * <li>	Stop and wait a thread for connectores receptions.
 * </ul>
 *
 * @author Dalmau
 */

// Classe qui gere le recepteur reseau de l'hote
// Traite les demandes de creation/suppression de threads d'emission reseau
// deposees par la PF quand elle cree/supprime un connecteur deporte
public class NetworkReceptionService {

    private HashMap<String, ConnectorReceptionThread> listeThreads; // liste des Threads Recepteurs

    /**
     * Construction of the list of reception threads
     */
    public NetworkReceptionService() {
        listeThreads = new HashMap<String, ConnectorReceptionThread>(); // liste des threads actifs
    }

    /**
     * Create a reception thread for a connector
     * A reception thread can receive Samples or encapsulated samples according to the "type" parameter.
     * When a connector is part of a relai connector il receives encapsulated data because the normally sent class
     * of samples can be an unknown class on the relai host. So the emitter part of the connector encapsulate them.
     * In this case the reception thread will desencapsulate data as received.
     *
     * @param nom name of the connector
     * @param adr address connected to this thread
     */
    public synchronized void createConnectorReceptionThread(String nom, NetworkAddress adr) {
        // cree un thread pour gerer les receptions d'un connecteur en entree reseau
        ConnectorReceptionThread rec = new ConnectorReceptionThread(nom, adr);
        listeThreads.put(nom, rec); // ajouter le thread a la liste
        rec.start();
    }

    /**
     * Remove the reception thread of a connector
     *
     * @param nom Name of the connector associated to this thread
     */
    public synchronized void removeConnectorReceptionThread(String nom) {
        // supprime un thread qui gerait les receptions d'un connecteur en entree reseau
        ConnectorReceptionThread cherche = listeThreads.get(nom);
        if (cherche != null) {
            cherche.stopThread(); //arrete le thread
            cherche.waitUntilStopped(); //waitUntilStopped que le thread se termine
            listeThreads.remove(nom); // supprimer le thread de la liste
        }
    }

    /**
     * Returns the actual total number of bytes received on network by the connectors since last measure
     * @return the actual total number of bytes received on network  by the connectors since last measure
     */
    public synchronized int getNetworkTraffic() {
        int taille = 0;
        for (String cle : listeThreads.keySet()) {
            ConnectorReceptionThread conn = listeThreads.get(cle);
            int tailleConn = conn.getRecepteur().getDataSize();
            conn.calculeDebitMoyen(tailleConn);
            taille = taille+tailleConn;
        }
        return taille;
    }

}
