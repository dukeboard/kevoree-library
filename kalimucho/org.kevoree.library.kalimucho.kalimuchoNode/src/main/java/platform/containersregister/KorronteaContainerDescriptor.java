package platform.containersregister;

import model.interfaces.IContainer;
import model.korrontea.ConnectorContainer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.Commands;

/**
 * Stores a Korrontea container and its creation parameters (name, input, output).<br>
 * Used by the supervisor in order to manage the list of BCs and connectors
 * actually running.
 *
 * @author Dalmau
 */

// Classe utilisee pour representer les conteneurs lors de leur enregistrement par la PF
// permet d'en recuperer les informations et de les arreter
public class KorronteaContainerDescriptor {
    private ConnectorContainer conteneur; // le conteneur
    private String nom; // le nom symbolique du conteneur
    private String entree, sortie; // ce qu'il a en entree et en sortie

    /**
     * Construction of a Container Descriptor
     *
     * @param i Container
     * @param n name of the container
     * @param e input of the container (as in the create command)
     * @param s output of the container (as in the create command)
     */
    public KorronteaContainerDescriptor(ConnectorContainer i, String n, String e, String s) {
        conteneur=i;
        nom=n;
        entree=e;
        sortie=s;
    }

    // Methodes de recuperation des informations du conteneur
    /**
     * Returns container name
     *
     * @return container name
     */
    public String getNom() { return nom; }

    /**
     * Return the container
     *
     * @return The container
     */
    public IContainer getContainer() { return conteneur; }

    /**
     * Return container input
     *
     * @return container input (as in the create command)
     */
    public String getEntree() { return entree; }

    /**
     * Change container input
     *
     * @param entree new input
     */
    public void setEntree(String entree) { 
        this.entree = entree;
    }

    /**
     * Return container output
     *
     * @return container output (as in the create command)
     */
    public String getSortie() { return sortie; }

    /**
     * Change container output
     *
     * @param sortie new output
     */
    public void setSortie(String sortie) { 
        this.sortie = sortie;
    }

    /**
     * Returns true if this connector has its input comming from network
     * @return true if this connector has its input comming from network
     */
    public boolean isInputNetwork() {
        return !((entree.equals(Commands.ES_INTERNE) || entree.equals(Commands.ES_NOT_USED)|| entree.equals(Commands.ES_NULL)));
    }

    /**
     *  Returns true if this connector has its output going to network
     * @return  true if this connector has its output going to network
     */
    public boolean isOutputNetwork() {
        return !((sortie.equals(Commands.ES_INTERNE) || sortie.equals(Commands.ES_NOT_USED)|| sortie.equals(Commands.ES_NULL)));
    }

    // Methode permettant de supprimer ce conteneur
    /**
     * Stop a container<br>
     * The service provided by this container in unregistered
     * 
     */
    public void stopConteneur() {
        try { // desenregistrer le service de l'UC du conteneur
            ServicesRegisterManager.removeService(nom);
        }
        catch (ServiceClosedException sce) {
            System.err.println("Container of BC : "+nom+" allready removed");
        }
        conteneur=null; // supprimer le conteneur
    }

}
