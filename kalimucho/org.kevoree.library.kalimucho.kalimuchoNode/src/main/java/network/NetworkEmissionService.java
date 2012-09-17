package network;

import util.NetworkAddress;
import java.util.HashMap;
import network.connectors.ConnectorEmissionThread;

/**
 * Emitter manager. Offers methods to:<br>
 * <ul>
 * <li>	Create and run a thread for connectores emissions.
 * <li>	Stop and wait  a thread for connectores emissions.
 * </ul>
 *
 * @author Dalmau
 */

// Classe qui gere l'emetteur reseau de l'hote
// Traite les demandes de creation/suppression de threads
// d'emission par reseau deposees par la PF quand elle cree un connecteur deporte

public class NetworkEmissionService {

    private HashMap<String, ConnectorEmissionThread> listeThreads; // liste des threads associes a chaque connecteur qui emet

    /**
     * Construction of a network emitter
     *
     */
    public NetworkEmissionService() {
        listeThreads = new HashMap<String, ConnectorEmissionThread>(); // liste des noms des thread actifs (nom du connecteur)
    }


    /**
     * Create an emission thread for a connector.
     * An emission thread can send Samples or encapsulated samples according to the "type" parameter.
     * When a connector is part of a relai connector il sends encapsulated data because the normally sent class
     * of samples can be an unknown class on the relai host.
     *
     * @param nom name of the connector
     * @param adr a to emit messages
     *
     */
    public void createConnectorEmissionThread(String nom, NetworkAddress adr) {
        // cree un thread pour gerer les emission d'un connecteur en sortie reseau
        ConnectorEmissionThread empf = new ConnectorEmissionThread(nom, adr);
        listeThreads.put(nom, empf); // ajouter le thread a la liste
        empf.start(); // le lancer
    }

    /**
     * Remove an emission thread for a connector
     *
     * @param nom name of the connector
     */
    public void removeConnectorEmissionThread(String nom) {
        // Arreter un thread qui gerait les emission d'un connecteur en sortie reseau
        // et attendre qu'il se termine
        ConnectorEmissionThread cherche = listeThreads.get(nom);
        if (cherche != null) {
            cherche.stopThread(); //arreter le thread
            cherche.waitUntilStopped(); //attendre qu'il se termine
            listeThreads.remove(nom); // supprimer le thread de la liste
        }
    }

    /**
     * Returns the actual total number of bytes sended on network by the connectors since last measure
     * @return the actual total number of bytes sended on network  by the connectors since last measure
     */
    public int getNetworkTraffic() {
        int taille = 0;
        for (String cle : listeThreads.keySet()) {
            ConnectorEmissionThread conn = listeThreads.get(cle);
            int tailleConn = conn.getEmetteur().getDataSize();
            conn.calculeDebitMoyen(tailleConn);
            taille = taille+tailleConn;
        }
        return taille;
    }

}
