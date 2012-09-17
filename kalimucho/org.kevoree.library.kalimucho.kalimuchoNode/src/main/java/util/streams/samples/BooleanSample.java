package util.streams.samples;

/**
 * Class of a sample to send/receive booleans
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons en entiers
public class BooleanSample extends Sample {

    private static final long serialVersionUID = 64240050101000001L; // pour serialisation
    private boolean valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter
     */
    public BooleanSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v The value
     */
    public BooleanSample(boolean v) {
        valeur=v;
    }

    /**
     * Get the value
     *
     * @return the boolean value
     */
    public boolean getValue() {
        return valeur;
    }
    

}
