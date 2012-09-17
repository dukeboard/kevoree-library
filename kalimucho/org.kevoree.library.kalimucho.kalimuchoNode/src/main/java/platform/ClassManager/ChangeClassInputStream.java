package platform.ClassManager;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;

/**
 *
 * @author Dalmau
 */
public class ChangeClassInputStream extends ObjectInputStream {

    private ClassLoader classLoader;
    
    /**
     * A stream used to deserialize an object according to a given class loader
     * @param in the input strem on which the object is read
     * @param cl the class loader to deserialize the object
     * @throws IOException if an error occurs when deserializing the object
     */
    public ChangeClassInputStream(InputStream in, ClassLoader cl) throws IOException {
        super(in);
        classLoader = cl;
    }

    /**
     * Overrides the resolveClass method of java ObjectInputStream in order to find or download the needed classes
     * @param desc the class to resolve
     * @return Th resolved class
     * @throws IOException if an errors occurs when resolving the class
     * @throws ClassNotFoundException the class is not resident and can't be loaded from network
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (desc.getName().startsWith("[")) return Class.forName(desc.getName(), false, this.getClass().getClassLoader());
        else return classLoader.loadClass(desc.getName());
    }
}
