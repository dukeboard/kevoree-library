package util.streams.samples;

/**
 * Class of a sample to send/receive strings
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons en reels de type "double"
public class StringSample extends Sample {

    private static final long serialVersionUID = 64240050101000009L; // pour serialisation
    private String valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter
     */
    public StringSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a string
     *
     * @param v the double
     */
    public StringSample(String v) {
        valeur=v;
    }

    /**
     * Returns the string
     *
     * @return The double
     */
    public String getValue() {
        return valeur;
    }
    
}
