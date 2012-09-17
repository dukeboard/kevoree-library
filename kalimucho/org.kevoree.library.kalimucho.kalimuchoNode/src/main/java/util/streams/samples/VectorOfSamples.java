package util.streams.samples;

import java.util.Vector;

/**
 * Class of a sample to send/receive Vectors of samples.<br>
 * A VectorOfSamples is a vector which holds elements of class Sample or inherited
 *
 * @author Dalmau
 */

// Extention de la classe Echantillon pour des echantillons
// constitues de vecteurs dont les elements sont des echantillons

public class VectorOfSamples extends Sample {

    private Vector valeur; // vecteur d'echantillons

    /**
     * Construction without parameter
     */
    public VectorOfSamples() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v the Vector
     */
    public VectorOfSamples(Vector v) {
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
