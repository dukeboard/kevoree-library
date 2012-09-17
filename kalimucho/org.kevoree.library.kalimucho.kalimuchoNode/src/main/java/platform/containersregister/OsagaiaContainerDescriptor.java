package platform.containersregister;

import model.interfaces.IContainer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.ClassManager.ClassLoaderFromJarFile;
import java.util.Vector;

/**
 * Stores an Osagaia container and its creation parameters (name, input, output).<br>
 * Used by the supervisor in order to manage the list of BCs and connectors
 * actually running.
 *
 * @author Dalmau
 */

// Classe utilisee pour representer les conteneurs lors de leur enregistrement par la PF
// permet d'en recuperer les informations et de les arreter
public class OsagaiaContainerDescriptor {
    private IContainer conteneur; // le conteneur
    private String nom; // le nom symbolique du conteneur
    private String[] entree; // ce qu'il a en entree
    private Vector<String> sortie; // ce qu'il a en sortie
    ClassLoaderFromJarFile classLoader;

    /**
     * Construction of a Container Descriptor
     *
     * @param ic Container
     * @param cl class loader of the BC
     * @param n name of the container
     * @param e input of the container (as in the create command)
     * @param s output of the container (as in the create command)
     */
    public OsagaiaContainerDescriptor(IContainer ic, ClassLoaderFromJarFile cl, String n, String[] e, String[] s) {
        conteneur=ic;
        classLoader = cl;
        nom=n;
        entree=e;
        sortie=new Vector<String>();
        for (int i=0; i<s.length; i++) sortie.addElement(s[i]);
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
    public String[] getEntree() { return entree; }


    /**
     * Gets the complete list of inputs
     * @return complete list of inputs
     */
    public String getEntryList() {
        String ret;
        if (entree.length > 0) {
            ret = "["+entree[0];
            for (int i=1; i<entree.length; i++)
                ret = ret.concat(" "+entree[i]);
            ret = ret.concat("]");
        }
        else ret = "null";
        return ret;
    }
    
    /**
     * Change container input
     *
     * @param number number of input
     * @param entree new input
     */
    public void setEntree(int number, String entree) { 
        this.entree[number] = entree;
    }

    /**
     * Return container output
     *
     * @return container output (as in the create command)
     */
    public String[] getSortie() { 
        String[] ret = new String[sortie.size()];
        for (int i=0; i<sortie.size(); i++) ret[i]= sortie.elementAt(i);
        return ret;
    }

    /**
     * Gets the output list
     * @return the output list
     */
    public String getOutputList() {
        String ret;
        if (sortie.size() > 0) {
            ret = "["+ sortie.elementAt(0);
            for (int i=1; i<sortie.size(); i++)
                ret = ret.concat(" "+ sortie.elementAt(i));
            ret = ret.concat("]");
        }
        else ret = "null";
        return ret;
    }

    /**
     * Add an output to the container
     *
     * @param out output to add
     */
    public void addSortie(String out) {
        sortie.addElement(out);
    }

    /**
     * Remove an output from the container
     *
     * @param out output to remove
     */
    public void removeSortie(String out) {
        boolean trouve = false;
        int i=0;
        while ((!trouve) &&(i<sortie.size())) {
            if ((sortie.elementAt(i)).equals(out)) trouve = true;
            else i++;
        }
        if (trouve) sortie.removeElementAt(i);
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
        if (classLoader instanceof ClassLoaderFromJarFile) {
            classLoader.deleteJarFile();
        }
        conteneur=null; // supprimer le conteneur
    }

}
