package util.streams.samples;

/**
 * Class of a sample to send/receive images.
 * This sample contains an image included in a byte array and a format ("png", "jpg")
 * The byte array is exactly the same that can be read in a file of this format
 *
 * @author Dalmau
 */
public class ImageSample extends Sample {

    private String format;
    private byte[] photo;

    /**
     * Constructor whithout parameters for serialization
     */
    public ImageSample() {
        format = null;
        photo = null;
    }

    /**
     * Construction with a byte array
     * @param original byte array containing the image
     * @param form image format
     */
    public ImageSample(byte[] original, String form) {
        format = form;
        photo = original;
    }

    /**
     * Returns the format of the image
     * @return the format of the image
     */
    public String getFormat() { return format; }

    /**
     * Returns the byte array containing the image
     * @return the byte array containing the image
     */
    public byte[] getPhoto() { return photo; }

}
