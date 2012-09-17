package platform.context;

/**
 * General class for context information.
 * For the moment it stores only a String (context description) and the system time when created.
 * @author Dalmau
 */
public class ContextInformation {

    private String information;
    private long date;

    /**
     * Create a context information and adds the current date
     * @param info the contexte information
     */
    public ContextInformation(String info) {
        information = info;
        date = System.currentTimeMillis();
    }

    /**
     * Returns the context information
     * @return the context information
     */
    public String getInformation() { return information; }

    /**
     * Returns the date of the context information
     * @return the date of the context information
     */
    public long getDate() { return date; }

}
