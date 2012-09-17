package network.platform;

import platform.servicesregister.ServicesRegisterManager;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import network.NetworkMessage;
import network.NetworkEmitterContainer;
import util.NetworkAddress;
import platform.InternalPlatformMessage;
import util.Parameters;

/**
 * Extends NetworkMessage and holds:<br>
 * A string which is the platform command<br>
 * The name of the PF service to which this message is<br>
 * The name of the PF servoce to reply to this message<br>
 * A serialized BC as a byte array<br>
 * This class is used by the platform only.
 *
 * @author Dalmau
 */

/*
 * Classe pour envoyer et recevoir des messages par reseau par la PF
 */

public class NetworkPlatformMessage extends NetworkMessage implements Serializable {

    private static final long serialVersionUID = 64240030300000001L; // pour serialisation

    private String contenu; // commande et parametres pour une PF
    private String owner; // service destinataire du message
    private String replyTo; // service auquel repondre
    private byte[] serializedObject; // CM serialise
    private long date;
    /* Le CM serialise est envoye sous forme de tableau d'octets et pas d'objet car
     * lorsque le message est relaye par une PF il est possible que cette PF n'ait
     * pas de chargeur de classe pour ce CM.
     * La PF qui migre un composant le convertit en tableau d'octets
     * La PF qui relaye le message se conten te de transmettre ce tableau d'octets
     *   (elle n'a pas le chargeur de classe pour le convertir en objet)
     * La PF qui recoit le message transforme ce tableau d'octets en objet
     *   (elle a le chargeur de classe pour le faire)
     */
	
    /**
     * Construction without parameters necessary for de-serialisation with KSN
     */
    public NetworkPlatformMessage() {
        // constructeur sans parametre necessaire pour pouvoir deserialiser
        super();
        contenu = "";
        owner = "";
        replyTo = "";
        date = System.currentTimeMillis();
        serializedObject = new byte[0];
 	}
	
    /**
     * Construction of a message
     *
     * @param own PF service for which this message is
     * @param addr Address and port number to send this message to
     */
    public NetworkPlatformMessage(String own, String addr) {
        super(addr);
        NetworkEmitterContainer nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        int port = nec.getPlatformCommandPortNumber(new NetworkAddress(getAddress()).getType());
        setPortNumber(port);
        setExpeditorPort(port);
        contenu = "";
        owner = own;
        replyTo = own;
        date = System.currentTimeMillis();
        serializedObject = new byte[0];
 	}

