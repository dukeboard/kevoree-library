package platform.servicesregister;

/**
 * Exception rose when a service is not availble
 *
 * @author Dalmau
 */
public class ServiceClosedException extends Exception  {

    /**
     * Message of this exception
     */
    public static final String SERVICE_CLOSED= "Service ferme";

    private static final long serialVersionUID = 64240040500000001L;
    private String message = null;

    /**
     * Construction with standard message
     */
    public ServiceClosedException() {
		super();
		this.message = SERVICE_CLOSED;
	}

    /**
     * Construction with custom message
     *
     * @param message Custom message
     */
    public ServiceClosedException(String message) {
		super(message);
		this.message = message;
	}

    /**
     * Returns the message
     *
     * @return The exception message
     */
    @Override
    public String getMessage() {
		return this.message;
	}


}
