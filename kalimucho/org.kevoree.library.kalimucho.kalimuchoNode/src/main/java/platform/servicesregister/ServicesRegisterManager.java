package platform.servicesregister;

/**
 * Register services accessible by the platform.
 * A service is registered under a symbolic name and associated to an object
 * from which methods can be called.<br>
 * The class ServicesRegisterManager is static and can't be instantiated,
 * it offers methods for:<br>
 * <ul>
 * <li>	Register a service by a name associated to the object which provides the service.
 * If the service is allready registered a ServiceInUseException is raised.
 * <li>	Unregistrer a service indicated by its name.
 * If the service is not registered a ServiceClosedException is raised.
 * <li>	Look for a service indicated by its name;
 * return the object which provides this service.
 * If the service is not registered a ServiceClosedException is raised.
 * <li>	Wait for a service indicated by its name to be available;
 * returns the object which provides this service.
 * If the service is not yet registered requester is suspended on a semaphore
 * until the service becomes available.
 *
 * @author Dalmau
 */

// Permet d'enregistrer un objet sous un nom pour le retrouver
// Remplace le mecanisme d'enregistreur dee services (InterIsolateServer) de squawk
public class ServicesRegisterManager extends ServicesRegister {

    // Methode d'enregistrement d'un service
    /**
     * Register a service
     *
     * @param name name of the service
     * @param service Object which provides this service
     * @throws ServiceInUseException Exception raised if the service is already registered
     */
    static public void registerService(String name, Object service) throws ServiceInUseException {
        synchronized(ServicesRegisterManager.class) { // semaphore sur lequel se bloquent ceux qui attendent un service
            try {
                lookForService(name); // verifier que ce service n'existe pas deje
                throw new ServiceInUseException("Service "+name+" deja inscrit");
            }
            catch (ServiceClosedException sce) { // le service n'existait pas deje
                enregistrement.put(name, service); // l'enregistrer
//System.out.println("enregistrement du service : "+name+" assure par : "+service.getClass().getName());
            }
            ServicesRegisterManager.class.notifyAll(); // debloquer ceux qui attendent un service
        }
    }

    // Methode de desenregistrement d'un service
    /**
     * Remove a service
     *
     * @param name name of the service to remove
     * @throws ServiceClosedException Exception if the service is not registered
     */
    static public synchronized void removeService(String name) throws ServiceClosedException {
        if (enregistrement.containsKey(name)) enregistrement.remove(name); // s'il existe l'enlever
        else throw new ServiceClosedException(); // sinon c'est une erreur
    }

    // Methodes qui attendent qu'un service soit enregistre
    /**
     * This method is used by the PF for accessing internal services
     * Wait for a service. The requester is suspended on a semaphore
     * until the service exists.
     *
     * @param name Name of the service to wait for
     * @return The object providing this service
     */
    static public Object platformWaitForService(String name) {
        try { return waitForService(name); }
        catch (InterruptedException ie) { return null; }
    }

}
