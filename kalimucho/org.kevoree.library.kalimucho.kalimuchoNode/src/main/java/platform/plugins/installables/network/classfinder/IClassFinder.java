package platform.plugins.installables.network.classfinder;

/**
 * General interface of a service that find java byte code.
 * Define the method:
 * findClass that looks for the byte code of a class (it can be done using broadcast, multicast or a specific server)
 * @author Dalmau
 */
public interface IClassFinder {

    /**
     * Sends a broadcasted message used for requesting a class on network and
     * returns the reply
     * @param classeDemandee requested class
     * @return the reply
     * @throws ClassNotFoundException when there is no reply
     */
    public byte[] findClass(String classeDemandee) throws ClassNotFoundException;
}
