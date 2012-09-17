package platform.containersregister;

import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import util.Parameters;

/**
 * This class is the manager for all containers (BC and connectors).
 *
 * @author Dalmau
 */
public class ContainersManager {

    private ComponentsRegistration composants;
    private ConnectorsRegistration connecteurs;

    /**
     *
     */
    public ContainersManager() {
        composants = new ComponentsRegistration();
        connecteurs = new ConnectorsRegistration();
        try {
            ServicesRegisterManager.registerService(Parameters.CONTAINERS_MANAGER, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Containers Manager service already created");
        }
    }

    /**
     * Returns the components' containers manager
     * @return the components' containers manager
     */
    public ComponentsRegistration getComposants() { return composants; }
    /**
     * Returns  the connectors' containers manager
     * @return the connectors' containers manager
     */
    public ConnectorsRegistration getConnecteurs() { return connecteurs; }

}
