package platform.containersregister;

import java.util.Vector;

/**
 * Class used by the supervisor in order to manage the list of BCs containers actually created.<br>
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
public class ComponentsRegistration {

    private Vector<OsagaiaContainerDescriptor> conteneurs; // Liste des conteneurs enregistres (osagaia et korrontea)
    /**
     * Indicator of a not connected input
     */
    public static final String DECONNECTE = "NC";

    /**
     * Construction of a Container list
     */
    public ComponentsRegistration() {
        conteneurs = new Vector<OsagaiaContainerDescriptor>();
    }

    /**
     * Add a container to the list
     *
     * @param descriptor the container to be added
     */
    public void ajouterConteneur(OsagaiaContainerDescriptor descriptor) {
        conteneurs.addElement(descriptor);
    }

    /**
     * Find a container in the list
     *
     * @param name name of the container to find
     * @return The container descriptor (null if no container of this name)
     */
    public OsagaiaContainerDescriptor trouverConteneur(String name) {
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
     * @param number number of input
     * @param input name of the new input of the container
     */
    public void changerEntreeConteneur(String name, int number, String input) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        OsagaiaContainerDescriptor courant = trouverConteneur(name);
        if (courant != null)  // s'il existe on modifie son entree
            courant.setEntree(number, input);
    }

    /**
     * Change the output of a container
     *
     * @param name name of the container to change
     * @param output name of the new output of the container
     */
    public void ajouterSortieConteneur(String name, String output) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        OsagaiaContainerDescriptor courant = trouverConteneur(name);
        if (courant != null)  // s'il existe on modifie son entree
            courant.addSortie(output);
    }
    
    /**
     * Update all inputs/outputs of components when a connector is removed
     *
     * @param nameConnector name of the removed connector
     */
    public void deconnecterESConteneur(String nameConnector) {
        OsagaiaContainerDescriptor courant=null;
        for (int i=0; i<conteneurs.size(); i++) {
            courant = conteneurs.elementAt(i);
            for (int j=0; j<courant.getEntree().length; j++) {
                if (courant.getEntree()[j].equals(nameConnector)) { // le connecteur enleve etait en entree de ce composant
                    courant.setEntree(j, DECONNECTE); // marquer l'entree comme deconnectee
                }
            }
            courant.removeSortie(nameConnector); // marquer la sortie comme deconnectee si le connecteur etait en sortie
        }
    }

    /**
     * Finds the component which input is connected to a given connector
     * @param nameConnector connector to find
     * @return the name of the connected component and the number of the connected input
     */
    public ConnectedInput trouveComposantEntreeSur(String nameConnector) {
        OsagaiaContainerDescriptor courant=null;
        boolean trouve = false;
        int i = 0;
        int j = 0;
        while ((i<conteneurs.size()) && (!trouve)) {
            courant = conteneurs.elementAt(i);
            j = 0;
            while ((j < courant.getEntree().length) && (!trouve)) {
                if (courant.getEntree()[j].equals(nameConnector)) trouve = true;
                else j++;
            }
            if (!trouve) i++;
        }
        if (trouve) {
            return new ConnectedInput(courant.getNom(), j);
        }
        else return null;
    }

    private int trouverRangConteneur(String name) {
        // Recherche dans la liste des conteneurs celui dont le nom est passe en parametre
        int i=0;
        boolean trouve=false;
        OsagaiaContainerDescriptor courant=null;
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
     * Returns the number of registered components
     * @return the number of registered components
     */
    public int getComponentsNumber() {
        // Renvoie le nombre de composants enregistres
        return conteneurs.size();
    }

    /**
     * Returns a registered component
     * @param index number of the required component
     * @return the required registered component
     */
    public OsagaiaContainerDescriptor getComponentAt(int index) {
        // Renvoie le indexieme composant
        return conteneurs.elementAt(index);
    }

}
