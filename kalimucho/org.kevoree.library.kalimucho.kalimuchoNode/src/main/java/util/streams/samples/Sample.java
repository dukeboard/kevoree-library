package util.streams.samples;

import java.io.Serializable;

/**
 * Abstract serializable class from which inherit all the classes of samples
 * read and writed by BCs.<br>
 * This class as the same role than the class Object for samples.<br>
 * This class provides a clone method (similar to the clone  method in Object).
 * This method used a serialize/deserialise mechanism that can be slow.<br>
 * So, in order to fasten the cloning it is usefull to override this method in each onherited class of samples.
 *
 * @author Dalmau
 */

// Classe abstraite pour ceer des echantillons pouvant etre transportes par des connecteurs
// Ces echantillons doivent etre serialisables.

public abstract class Sample implements Serializable {
    
    private static final long serialVersionUID = 64240050101000000L; // pour serialisation
    private transient int inputNumber;
    private transient String exp;
    private transient long date;

    /**
     * Construction without parameter
     */
    public Sample() {
        date = System.currentTimeMillis();
        inputNumber = 0;
        // necessaire pour pouvoir le deserialiser
    }
    
    /**
     * Returns the number of the entry the sample comes from
     * @return number of the entry the sample comes from
     */
    public int getInputNumber() { return inputNumber; }
    
    /**
     * Sets the number of the entry the sample comes from
     * @param number of the entry the sample comes from
     */
    public void setInputNumber(int number) { inputNumber = number; }

    /**
     * Returns the address of the host which sends this sample.
     * If this sample is local the returned value is "localhost" (IP local host)
     * @param adr of the host which sends this sample
     */
    public void setExpeditor(String adr) { exp = adr; }

    /**
     * Sets the address of the host which sends this sample.
     * If this sample is local the value is "localhost" (IP local host)
     * @return adr address of the host which sends this sample
     */
    public String getExpeditor() { return exp; }

    /**
     * Sets the sample's date
     * @param d  the sample's date
     */
    public void setDate(long d) {
        date = d;
    }

    /**
     * Returns the sample's date
     * @return  the sample's date
     */
    public long getDate() { return date; }

    /**
     * Returns the age of the sample (actual time - sample date in local time)
     * @return the age of the sample
     */
    public long getAge() {
        return System.currentTimeMillis()-date;
    }
    
}
