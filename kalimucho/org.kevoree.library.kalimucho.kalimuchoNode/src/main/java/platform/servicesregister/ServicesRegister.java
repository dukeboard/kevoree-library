package platform.servicesregister;

import java.util.HashMap;

/**
 * Manager of registered services usable by BCs.<br>
 * It only offers methods for getting a registered service (giving its name):<br>
 * lookForService returns a registered service or raises a ServiceClosedException<br>
 * waitForService lock the requester on a semaphore until the requested service becomes available
 * @author Dalmau
 */
public class ServicesRegister {

    /**
     * The list of registered services
     */
    protected static HashMap<String, Object> enregistrement = new HashMap<String, Object>(); // services enregistres

    // Methode de recherche d'un service, s'il n'existe pas leve une exception sinon le renvoie
    /**
     * Find a service
     *
     * @param name Name of the service to find
     * @return The object which provides the service
     * @throws ServiceClosedException Exception if the service is closed
     */
    static public synchronized Object lookForService(String name) throws ServiceClosedException {
        Object reponse = enregistrement.get(name);
        if (reponse != null) {
            return reponse; // objet qui assure le service
        }
        else throw new ServiceClosedException(); // le service n'existe pas
    }

    /**
     * This method is used by BC for accessing services of the PF
     * Wait for a service. The requester is suspended on a semaphore
     * until the service exists or the BC is interrupted.
     *
     * @param name Name of the service to wait for
     * @return The object providing this service
     * @throws InterruptedException allows to stop a BC when waiting for a service
     */
    static public Object waitForService(String name) throws InterruptedException {
        synchronized(ServicesRegisterManager.class) { // semaphore pour bloquer quand le service n'est pas encore enregistre
            boolean cree = false;
            Object ret = null;
            while (!cree) { // boucle pour recommencer si le service n'est pas encore cree
                try {
                    ret = lookForService(name);
                    cree=true; // on a trouve le service => on ne reessaie plus
                }
                catch (ServiceClosedException ex) {
                    // se bloquer jusqu'e refaire une tentative
                    cree=false;
                    ServicesRegisterManager.class.wait(); // peut ?tre interrompu par InterruptedException
                }
            }
            return ret;
        }
    }

}
