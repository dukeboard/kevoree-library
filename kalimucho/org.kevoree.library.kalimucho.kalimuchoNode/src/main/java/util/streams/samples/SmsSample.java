package util.streams.samples;

/**
 * Class of a sample to send/receive phone numbers and a texts to send as an SMS
 *
 * @author Dalmau
 */
public class SmsSample extends Sample {

    private static final long serialVersionUID = 64240010200000003L; // pour serialisation
    private String telephone, texto;;

    /**
     * Constructor whithout parameters for serialization
     */
    public SmsSample() {}

    /**
     * Construction with a phone number and a text
     * @param tel phone number
     * @param txt SMS message
     */
    public SmsSample(String tel , String txt) {
        telephone = tel;
        texto = txt;
    }

    /**
     * Returns the phone number
     * @return the phone number
     */
    public String getPhoneNumber() { return telephone; }

    /**
     * Returns the sms
     * @return the sms
     */
    public String getSmsText() { return texto; }

}
