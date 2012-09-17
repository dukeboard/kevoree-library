package util.streams.samples;

/**
 * Class of a sample to send/receive integers
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons en entiers
public class IntegerSample extends Sample {

    private int valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter
     */
    public IntegerSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v The value
     */
    public IntegerSample(int v) {
        valeur=v;
    }

    /**
     * Get the value
     *
     * @return the integer
     */
    public int getValue() {
        return valeur;
    }
    
}
