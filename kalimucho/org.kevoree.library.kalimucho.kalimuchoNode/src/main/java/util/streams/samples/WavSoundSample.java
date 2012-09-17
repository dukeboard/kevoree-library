package util.streams.samples;

/**
 * Class of a sample to stream sound<br>
 * Each sample contains a part of sound signal for streaming.<br>
 * Contains:<br>
 * &nbsp;&nbsp;&nbsp; sound format (linear PCM)<br>
 * &nbsp;&nbsp;&nbsp; number of channels (mono/stereo)<br>
 * &nbsp;&nbsp;&nbsp; frequency<br>
 * &nbsp;&nbsp;&nbsp; coding model (8 ou 16 bits)<br>
 * &nbsp;&nbsp;&nbsp; a byte array with a part of the sound signal<br>
 *
 * @author Dalmau
 */

public class WavSoundSample extends Sample {

    private int format, channels, rate, bits;
    private byte[] son;

    /**
     * Create an empty WAV sample
     */
    public WavSoundSample() {
    }

    /**
     * Create a WAV sample
     * @param f encoding format
     * @param c number of channels
     * @param r sampling rate
     * @param b encoding number of bits
     * @param s byte array containing sound samples
     */
    public WavSoundSample(int f, int c, int r, int b, byte[]s) {
        format = f;
        channels = c;
        rate = r;
        bits = b;
        son = s;
    }

    /**
     * Returns the encoding format
     * @return the encoding format
     */
    public int getFormat() { return format; }

    /**
     * Returns the number of channels
     * @return the number of channels
     */
    public int getChannels() { return channels; }

    /**
     * Returns the sampling rate
     * @return the sampling rate
     */
    public int getRate() { return rate; }

    /**
     * Returns the encoding number of bits
     * @return the  encoding number of bits
     */
    public int getBits() { return bits; }

    /**
     * Returns a byte array containing sound samples
     * @return the byte array containing sound samples
     */
    public byte[] getSound() { return son; }

}
