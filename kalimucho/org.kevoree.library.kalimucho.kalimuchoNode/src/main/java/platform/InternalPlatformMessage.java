package platform;

import network.platform.NetworkPlatformMessage;
import model.osagaia.BCModel;
import network.ClassLoaderObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Internal message that looks like a NetworkPlatformMessage but:<br>
 * The BC is an object instead of a byte array
 * The class loader used to create the BC is stored in this message
 *
 * @author Dalmau
 */

public class InternalPlatformMessage extends NetworkPlatformMessage {

    private BCModel bc;
    private ClassLoader classLoader;

    /**
     * Creates a message without BC
     *
     * @param own the service to which this message is
     * @param addr address of the host to send this message to
     */
    public InternalPlatformMessage(String own, String addr) {
        super (own, addr);
        setReplyTo(own);
        bc = null;
        classLoader = null;
    }

    /**
     * Creates a message from a network message.
     * If there is a serialized BC as a byte array in the message, it is converted into an object.
     *
     * @param npfm the network message
     * @throws ClassNotFoundException if the byte array containing the bc in the NetworkPlatformMessage<br>
     * is of an unknown class and this class cant be downloaded by network
     */
    public InternalPlatformMessage(NetworkPlatformMessage npfm) throws ClassNotFoundException {
        super (npfm.getOwner(), npfm.getAddress());
        setPortNumber(npfm.getPortNumber());
        setFinalAddress(npfm.getFinalAddress());
        setFinalPort(npfm.getFinalPort());
        setExpeditorAddress(npfm.getExpeditorAddress());
        setExpeditorPort(npfm.getExpeditorPort());
        setReplyTo(npfm.getReplyTo());
        addContent(npfm.getContent());
        ClassLoaderObjectInputStream ois = null;
        if (npfm.getSerializedObject().length != 0) { // si le message contient un CM serialise
            try { // le convertir en objet en utilisant le bon chargeur de classe
                ois = new ClassLoaderObjectInputStream(new ByteArrayInputStream(npfm.getSerializedObject()));
                bc = (BCModel)ois.readObject(); // deserialisation en objet
            }
            catch (IOException ioe) { bc = null; }
        }
        else bc = null;
        if (ois != null) classLoader = ois.getLoadedClass().getChargeur();
        else classLoader = null;
    }

    /**
     * Creates a message with a BC
     *
     * @param own the service to which this message is
     * @param addr address of the host to send this message to
     * @param cm the serialized BC
     */
    public InternalPlatformMessage(String own, String addr, BCModel cm) {
        super (own, addr);
        setReplyTo(own);
        bc = cm;
        classLoader = null;
    }

    /**
     * Creates a message with a BC and a class loader
     *
     * @param own the service to which this message is
     * @param addr address of the host to send this message to
     * @param cm the serialized BC
     * @param cl the associated class loader
     */
    public InternalPlatformMessage(String own, String addr, BCModel cm, ClassLoader cl) {
        super (own, addr);
        setReplyTo(own);
        bc = cm;
        classLoader = cl;
    }

    /**
     * Returns the BC in the message or null if none
     * @return the BC in the message or null if none
     */
    public BCModel getBC() { return bc; }
    /**
     * Sets  the BC in the message
     * @param cm  the BC to put in the message
     */
    public void setBC(BCModel cm) {
        bc= cm;
    }

    /**
     * Returns the class loader associated to the BC in the message
     * @return the class loader associated to the BC in the message
     */
    public ClassLoader getClassLoader() { return classLoader; }
    /**
     * Sets  the class loader associated to the BC in the message
     * @param cl  the class loader associated to the BC in the message
     */
    public void setClassLoader(ClassLoader cl) { classLoader = cl; }

}
