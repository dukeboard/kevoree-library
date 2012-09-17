package platform.containersregister;

/**
 * Descriptor on a connected input = name of the component and number of the input
 * @author Dalmau
 */
public class ConnectedInput {
    private String nom;
    private int entree;

    /**
     * Construction of a descriptor
     * @param name name of the component
     * @param input number of its input
     */
    public ConnectedInput(String name, int input) {
        nom = name;
        entree = input;
    }

    /**
     * Gets the name of the component
     * @return the name of the component
     */
    public String getName() { return nom; }

    /**
     * Gets the number of the input
     * @return the number of the input
     */
    public int getInput() { return entree; }

}
