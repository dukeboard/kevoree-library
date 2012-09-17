package util;

import java.io.OutputStream;
import java.io.IOException;

/**
 * An output stream used only to get the size of a java object.
 * The object is serialized in this stream through an ObjectOutputStream
 * that only performs a count of writen bytes.
 *
 * @author Dalmau
 */

public class SizeCountOutputStream extends OutputStream {
    
    int taille;

    /**
     * Create a stream for size count
     */
    public SizeCountOutputStream() {
        super();
        taille = 0;
    }

    /**
     * Closes the stream
     * @throws IOException if an errors occors during closing
     */
    @Override
    public void close() throws IOException {
        super.close();
    }
    /**
     * flush the stream
     * @throws IOException if an errors occors during flushing
     */
    @Override
    public void flush() throws IOException {
        super.flush();
    }
    /**
     * Overrides the write method in order to count writed bytes
     * @param b the byte to write
     */
    public void write(int b) { taille++; }
    /**
     *  Overrides the write method in order to count writed bytes
     * @param b the byte array to write
     */
    @Override
    public void write(byte[] b) { taille = taille+b.length; }
    /**
     *  Overrides the write method in order to count writed bytes
     * @param b the byte to partially write
     * @param offset satarting point
     * @param length size
     */
    @Override
    public void write(byte[] b, int offset, int length) { taille = taille+length; }

    /**
     * Returns the size on data written in the stream
     * @return the size on data written in the stream
     */
    public int getSize() { return taille; }

}
