package platform.plugins.installables.network.routing;

/**
 * exception raise by the routing service when no route can be found to a given host.
 * @author Dalmau
 */
public class NoRouteException extends Exception   {
    /**
     * Indicates that no route is found
     */
    public static final String NO_ROUTE= "Pas de route possible";

    private static final long serialVersionUID = 64240040400000001L;
    private String message = null;

    /**
     * Construction with standard message
     */
    public NoRouteException() {
		super();
		this.message = NO_ROUTE;
	}

    /**
     * Construction with custom message
     *
     * @param message Custom message
     */
    public NoRouteException(String message) {
		super(message);
		this.message = message;
	}

    /**
     * Get the exception message
     *
     * @return the exception message
     */
    @Override
    public String getMessage() {
		return this.message;
	}

}
