package network;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import platform.ClassManager.KalimuchoClassLoader;
import platform.ClassManager.LoadedClass;

/**
 * ObjectInputStream that can deserialize objects of standard classes
 * or downloaded classes and associated to a specific jar file.<br>
 * If this stream receives an object of a locally unknown class, it tries to download the byte code from network.
 * 
 * @author Dalmau
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

    private LoadedClass utilisee;

    /**
     * Create an ObjectInputStream for reading objects of standard classes or loaded classes
     * @param in InputStream from which the ClassLoaderObjectInputStream is created
     * @throws IOException the ClassLoaderObjectInputStream can't be created
     */
    public ClassLoaderObjectInputStream(InputStream in) throws IOException {
        super(in);
        utilisee = null;
    }

    /**
     * Returns the last loaded class for this stream
     * @return the last loaded class for this stream
     */
    public LoadedClass getLoadedClass() { return utilisee; }

    /**
     * Overrides the resolve class of ObjectInputStream
     * Looks for a standard class or a loaded one
     * @param desc descriptor of the class to load
     * @return the class
     * @throws ClassNotFoundException the class corresponding to the descriptor is not available
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
        LoadedClass rep = KalimuchoClassLoader.loadOrCreateClass(desc.getName());
        if (rep.getChargeur() != null) utilisee = rep;
        return rep.getClasse();
    }
}
