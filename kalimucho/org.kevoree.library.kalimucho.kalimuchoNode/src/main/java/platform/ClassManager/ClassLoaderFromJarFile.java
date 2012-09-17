package platform.ClassManager;

/**
 * ClassLoader that can load classes from a jar file
 * Allows to find a class (by its name) in a jar file
 *
 * @author Dalmau
 */

public class ClassLoaderFromJarFile extends ClassLoader {

    private String jarFileName; // fichier jar utilise
    private int liens; // nombre de CM qui utilisent ce fichier (permet de savoir quand on n'en a plus besoin)

    /**
     * Cretes a class loader that finds the classes into a jar file
     * @param parent the parent class loader
     * @param jarName the name of the jar file
     */
    public ClassLoaderFromJarFile(ClassLoader parent, String jarName) {
    	super(parent);
        liens = 0;
        jarFileName = jarName;
    }

    /**
     * In order to remove the jar file when not in use, each BC using the file
     * is added as linked. When no BC is linked the jar file can be destroyed
     */
    public synchronized void addLink() {
System.out.println("ajout de lien sur "+jarFileName);
        liens++;
    } // un CM supplementaire utilise ce fichier jar

    /**
     * Returns the jar file name used by this class loader
     * @return the jar file name used by this class loader
     */
    public String getJarFileName() { return jarFileName; }

    /**
     * When no BC is linked the jar file can be destroyed
     */
    public synchronized void deleteJarFile() {
        liens--; // diminuer le nombre de CM utilisant ce fichier jar
        if (liens <= 0) { // s'il n'en reste plus on peut le supprimer de la liste
            KalimuchoClassLoader.removeClassLoader(this);
        }
    }

    /**
     * Overrides the load class method in order to load a class from a jar file
     * @param name name of the class to load
     * @return the loaded class
     * @throws ClassNotFoundException if the class not exist in the jar file
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // surcharge de le methode load class pour trouver les classes avec ce ClassLoader
        try { // essayer de trouver la classe avec le classLoader de base de la PF
            Class<?> recu = super.loadClass(name); // renvoyer le Class correspondant
            return recu;
        }
        catch (ClassNotFoundException cnfe) { // le classLoader de base de la PF ne trouve pas la classe
            byte[] code = null;
            JarRessources jarRessources = new JarRessources (jarFileName); // chercher dans le fichier jar
            code = jarRessources.getRessource("classes/"+name.replace('.', '/')+".class");
            if (code != null) { // la classe demandee est dans le jar
                Class<?> recu = defineClass(name, code, 0, code.length);
                return recu; // renvoyer le Class correspondant
            }
            else { // la classe demandee n'est pas dans le jar
                throw new ClassNotFoundException();
            }
        }
    }

}
