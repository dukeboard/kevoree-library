package util.streams.samples;

/**
 * Class of a sample to send/receive floats
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons en reels de type "double"
public class FloatSample extends Sample {

    private float valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter
     */
    public FloatSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v the double
     */
    public FloatSample(float v) {
        valeur=v;
    }

    /**
     * Returns the value
     *
     * @return The double
     */
    public float getValue() {
        return valeur;
    }
    
}
