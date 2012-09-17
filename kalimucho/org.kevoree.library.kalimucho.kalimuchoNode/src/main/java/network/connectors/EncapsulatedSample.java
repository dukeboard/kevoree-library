package network.connectors;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import platform.ClassManager.ChangeClassInputStream;
import util.streams.samples.Sample;
import java.io.Serializable;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import util.NetworkAddress;


/**
 * This class is used to encapsulate samples created by BCs.<br>
 * Connectors receive encapsulated data because the normally sent class
 * of samples can be an unknown class for the connector.
 * The OU of the Osagaia container always encapsulate the samples produced by the BC before sending them to a connector.
 * The IU  of the Osagaia container  desencapsulate the samples before giving them to the BC.
 *
 * @author Dalmau
 */
public class EncapsulatedSample implements Serializable {

    private static final long serialVersionUID = 64240050101000111L; // pour serialisation
    /**
     * The number of extra bytes in the EncapsulatedSample when serialized from the size of its contents.
     * This information is used by the platform to calculate the number of bytes sent or received on the network.
     */
    public transient static final int EXTRA_SIZE = 111;
    private byte[] contenu;
    private transient String expediteur = "unknown";
    private long date;

    /**
     *
     */
    public EncapsulatedSample() {
        contenu = new byte[0];
        expediteur = "unknown";
        date = System.currentTimeMillis();
    }

    /**
     * Create an encapsulated sample from a Sample
     * @param ini the sample to encapsulate
     */
    public EncapsulatedSample(Sample ini) {
        date = ini.getDate();
        expediteur = "unknown";
        contenu = new byte[0];
        if (ini != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(ini); // serialisation en octets
                contenu = bos.toByteArray();
            }
            catch (IOException ioe) {
            	System.err.println("ByteArraySample: Cant convert Sample to byte array");
            }
        }
    }

    /**
     * Returns the size of the byte array included in this encapsulated sample
     * @return the size of the byte array included in this encapsulated sample
     */
    public int size() {return contenu.length; }

    /**
     * When an encapsulated sample is received by network, the address of the expeditor is set by this method
     * @param exp the address of the expeditor of an encapsulated sample received by network
     */
    public void setExpeditor(String exp) { expediteur = exp; }

    /**
     * Desencapsulate a sample according to a given class loader
     * @param cl the class loader used to desencapsulate the sample (normaly the class loader of the BC that uses this sample)
     * @return the desencapsulated sample
     */
    public Sample getSample(ClassLoader cl) {
        Sample retour;
        ByteArrayInputStream bis = new ByteArrayInputStream(contenu);
        try {
            ChangeClassInputStream ois = new ChangeClassInputStream(bis, cl);
            retour = (Sample)ois.readObject(); // deserialisation
            retour.setDate(date);
            if (expediteur != null) retour.setExpeditor(expediteur);
            else retour.setExpeditor("unknown");
        }
        catch (IOException ioe) {
            System.err.println("EncapsulatedSample: Cant read encapsulated Sample in byte array");
            retour = null;
        }
        catch (ClassNotFoundException cnfe) { 
            System.err.println("EncapsulatedSample: Class error converting EncapsulatedSample to Sample");
            retour = null;
        }
        return retour;
    }

    /**
     * Adds the actual local time to the encapsulated sample
     */
    public void setLocalDate() {
        date = System.currentTimeMillis();
    }

    /**
     * Sets the date of the encapsulated sample in local time:
     * This date is calculated from the remote date according to the clock shift between
     * the sender and the local host (if available). Otherwise the date is the local reception date.
     * @param dns the dns service (if this service is not active this methos is not called)
     * @param expediteur the address of the sender of the encapsulated sample
     */
    public void setRemoteDate(KalimuchoDNS dns, NetworkAddress expediteur) {
        String IDexpeditor = dns.getHostID(expediteur);
        if (IDexpeditor != null) {
            if (dns.isRemoteHostClockShiftAvailable(IDexpeditor)) date = date+dns.getRemoteHostClockShift(IDexpeditor);
        }
        else setLocalDate();
    }
    
}
