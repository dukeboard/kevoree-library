package platform.plugins.installables.jarRepository;

import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import model.interfaces.platform.IPlatformPlugin;
import java.io.File;
import util.Parameters;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.HashMap;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import platform.ClassManager.KalimuchoClassLoader;
/**
 * Plugin that looks in the repository for a given class
 * These classes are are defined in the jar file by:
 * BC: for the class of the BC
 * Samples: for the classes of samples (names of classes separated by comas)
 *
 * @author Dalmau
 */

public class JarRepositoryManager implements IPlatformPlugin {

    private HashMap<String, String>[] classesDisponibles;
    private final static String[] types = {"PC" , "Android"};

    /**
     * Starts the plugin = register this plugin as a service.
     */
    public void startPlugin() {
        classesDisponibles = new HashMap[types.length];
        try { // enregistrer le service d'acces aux depot de jars
            ServicesRegisterManager.registerService(Parameters.JAR_REPOSITORY_MANAGER, this);
            rescanRepository(); // creer les listes de classes disponibles a partir du depot
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Jar Repository Manager service created twice");
        }
    }

    /**
     * Stops the plugin = unregister this plugin as a service.
     */
    public void stopPlugin() {
        try {
            ServicesRegisterManager.removeService(Parameters.JAR_REPOSITORY_MANAGER);
        }
        catch (ServiceClosedException sce) {}
    }

    // Recherche dans le depot le jar contenant la classe demandee pour le type d'hote indique
    // Cette classe doit etre indiquee dans le manifest par :
    // BC: classe_du_CM  pour un composant metier
    // Samples: classe_d_echantillon1, classe_d_echantillon2 ...   pour les echantillons
    /**
     * Find a class in the repository.<br>
     * This class is identified in the jar file by:<br>
     * BC: for the class of the BC<br>
     * Sample: for the classes of samples (classes' names separated by comas)<br>
     * @param type Type of host the class is for (Android, CD, PC)
     * @param name Name of class to find
     * @return The class byte code in a byte array
     * @throws ClassNotFoundException When the class can't be found
     */
     public byte[] findByteCodeForClass(String type, String name) throws ClassNotFoundException {
        File resultat = findJarFileForClass(type, name);
        try { // recuperer son contenu sous forme de tableau d'octets
            ByteArrayOutputStream buffer = null;
            FileInputStream lireOctets = new FileInputStream(resultat);
            buffer = new ByteArrayOutputStream();
            int cpt;
            byte[] lu = new byte[1024]; // lecture par blocs de 1K => plus rapide
            do {
                cpt = lireOctets.read(lu);
                if (cpt > 0) buffer.write(lu);
            }
            while (cpt == 1024 );
            return buffer.toByteArray(); // revoyer la tableau d'octets
        }
        catch (IOException ioe) {
            System.err.println("Can't open jar file "+resultat.getName());
            throw new ClassNotFoundException();
        }
     }

    /**
     * Find a jar file in the repository containing a specified class.<br>
     * This class is identified in the jar file by:<br>
     * BC: for the class of the BC<br>
     * Samples: for the classes of samples (classes' names separated by comas)<br>
     * @param type Type of host the class is for (Android, CD, PC)
     * @param name Name of class to find
     * @return The jar file in which this class is
     * @throws ClassNotFoundException When the class can't be found
     */
     public File findJarFileForClass(String type, String name) throws ClassNotFoundException {
         // On cherche la classe demandee parmi celles repertoriees lors du lancement de Kalimucho
         // Si on ne la trouve pas on recree la liste (cas ou le fichier jar aurait ete ajoute apres le demarrage)
         // Si on la trouve on revoie le fichier jar la contenant sinon on leve une exception
         int index=0;
         boolean trouve = false;
         while ((index<types.length) && (!trouve)) { // recherche du type
             if (type.equals(types[index])) trouve = true;
             else index++;
         }
         if (!trouve) throw new ClassNotFoundException(); // type introuvable
         else { // le type est connu on cherche la classe
             int essais = 0;
             while (essais != 2) {
                 String fich = classesDisponibles[index].get(name);
                 if (fich != null) { // classe repertoriee dans la liste
                     essais = 2; // on renvoie le fichier
                     return new File(Parameters.COMPONENTS_REPOSITORY+"/"+type+"/"+fich);
                 }
                 else { // la classe n'est pas dans la liste
                     essais++;
                     if (essais == 1) { // si on ne l'a pas deja fait on recree la liste a partir du depot
                         rescanRepository();
                     }
                 }
             }
             throw new ClassNotFoundException(); // Classe introuvable meme apres avoir recree la liste
         }
     }
     
     /**
      * Creates the list of available classes in repository
      */
     public void rescanRepository() { // recree les liste de classes connues a partir du depot
        // cette methode peut etre invoquee si on sait que le depot a ete modifie
        for (int i=0; i<types.length; i++) {
            classesDisponibles[i] = new HashMap<String, String>();
             scanRepository(types[i], classesDisponibles[i]);
        }
     }

     private void scanRepository(String type, HashMap<String, String> liste) {
         // Chemin du depot defini dans parameters et complete par le type d'hote
         String chemin = new String(Parameters.COMPONENTS_REPOSITORY+"/"+type);
//System.out.println("explore : "+chemin);
         File depot = new File(chemin);
         File[] fichiers = depot.listFiles(); // liste des fichiers contenus dans ce repertoire
         for (int i = 0; i<fichiers.length; i++) { // explorer ces fichiers
            if (fichiers[i].isFile()) { // c'est un fichier
                if (fichiers[i].getName().endsWith(".jar")) { // c'est un fichier .jar
                    try {
                        JarFile accesJar = new JarFile(fichiers[i]);
                        Manifest manifest = accesJar.getManifest(); // recuperer le manifest de ce fichier
                        // Recuperer le nom de la classe du composant metier (dans ce manifest)
                        String classeCM = manifest.getMainAttributes().getValue(KalimuchoClassLoader.BC_CLASS);
                        liste.put(classeCM, fichiers[i].getName());
//System.out.println("ajoute : ("+classeCM+" , "+fichiers[i].getName()+")");
                    }
                    catch (IOException ioe) {
                        System.err.println("Can't access to jar file "+fichiers[i].getName()+" in "+chemin);
                    }
                }
            }
        }
     }

}