    /**
     * Construction of a message from an internal platform message
     * If a BC is present as an object it is converted into a byte array.
     *
     * @param ini the internal platform message
     */
    public NetworkPlatformMessage(InternalPlatformMessage ini) {
        this(ini.getOwner(), ini.getAddress());
        setPortNumber(ini.getPortNumber());
        setFinalAddress(ini.getFinalAddress());
        setFinalPort(ini.getFinalPort());
        setExpeditorAddress(ini.getExpeditorAddress());
        setExpeditorPort(ini.getExpeditorPort());
        setReplyTo(ini.getReplyTo());
        setDate(ini.getDate());
        if (ini.getBC() != null) { // s'il y a un CM serialise le convertir en tableau d'octets
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(ini.getBC()); // serialisation en octets
                serializedObject = bos.toByteArray();
            }
            catch (IOException ioe) {
                System.err.println("Cant convert serialized BC to byte array");
                serializedObject = new byte[0];
            }
        }
        else serializedObject = new byte[0]; // pas de CM serialise dans ce message
        addContent(ini.getContent());
    }

    /**
     * Deserializes a NetworkPlatformMessage from a byte array.
     * This is usefull because broadcast or multicast servers can't receive objects but only byte arrays
     * @param content the byte array to deserialize from
     */
    public NetworkPlatformMessage(byte[] content) {
        super(); // pas d'adresse et de port d'envoi
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            finalAddress = (String)ois.readObject();
            finalPort = ((Integer)ois.readObject()).intValue();
            expeditorAddress = (String)ois.readObject();
            expeditorPort = ((Integer)ois.readObject()).intValue();
            senderID = (String)ois.readObject();
            expeditorID = (String)ois.readObject();
            contenu = (String)ois.readObject();
            owner = (String)ois.readObject();
            replyTo = (String)ois.readObject();
            date = ((Long)ois.readObject()).longValue();
            int taille = ((Integer)ois.readObject()).intValue();
            serializedObject = new byte[taille];
            for (int i=0; i<taille; i++) serializedObject[i] = (byte)bis.read();
        }
        catch (IOException ioe) {
            System.err.println("Error converting a received network Platform message : IOError");
        }
        catch (ClassNotFoundException ioe) {
            System.err.println("Error converting a received network Platform message : class not found");
        }
    }

    /**
     * Serializes a NetworkPlatformMessage to a byte array.
     * This is usefull because broadcast or multicast senders can't send objects but only byte arrays
     * @return the serilized NetworkPlatformMessage into a byte array
     */
    public byte[] toByteArray() {
        byte[] retour;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(finalAddress);
            oos.writeObject(new Integer(finalPort));
            oos.writeObject(expeditorAddress);
            oos.writeObject(new Integer(expeditorPort));
            oos.writeObject(senderID);
            oos.writeObject(expeditorID);
            oos.writeObject(contenu);
            oos.writeObject(owner);
            oos.writeObject(replyTo);
            oos.writeObject(new Long(date));
            oos.writeObject(new Integer(serializedObject.length));
            for (int i=0; i<serializedObject.length; i++) bos.write(serializedObject[i]);
            retour = bos.toByteArray();
        }
        catch (IOException ioe) {
            System.err.println("Error converting a network platform message to byte array");
            retour =  null;
        }
        return retour;
    }

    /**
     * Makes a copy a the message.
     * When a message is sent some informations can be modified by the network sender.
     * So it is necessary to make a copy of the message because the same message can be sent on various networks.
     * In this case each sender will modify the message so it cant be the same object.
     * @return a clome of the message
     */
    @Override
    public NetworkPlatformMessage clone() {
        NetworkPlatformMessage copie = new NetworkPlatformMessage();
        copie.setAddress(new String(destinationAddress));
        copie.setPortNumber(destinationPort);
        copie.setExpeditorAddress(new String(expeditorAddress));
        copie.setExpeditorPort(expeditorPort);
        copie.setFinalAddress(new String(finalAddress));
        copie.setFinalPort(finalPort);
        copie.setSenderID(new String(getSenderID()));
        copie.setExpeditorID(new String(getExpeditorID()));
        copie.setOwner(new String(owner));
        copie.setReplyTo(new String(replyTo));
        copie.setDate(System.currentTimeMillis());
        copie.addContent(new String(contenu));
        byte[] code = new byte[serializedObject.length];
        for (int i=0; i<serializedObject.length; i++) code[i] = serializedObject[i];
        copie.setSerializedObject(code);
        return copie;
    }

    /**
     * Add a string to the message
     *
     * @param e The string to be added to the message
     */
    public void addContent(String e) {
        contenu = e;
    }

    /**
     * Get the string in the message
     *
     * @return command or alarm in the message
     */
    public String getContent() {
        return contenu;
    }

    /**
     * Sets the service to which this message is
     * @param own the service to which this message is
     */
    public void setOwner(String own) {
        owner = own;
    }

    /**
     * Sets the service to which the answer of this message is
     * @param to the service to which the answer of this message is
     */
    public void setReplyTo(String to) {
        replyTo = to;
    }
    /**
     * Gets the service to which this message is
     * @return the service to which this message is
     */
    public String getOwner() { return owner; }
    
    /**
     * Gets the service to which the answer of this message is
     * @return the service to which the answer of this message is
     */
    public String getReplyTo() { return replyTo; }

    /**
     * Returns the date of creation of the massage
     * @return the date of creation of the massage
     */
    public long getDate() { return date; }
    /**
     * Sets  the date of creation of the massage
     * @param d  the date of creation of the massage
     */
    public void setDate(long d) { date = d; }

    /**
     * Gets the serialized BC as a byte array (empty array if there is no BC)
     * @return the serialized BC as a byte array
     */
    public byte[] getSerializedObject() { return serializedObject; }

    /**
     * Sets the byte array containing a serialized BC
     * @param code the byte array containing a serialized BC
     */
    public void setSerializedObject(byte[] code) { serializedObject = code; }
}
