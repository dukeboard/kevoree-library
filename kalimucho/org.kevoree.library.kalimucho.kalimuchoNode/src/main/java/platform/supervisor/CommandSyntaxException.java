package platform.supervisor;

/**
 * Exception rose by the supervisor's command analyser when the command is incorrect.
 * 
 * @author Dalmau
 */
public class CommandSyntaxException extends Exception {

    /**
     * Standard message of this exception
     */
    public static final String STOP_THREAD= "Bad command syntax";

    private static final long serialVersionUID = 64240040600000001L;
    private String message = null;
	
    /**
     * Construction with a standard message
     */
    public CommandSyntaxException() {
		super();
		this.message = STOP_THREAD;
	}
	
    /**
     * Construction with custom message
     * @param message custom message
     */
    public CommandSyntaxException(String message) {
		super(message);
		this.message = message;
	}
	
    /**
     * Returns the message
     * @return message
     */
    @Override
    public String getMessage() {
		return this.message;
	}
}
