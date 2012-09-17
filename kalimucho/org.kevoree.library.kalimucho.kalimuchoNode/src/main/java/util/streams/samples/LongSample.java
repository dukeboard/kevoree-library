package util.streams.samples;

/**
 * Class of a sample to send/receive longs
 * @author Dalmau
 */
public class LongSample extends Sample {

    private long valeur; // la valeur de l'echantillon

    /**
     * Construction without parameter necessary to de-serialize
     */
    public LongSample() {
        // necessaire pour pouvoir le deserialiser
    }

    /**
     * Construction with a value
     *
     * @param v The value
     */
    public LongSample(long v) {
        valeur=v;
    }

    /**
     * Get the value
     *
     * @return the integer
     */
    public long getValue() {
        return valeur;
    }

}

