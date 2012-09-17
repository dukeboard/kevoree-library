
/**
 *
 * @author ccassag1
 */

package model;

// Classe des exceptions utilisees pour arreter un composant metier
// ou un composant de transfert de flux dans un connecteur

/**
 * Exception used by the platform to stop a BC
 *
 * @author ccassag1 modifyed by Dalmau
 */
public class StopBCException extends Exception {

    /**
     * Standard message of this exception
     */
    public static final String STOP_THREAD= "stop current thread component";

    private static final long serialVersionUID = 64240020000000001L;
	private String message = null;
	
    /**
     * Construction with a standard message
     */
    public StopBCException() {
		super();
		this.message = STOP_THREAD;
	}
	
    /**
     * Construction with custom message
     * @param message custom message
     */
    public StopBCException(String message) {
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
