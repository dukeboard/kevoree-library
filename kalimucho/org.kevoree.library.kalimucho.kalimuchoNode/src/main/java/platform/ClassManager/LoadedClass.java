package platform.ClassManager;

/**
 * Describes a class loaded by the platform:<br>
 * The Class object and the associated class loader (from a jar file).
 * @author Dalmau
 */
public class LoadedClass {

    private Class<?> classe;
    private ClassLoaderFromJarFile chargeur;

    /**
     * Create a descriptor of a loaded class = class + class loader
     * @param c the class
     * @param clfbc the class loader
     */
    public LoadedClass(Class<?> c, ClassLoaderFromJarFile clfbc) {
        classe = c;
        chargeur = clfbc;
    }

    /**
     * Returns the class in the descriptor
     * @return the class in the descriptor
     */
    public Class<?> getClasse() { return classe; }
    /**
     * Returns the class loader in the descriptor
     * @return the class loader in the descriptor
     */
    public ClassLoaderFromJarFile getChargeur() { return chargeur; }

}
