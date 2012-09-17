package platform.ClassManager;

import java.util.Vector;
import java.io.File;
import util.Parameters;
import platform.servicesregister.ServiceClosedException;
import platform.servicesregister.ServicesRegisterManager;
import platform.plugins.installables.jarRepository.JarRepositoryManager;

/**
 * Manages the classLoaders used by Kalimucho.<br>
 * Allows to register/unregister classLoaders associated to jar files<br>
 * The method loadClass loads a class accessible by the classLoader of the PF
 *   or by one of the registered classLoaders associated to jar files.<br>
 * The method loadOrCreateClass acts as the java loadClass method but if the class cant be found
 *   it goes into the components repository in order to find it.<br>
 *
 * @author Dalmau
 */
public class KalimuchoClassLoader {

    /**
     * Type of the host (PC/CDC/Android) used to find jar file in repository
     */
    public static final String MON_TYPE = "PC";
    /**
     * Key used in manifests to indicate BCs' classes
     */
    public static String BC_CLASS = "BC"; // cle utilisee dans les manifest pour designer les classes de CM
    /**
     * Separator when there is more than one class associated to a key in the manifest
     */
    public static String CLASS_SEPARATOR = " "; // separateur quand il y a plusieurs classes associees a une meme cle

    // liste des classLoaders enregistres
    private static Vector<ClassLoaderFromJarFile> registeredClassLoaders = new Vector<ClassLoaderFromJarFile>();

    /**
     * Registers a new class loader
     * @param classLoader the class loader to register
     */
    public static void addClassLoader(ClassLoaderFromJarFile classLoader) { // ajoute un classLoader a la liste
        registeredClassLoaders.addElement(classLoader);
        System.out.println("Adding jar file: "+classLoader.getJarFileName());
    }

    /**
     * Unregisters a registered class loader
     * @param classLoader the class loader to unregister
     */
    public static void removeClassLoader(ClassLoaderFromJarFile classLoader) { // enleve un classLoader de la liste
        int i = 0; boolean trouve = false;
        while ((i < registeredClassLoaders.size()) && (!trouve)) {
            if (registeredClassLoaders.elementAt(i) == classLoader) trouve = true;
            else i++;
        }
        if (trouve) {
            System.out.println("Removing jar file: "+classLoader.getJarFileName());
            registeredClassLoaders.removeElementAt(i);
        }
    }

    // Charge une classe accessible par le classLoader de base de la PF
    //  ou l'un des classLoaders sur fichier jar enregistres
    /**
     * Loads a class from the standard class loader or from a registered one
     * @param name name of the class to load
     * @return the loaded class
     * @throws ClassNotFoundException if the class is neither loadable by the standard or a registered class loader
     */
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        // Il y a un bug pour le cas particuler des tableaux que le classLoader de base
        // de java ne trouve pas => on traite ce cas a part
        if (name.startsWith("[")) return Class.forName(name, false, KalimuchoClassLoader.class.getClassLoader());
        Class<?> reponse = null;
        try { // essayer d'abord de trouver la classe avec le classLoader de base de la PF
            reponse = KalimuchoClassLoader.class.getClassLoader().loadClass(name);
            return reponse; // si le classLoader de base de la PF l'a trouvee on le renvoie
        }
        catch (ClassNotFoundException cnfe) { // le classLoader de base de la PF ne trouve pas le classe demandee
            int i = 0; boolean trouve = false;
            while ((i < registeredClassLoaders.size()) && (!trouve)) { // essayer les classLoaders enregistres
                try {
                    reponse = registeredClassLoaders.elementAt(i).loadClass(name);
                    trouve = true; // on a trouve la classe
                }
                catch (ClassNotFoundException cnfe2) {
                    i++; // on n'a pas trouve la classe => essayer un autre classLoader enregistre
                }
            }
            if (trouve) {
                return reponse; // renvoyer la classe trouve par un classLoader enregistre
            }
            else {
                throw new ClassNotFoundException(); // classe introuvable
            }
        }
    }

    private static LoadedClass findLoadedClass(String name) throws ClassNotFoundException {
        // Il y a un bug pour le cas particuler des tableaux que le classLoader de base
        // de java ne trouve pas => on traite ce cas a part
        if (name.startsWith("[")) {
            return new LoadedClass(Class.forName(name, false, KalimuchoClassLoader.class.getClassLoader()), null);
        }
        Class<?> reponse = null;
        try { // essayer d'abord de trouver la classe avec le classLoader de base de la PF
            reponse = KalimuchoClassLoader.class.getClassLoader().loadClass(name);
            return new LoadedClass(reponse, null); // si le classLoader de base de la PF l'a trouvee on le renvoie
        }
        catch (ClassNotFoundException cnfe) { // le classLoader de base de la PF ne trouve pas le classe demandee
            int i = 0; boolean trouve = false;
            while ((i < registeredClassLoaders.size()) && (!trouve)) { // essayer les classLoaders enregistres
                try {
                    reponse = registeredClassLoaders.elementAt(i).loadClass(name);
                    trouve = true; // on a trouve la classe
                }
                catch (ClassNotFoundException cnfe2) {
                    i++; // on n'a pas trouve la classe => essayer un autre classLoader enregistre
                }
            }
            if (trouve) {
                return new LoadedClass(reponse,registeredClassLoaders.elementAt(i)); // renvoyer la classe trouve par un classLoader enregistre
            }
            else {
                throw new ClassNotFoundException(); // classe introuvable
            }
        }
    }

    // Essaye de trouver la classe demandee par loadClass mais si la classe n'est pas trouvable
    //  Lancer une recherche dans le depot de composants pour la recuperer
    /**
     * Tries to load a class from the standard class loader or from a registered one.
     * If the class can't be found, broascast a request on network to download it
     * @param name name of the class to load
     * @return the loaded class
     * @throws ClassNotFoundException if the class can't be found
     */
    public static LoadedClass loadOrCreateClass(String name) throws ClassNotFoundException {
        LoadedClass reponse = null;
        try { // essayer de trouver la classe demandee par loadClass
            reponse = findLoadedClass(name);
            return reponse; // on l'a trouvee car la classe demandee est deja connue
        }
        catch (ClassNotFoundException cnfe) { // La classe demandee n'est pas connue
            try { // on va essayer de la trouver dans le depot
                JarRepositoryManager depot = (JarRepositoryManager)ServicesRegisterManager.lookForService(Parameters.JAR_REPOSITORY_MANAGER);
                File resultat = depot.findJarFileForClass("PC", name); // on l'a trouvee
                ClassLoaderFromJarFile cl = new ClassLoaderFromJarFile(KalimuchoClassLoader.class.getClassLoader(), resultat.getAbsolutePath());
                addClassLoader(cl); // memoriser le class loader associe
                return new LoadedClass(cl.loadClass(name), cl); // renvoyer la classe trouvee et le classLoader associe
            }
            catch (ServiceClosedException sce) { // le depot n'est pas actif = la classe est introuvable
                throw new ClassNotFoundException();
            }
        }
    }

}
