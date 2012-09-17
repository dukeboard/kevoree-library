package model.osagaia;

import util.Commands;
import platform.servicesregister.ServicesRegisterManager;

/**
 * Thread which connects the Input Unit to a connector<br>
 * Waits util the connector is available
 *
 * @author Dalmau
 */
public class InputUnitConnectionAfterRedirectionThread extends Thread {

    private InputUnit ue;
    private String connecteur;
    private String[] nomconnecteurEntree;
    private String[] nomconnecteurSortie;

    /**
     * Thread construction
     *
     * @param u Input Unit to connect
     * @param nom name of the connector to connect with
     * @param inputConnectors connectors used as inputs
     * @param outputConnectors connectors used as outputs
     */
    public InputUnitConnectionAfterRedirectionThread(InputUnit u, String nom, String[] inputConnectors, String[] outputConnectors) {
        ue = u;
        connecteur = nom;
        nomconnecteurEntree = inputConnectors;
        nomconnecteurSortie = outputConnectors;
    }

    /**
     * Wait until the connector is available
     */
    @Override
    public void run() {
        for (int i=0; i<nomconnecteurSortie.length; i++) { // attente des connecteurs en sortie
            if ((!nomconnecteurSortie[i].equals(Commands.ES_NOT_USED)) && (!nomconnecteurSortie[i].equals(Commands.ES_NULL))) {
                // attente du service de l'UE du connecteur en sortie
                ServicesRegisterManager.platformWaitForService(nomconnecteurSortie[i]);
            }
        }
        for (int i=0; i<nomconnecteurEntree.length; i++) { // attente des connecteurs en entree
            if ((!nomconnecteurEntree[i].equals(Commands.ES_NOT_USED)) && (!nomconnecteurEntree[i].equals(Commands.ES_NULL))) {
                // attente du service de l'US du connecteur en entree
                ServicesRegisterManager.platformWaitForService(nomconnecteurEntree[i]);
            }
        }
        ue.connection(connecteur);
    }

}
