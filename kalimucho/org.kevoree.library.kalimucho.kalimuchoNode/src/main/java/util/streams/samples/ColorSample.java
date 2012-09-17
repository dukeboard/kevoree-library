package util.streams.samples;

/**
 * Class of a sample to send/receive colors (RGB)
 * @author Dalmau
 */
public class ColorSample extends Sample {

    private int rouge, vert, bleu;

    /**
     * Construction without parameter
     */
    public ColorSample() {
        // necessaire pour pouvoir le deserialiser
        rouge = 255;
        vert =0;
        bleu = 0;
    }

    /**
     * Construction with a value
     *
     * @param r red 
     * @param v green
     * @param b blue
     */
    public ColorSample(int r, int v, int b) {
        rouge = r;
        vert = v;
        bleu = b;
    }

    /**
     * Get the red
     *
     * @return the red
     */
    public int getRed() {
        return rouge;
    }

    /**
     * Get the green
     *
     * @return the green
     */
    public int getGreen() {
        return vert;
    }

    /**
     * Get the blue
     *
     * @return the blue
     */
    public int getBlue() {
        return bleu;
    }
    
}
