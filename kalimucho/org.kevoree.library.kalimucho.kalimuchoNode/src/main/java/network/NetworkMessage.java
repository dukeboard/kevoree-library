package network;

import java.io.Serializable;

/**
 * Serializable message containing:
 * the address and port number to send this message to
 * the address and port number of the final addressee
 * the address and port number of the expeditor of this message.<br>
 * This class is used to create all the messages classes for network PF communication.
 *
 * @author ccasagn modifie par Dalmau
 */

/* 
 * Classe generique pour creer des messages pouvant etre envoyes ou recus par reseau par la PF.
 */

public abstract class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 64240030000000001L; // pour serialisation
    /**
     * Address to send this message to
     */
    transient protected String destinationAddress = null;      // @ ou envoyer ce message
    /**
     * Port number to send this message to
     */
    transient protected int destinationPort; // numero de port ou envoyer ce message
    /**
     * Address of the final addressee.
     * When a message is send to a PF which is not the final addresse this value is used to relay the message.
     * When a message is send to a PF which is the final addresse this value is "local"
     */
    protected String finalAddress; // adresse du destinataire final ou "local" si le message ne doit pas etre relaye
    /**
     * Port to send to the final addressee.
     * If this value is 0, the regular port number of the PF will by set before sending the message
     */
    protected int finalPort; // numero de port du destinataire final ou 0 si le message ne doit pas etre relaye
    /**
     * Address of the initial sender of this message
     * (can be different from the host from which this message comes when the message is relayed).
     * If this value is set to "" the real address of the host will be set before sending the message
     */
    protected String expeditorAddress; // adresse de l'expediteur initial du message
    /**
     * Port nulber to use to reply to the initial sender of this message
     * (can be different from the port number to use to reply to the host from which this message comes when the message is relayed).
     * If this value is 0, the regular port number of the PF will by set before sending the message
     */
    protected int expeditorPort; // numero de port pour la reponse a l'expediteur initial du message

    /**
     * Unique identifier of the host that sends this message
     */
    protected String senderID;
    /**
     * Unique identifier of the host that has created this message
     */
    protected String expeditorID;
	
    /**
     * Construction without parameters necessary to de-serialize
     */
    public NetworkMessage() {
        // constructeur sans parametre necessaire pour pouvoir deserialiser
            destinationAddress = "";
            destinationPort = 0;
            finalAddress = "local";
            finalPort = 0;
            expeditorAddress = "";
            expeditorPort = 0;
            senderID = "";
            expeditorID = "";
 	}
	
    /**
     * Construction of a network message
     *
     * @param addr Address where to send this message
     */
    public NetworkMessage(String addr) {
            destinationAddress = addr;
            destinationPort = 0;
            finalAddress = "local";
            finalPort = 0;
            expeditorAddress = "";
            expeditorPort = 0;
            senderID = "";
            expeditorID = "";
 	}

    /**
     * Returns the address of the sender of this message
     * @return the address of the sender of this message
     */
    public String getExpeditorAddress() {
            return expeditorAddress;
    }

    /**
     * Sets the initial expeditor address
     * @param exp initial expeditor address
     */
    public void setExpeditorAddress(String exp) {
        expeditorAddress = exp;
    }

    /**
     * Sets the intial expeditor address as to be defined when sending the message
     */
    public void setExpeditorAdressWhenSending() {
        expeditorAddress = "";
    }
    
    /**
     * Returns the address to send this message to
     * @return the address to send this message to
     */
    public String getAddress() {
        return destinationAddress;
    }
    /**
     * Sets the address
     * @param addr address
     */
    public void setAddress(String addr) { destinationAddress = addr; }

    /**
     * Gets the address of the final addresse used when message is relayed on the route
     * @return the address of the final addresse
     */
    public String getFinalAddress() {
        return finalAddress;
    }
    /**
     * Sets the address of the final addresse used when message is relayed on the route
     * @param addr the address of the final addresse
     */
    public void setFinalAddress(String addr) { finalAddress = addr; }
    
    /**
     * Gets the port number to send this message to
     * @return port number to send this message to
     */
    public int getPortNumber() {
        return destinationPort;
    }
    /**
     * Sets the port number to send this message to
     * @param p port number to send this message to
     */
    public void setPortNumber(int p) { destinationPort = p; }

    /**
     * Get the port number to use to reply to this message
     *
     * @return Port number to use to reply to this message
     */
    public int getExpeditorPort() {
        return expeditorPort;
    }
    /**
     * Sets the port number used to reply to this message
     * @param p the port number used to reply to this message
     */
    public void setExpeditorPort(int p) {
        expeditorPort = p;
    }

    /**
     * Returns the port of the final addressee of the message
     * @return the port of the final addressee of the message
     */
    public int getFinalPort() {
        return finalPort;
    }

    /**
     *  Sets the port of the final addressee of the message
     * @param p  the port of the final addressee of the message
     */
    public void setFinalPort(int p) {
        finalPort = p;
    }

    /**
     * Indicate if the message needs to be relayed
     * @return true the message needs to be relayed
     */
    public boolean isLocal() {
        if (finalAddress.equals("local")) return true;
        else return false;
    }

    /**
     * Returns the unique identifier of the host that has sent this message
     * @return the unique identifier of the host that has sent this message
     */
    public String getSenderID() { return senderID; }

    /**
     * Sets the unique identifier of the host that sends this message
     * @param id the unique identifier of the host that sends this message
     */
    public void setSenderID(String id) { senderID = id; }

    /**
     * Returns the unique identifier of the host that has created this message
     * @return the unique identifier of the host that has created this message
     */
    public String getExpeditorID() { return expeditorID; }

    /**
     * Sets the unique identifier of the host that creates this message
     * @param id the unique identifier of the host that creates this message
     */
    public void setExpeditorID(String id) { expeditorID = id; }

}
