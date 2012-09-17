package util;

import java.util.zip.Deflater;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A convenient class to compress byte arrays.
 * It can be use with the DecompressByteArray class to reduce network load.
 * @author Dalmau
 */
public class CompressByteArray {

    /**
     * Upper bound for the compression level range
     */
    public final static int BEST_COMPRESSION = Deflater.BEST_COMPRESSION;
    /**
     * Lower bound for compression level range
     */
    public final static int BEST_SPEED  = Deflater.BEST_SPEED;
    /**
     * Usage of the default compression level
     */
    public final static int DEFAULT_COMPRESSION  = Deflater.DEFAULT_COMPRESSION;
    /**
     * Default value for compression strategy
     */
    public final static int DEFAULT_STRATEGY  = Deflater.DEFAULT_STRATEGY;
    /**
     * Default value for compression method
     */
    public final static int DEFLATED  = Deflater.DEFLATED;
    /**
     * Possible value for compression strategy
     */
    public final static int FILTERED  = Deflater.FILTERED;
    /**
     * Possible value for compression strategy
     */
    public final static int HUFFMAN_ONLY  = Deflater.HUFFMAN_ONLY;
    /**
     * Possible value for compression level.
     */
    public final static int NO_COMPRESSION  = Deflater.NO_COMPRESSION;

    private byte[] aCompresser;

    /**
     * Creates a compressor for a byte array
     * @param ac the byte array to compress
     */
    public CompressByteArray(byte[] ac) {
        aCompresser = ac;
    }

    /**
     * Compress the byte array in default compression level
     * @return the compressed byte array
     */
    public byte[] compress() {
        return compress(DEFAULT_COMPRESSION);
    }

    /**
     * Compress the byte array in specified compression level
     * @param mode compression level
     * @return the compressed byte array
     */
    public byte[] compress(int mode) {
        Deflater compresseur = new Deflater();
        compresseur.setLevel(mode);
        compresseur.setInput(aCompresser);
        compresseur.finish();
        ByteArrayOutputStream compresse = new ByteArrayOutputStream(aCompresser.length);
        byte[] buf = new byte[1024];
        while (!compresseur.finished()) {
            int octetsLus = compresseur.deflate(buf);
            compresse.write(buf, 0, octetsLus);
        } // l'image compressee est dans imageYuvCompressee
        try {
            compresse.close();
            return compresse.toByteArray();
        }
        catch (IOException e) {
            return null;
        }

    }

}
