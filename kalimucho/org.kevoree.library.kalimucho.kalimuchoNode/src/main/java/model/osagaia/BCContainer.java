package model.osagaia;

/**
 *
 * @author Dalmau
 */

import util.Commands;
import platform.ClassManager.KalimuchoClassLoader;
import platform.ClassManager.ClassLoaderFromJarFile;
import platform.ClassManager.LoadedClass;
import model.interfaces.IContainer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;

// Classe du conteneur de CM (Osagaia)
/**
 * BCContainer is created with its name, the name of the BC's class,
 * the name of its input connector and the name of its output connector
 * ("null" when no connector).<br>
 * The container creates an instance of the BC, creates the input units,
 * the output unit and the control unit of an Osagaia container.<br>
 * It is a thread waiting for inputs and output connections of the BC,
 * then it runs the BC and register the control unit as a service for the platform.
 *
 * @author Dalmau
 */
public class BCContainer implements IContainer, Runnable {

    private String monNom; // nom symbolique du composant
    private String classeCM; // classe du CM
    private String[] connecteurSortie; // noms symboliques des connecteurs en sortie
    private String[] connecteurEntree; // noms symboliques des connecteurs en entree
    private InputUnit[] ue;
    private OutputUnit us;
    private ControlUnit uc;
    private BCModel cm;
    private ClassLoaderFromJarFile classLoader;

    /**
     * Construction of a BC container whith the class name of the BC
     *
     * @param name symbolic name of the BC
     * @param classOfCM class of the BC
     * @param inputConnectors names of input connectors
     * @param outputConnector names of output connectors
     */
    public BCContainer(String name, String classOfCM, String[] inputConnectors, String[] outputConnectors) {
        monNom = name;
        classeCM = classOfCM;
        connecteurEntree = inputConnectors;
        connecteurSortie = outputConnectors;
        // Creer les UE
        ue=new InputUnit[connecteurEntree.length];
        for (int i=0; i<connecteurEntree.length; i++) {
            if (!connecteurEntree[i].equals(Commands.ES_NULL)) ue[i]=new InputUnit(i);
            else ue[i]=null;
        }
        // creer l'US
        us=null;
        int i=0;
        boolean usCree = false;
        while ((!usCree) && (i<connecteurSortie.length)) {
            if (!connecteurSortie[i].equals(Commands.ES_NULL)) {
                us=new OutputUnit();
                usCree = true;
            }
            else i++;
        }
        uc=null;
        try {
            // creation d'une instance de CM d'apres le nom de la classe
            LoadedClass lc = KalimuchoClassLoader.loadOrCreateClass(classeCM);
            classLoader = lc.getChargeur();
            if (classLoader instanceof ClassLoaderFromJarFile) classLoader.addLink();
            Class<?> tmpClass = lc.getClasse();
            cm = (BCModel)tmpClass.newInstance();
            cm.setName(monNom);
            cm.setContainer(this);
            cm.setInputOutputUnits(ue, us); // lier le CM e son UE et son US
            // creer l'UC
            uc=new ControlUnit(connecteurEntree, connecteurSortie, ue, cm, us);
            cm.setControlUnit(uc); // lier le CM e son UC
        }
        catch (ClassNotFoundException cnfe) {
            System.err.println("Osagaia Container "+monNom+" : BC class unknown");
        }
        catch (InstantiationException cnfe) {
            System.err.println("Osagaia Container "+monNom+" : BC class can't be instantiated");
        }
        catch (IllegalAccessException cnfe) {
            System.err.println("Osagaia Container "+monNom+" : Illegal access to the BC class");
        }
        // le reste de la construction du conteneur est fait dans un thread car
        // elle attend que les connecteurs d'entree et de sortie soient crees
        new Thread(this).start();
    }

    /**
     * Construction of a BC container with a serialized BC
     *
     * @param name symbolic name of the BC
     * @param CM serialized BC
     * @param inputConnectors names of input connectors
     * @param outputConnector names of output connectors
     */
    public BCContainer(String name, BCModel CM, ClassLoaderFromJarFile cl, String[] inputConnectors, String[] outputConnectors) {
        monNom = name;
        connecteurEntree = inputConnectors;
        connecteurSortie = outputConnectors;
        // Creer les UE
        ue=new InputUnit[connecteurEntree.length];
        for (int i=0; i<connecteurEntree.length; i++) {
            if (!connecteurEntree[i].equals(Commands.ES_NULL)) ue[i]=new InputUnit(i);
            else ue[i]=null;
        }
        // creer l'US
        us=null;
        int i=0;
        boolean usCree = false;
        while ((!usCree) && (i<connecteurSortie.length)) {
            if (!connecteurSortie[i].equals(Commands.ES_NULL)) {
                us=new OutputUnit();
                usCree = true;
            }
            else i++;
        }
        uc=null;
        cm = CM;
        classLoader = cl;
        classeCM = cm.getClass().getName();
        cm.setName(monNom);
        cm.setContainer(this);
        cm.setInputOutputUnits(ue, us); // lier le CM e son UE et son US
        // creer l'UC
        uc=new ControlUnit(connecteurEntree, connecteurSortie, ue, cm, us);
        cm.setControlUnit(uc); // lier le CM e son UC
        cm.associateFilters();
        // le reste de la construction du conteneur est fait dans un thread car
        // elle attend que les connecteurs d'entree et de sortie soient crees
        new Thread(this).start();
    }

    /**
     * Returns the BC included in this container
     * @return BC included in this container
     */
    public BCModel getBC() {
        return cm;
    }

    /**
     * Returns the class loader of the BC included in the container
     * @return the class loader of the BC included in the container
     */
    public ClassLoaderFromJarFile getClassLoader() { return classLoader; }

    /**
     * Waits for inputs and output connections of the BC,
     * then runs the BC (start_CM method).
     *
     */
    public void run() {
        // enregistrement du service de l'UC pour la PF
        try {
            ServicesRegisterManager.registerService(monNom, uc);
        }
        catch(ServiceInUseException siue) {
            System.err.println("Osagaia Container "+monNom+" : created twice");
        }
        uc.connection(); // connecter ce composant aux connecteurs (attente de creation de ces connecteurs)
        uc.start_CM(); // lancer le CM (thread)
    }

    /**
     * Flags the BC as serialised (after a migration)
     */
    public void setSerialized() {
        cm.setSerialized();
    }
}
