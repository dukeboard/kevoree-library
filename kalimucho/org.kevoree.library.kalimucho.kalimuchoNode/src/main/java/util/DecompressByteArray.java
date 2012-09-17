package util;

import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A convenient class to decompress byte arrays.
 * It can be use with the CompressByteArray class to reduce network load.
 *
 * @author Dalmau
 */
public class DecompressByteArray {
    private byte[] aDecompresser;

    /**
     * Creates a decompressor for a byte array
     * @param adc the byte array to decompress
     */
    public DecompressByteArray(byte[] adc) {
        aDecompresser = adc;
    }

    /**
     * Decompress the byte array
     * @return the decompressed byte array
     * @throws DataFormatException when the initial byte array is in an unknown fcompression format
     */
    public byte[] decompress() throws DataFormatException {
        Inflater decompresseur = new Inflater();
        decompresseur.setInput(aDecompresser);
        ByteArrayOutputStream decompresse = new ByteArrayOutputStream(aDecompresser.length);
        byte[] buf = new byte[1024];
        while (!decompresseur.finished()) {
            int octetsLus = decompresseur.inflate(buf);
            decompresse.write(buf, 0, octetsLus);
        } // imageYUV contient l'image decompressee
        try { 
            decompresse.close();
            return decompresse.toByteArray();
        }
        catch (IOException e) {
            return null;
        }
    }

}
