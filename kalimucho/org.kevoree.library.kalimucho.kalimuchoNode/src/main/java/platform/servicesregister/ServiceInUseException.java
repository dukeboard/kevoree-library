package platform.servicesregister;

/**
 * Exception rose when a service already exists
 *
 * @author Dalmau
 */
public class ServiceInUseException extends Exception  {

    /**
     * Message of this exception
     */
    public static final String SERVICE_USED= "Service deja enregistre";

    private static final long serialVersionUID = 64240040500000002L;
    private String message = null;

    /**
     * Construction with standard message
     */
    public ServiceInUseException() {
		super();
		this.message = SERVICE_USED;
	}

    /**
     * Construction with custom message
     *
     * @param message Custom message
     */
    public ServiceInUseException(String message) {
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
