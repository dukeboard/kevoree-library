package util.streams.samples;

/**
 * Class of a sample to send/receive doubles
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons en reels de type "double"
public class DoubleSample extends Sample {

    private double valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter
     */
    public DoubleSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v the double
     */
    public DoubleSample(double v) {
        valeur=v;
    }

    /**
     * Returns the value
     *
     * @return The double
     */
    public double getValue() {
        return valeur;
    }

}
