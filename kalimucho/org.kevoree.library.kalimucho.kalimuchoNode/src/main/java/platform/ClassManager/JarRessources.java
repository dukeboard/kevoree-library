package platform.ClassManager;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.InputStream;

/**
 * JarResources: JarResources maps all resources included in a
 * Zip or Jar file. Additionaly, it provides a method to extract one
 * as a blob.
 * @author Dalmau adapted from http://www.javaworld.com/javaworld/javatips/javatip70/JarResources.java
 */
public class JarRessources {

    // external debug flag
    /**
     * Indicator to set to true is debug traces as needed
     */
    public boolean debugOn = false;
    // jar resource mapping tables
    private Hashtable<String, Integer> htSizes;
    private Hashtable<String, byte[]> htJarContents;
    // a jar file
    private String jarFileName;

    /**
     * creates a JarResources. It extracts all resources from a Jar
     * into an internal hashtable, keyed by resource names.
     * @param jarFileName a jar or zip file
     */
    public JarRessources(String jarFileName) {
        this.jarFileName = jarFileName;
        htSizes = new Hashtable<String, Integer>();
        htJarContents = new Hashtable<String, byte[]>();
        init();
    }

    /**
     * Extracts a jar resource as a byte array.
     * @param name a resource name.
     * @return the jar resource as a byte array
     */
    public byte[] getRessource(String name) {
        return htJarContents.get(name);

    }

    /**
     * Returns an Input stream on the a resource in jar file
     * @param name name of the resource
     * @return an Input stream on the a resource in jar file
     * @throws IOException when this resource can't be read in the jar file
     */
    public InputStream getRessourceAsStream(String name) throws IOException {
        ZipFile zf = new ZipFile(jarFileName);
        Enumeration<?> e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            if (ze.getName().equals(name)) {
                return zf.getInputStream(ze);
            }
        }
        return null;
    }

    /** initializes internal hash tables with Jar file resources.  */
    private void init() {
        try {
            // extracts just sizes only.
            ZipFile zf = new ZipFile(jarFileName);
            Enumeration<?> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                htSizes.put(ze.getName(), new Integer((int) ze.getSize()));
            }
            zf.close();

            // extract resources and put them into the hashtable.
            FileInputStream fis = new FileInputStream(jarFileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }

                if (debugOn) {
                    System.out.println("ze.getName()=" + ze.getName()
                            + "," + "getSize()=" + ze.getSize());
                }

                int size = (int) ze.getSize();
                // -1 means unknown size.
                if (size == -1) {
                    size =  htSizes.get(ze.getName()).intValue();
                }

                byte[] b = new byte[size];
                int rb = 0;
                int chunk = 0;
                while ((size - rb) > 0) {
                    chunk = zis.read(b, rb, size - rb);
                    if (chunk == -1) {
                        break;
                    }
                    rb += chunk;
                }

                // add to internal resource hashtable
                htJarContents.put(ze.getName(), b);

                if (debugOn) {
                    System.out.println(ze.getName() + "  rb=" + rb
                            + ",size=" + size
                            + ",csize=" + ze.getCompressedSize());
                }
            }
            zis.close(); fis.close();
        }
        catch (NullPointerException e) {
            //System.out.println("done.");
        }
        catch (FileNotFoundException e) {
            System.err.println("JarRessource : can't find file :"+jarFileName);
        }
        catch (IOException e) {
            System.err.println("JarRessource : can't read file :"+jarFileName);
        }
    }
}

