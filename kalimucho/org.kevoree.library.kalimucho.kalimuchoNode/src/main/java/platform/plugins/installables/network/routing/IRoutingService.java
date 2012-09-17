package platform.plugins.installables.network.routing;

import util.NetworkAddress;

/**
 * Interface for all Routing services.
 * Define the method:
 * findRoute that looks for a route (it can be done using broadcast, multicast or a specific server)
 *
 * @author Dalmau
 */
public interface IRoutingService {

    /**
     * Find a route
     * @param vers to which host
     * @return the founded route
     * @throws NoRouteException if there is no route
     */
    public ReplyForRouteMessage findRoute(NetworkAddress vers) throws NoRouteException ;
}
