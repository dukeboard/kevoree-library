
package util.streams.samples;

import java.util.Vector;

/**
 * Class of a sample to send/receive Vectors of strings.<br>
 *
 * @author Dalmau
 */
public class VectorOfStrings extends Sample {
    
    private Vector valeur; // vecteur d'echantillons

    /**
     * Construction without parameter
     */
    public VectorOfStrings() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v the Vector
     */
    public VectorOfStrings(Vector v) {
        valeur=v;
    }

    /**
     * Get the value
     *
     * @return the Vector
     */
    public Vector getValue() {
        return valeur;
    }
    
}
