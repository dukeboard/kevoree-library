package platform.containersregister;

import java.util.Vector;

/**
 * Class used by the supervisor in order to manage the list of connectors containers actually created.<br>
 * Offers methods to add, find and remove containers.
 * @author Dalmau
 */

// Classe utilisee par la PF pour enregistrer les conteneurs crees
// offre des methodes pour :
// enregistrer un conteneur (nom, entree, sortie)
// rechercher dans la liste un conteneur designe par son nom
// supprimer de la liste un conteneur designe par son nom
// changer l'entree d'un conteneur de la liste (deconnexion/reconnexion)
// changer la sortie d'un conteneur de la liste (deconnexion/reconnexion)
public class ConnectorsRegistration {

    private Vector<KorronteaContainerDescriptor> conteneurs; // Liste des conteneurs enregistres (osagaia et korrontea)

    /**
     * Construction of a Container list
     */
    public ConnectorsRegistration() {
        conteneurs = new Vector<KorronteaContainerDescriptor>();
    }

    /**
     * Add a container to the list
     *
     * @param descriptor the container to be added
     */
    public void ajouterConteneur(KorronteaContainerDescriptor descriptor) {
        conteneurs.addElement(descriptor);
    }

    /**
     * Find a container in the list
     *
     * @param name name of the container to find
     * @return The container descriptor (null if no container of this name)
     */
    public KorronteaContainerDescriptor trouverConteneur(String name) {
        // Recherche dans la liste le conteneur dont le nom est passe en parametre
        int rang = trouverRangConteneur(name);
        if (rang != -1)  // s'il existe on retourne son descripteur
            return conteneurs.elementAt(rang);
        else return null; // sinon on retourne null
    }

    /**
     * Remove a container from the list
     *
     * @param name name of the container to remove
     */
    public void enleverConteneur(String name) {
        // Enleve de la liste des conteneurs celui dont le nom est passe en parametre
        int rang = trouverRangConteneur(name);
        if (rang != -1)  // s'il existe on l'enleve
            conteneurs.removeElementAt(rang);
    }

    /**
     * Change the input of a container
     *
     * @param name name of the container to change
     * @param input name of the new input of the container
     */
    public void changerEntreeConteneur(String name, String input) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        KorronteaContainerDescriptor courant = trouverConteneur(name);
        if (courant != null)  // s'il existe on modifie son entree
            courant.setEntree(input);
    }

    /**
     * Change the output of a container
     *
     * @param name name of the container to change
     * @param output name of the new output of the container
     */
    public void changerSortieConteneur(String name, String output) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        KorronteaContainerDescriptor courant = trouverConteneur(name);
        if (courant != null)  // s'il existe on modifie son entree
            courant.setSortie(output);
    }

    private int trouverRangConteneur(String name) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        int i=0;
        boolean trouve=false;
        KorronteaContainerDescriptor courant=null;
        while ((!trouve) && (i<conteneurs.size())) {
            courant = conteneurs.elementAt(i);
            if (courant.getNom().equals(name)) {
                trouve=true;
            }
            else i++;
        }
        if (trouve) return i; // s'il existe on retourne son rang
        else return -1; // sinon on retourne -1
    }

    /**
     * Returns the number of registered connectors
     * @return the number of registered connectors
     */
    public int getConnectorsNumber() {
        // Renvoie le nombre de connecteurs enregistres
        return conteneurs.size();
    }

    /**
     * Returns a registered connector
     * @param index number of the required connector
     * @return the required registered connector     */
    public KorronteaContainerDescriptor getConnectorAt(int index) {
        // Renvoie le indexieme connecteur
        return conteneurs.elementAt(index);
    }

    /**
     * Returns the number of connector which have their input comming from network
     * @return the number of connector which have their input comming from network
     */
    public int getNetworkInputConnectorsNumber() {
        int nombre = 0;
        for (int i=0; i<conteneurs.size(); i++) {
            if (conteneurs.elementAt(i).isInputNetwork()) nombre++;
        }
        return nombre;
    }

    /**
     * Returns the number of connector which have their output going to network
     * @return the number of connector which have their output going to network
     */
    public int getNetworkOutputConnectorsNumber() {
        int nombre = 0;
        for (int i=0; i<conteneurs.size(); i++) {
            if (conteneurs.elementAt(i).isOutputNetwork()) nombre++;
        }
        return nombre;
    }

}
