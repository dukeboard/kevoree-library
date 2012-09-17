package platform.plugins.installables.network.DNS;

import java.util.HashMap;
import util.NetworkAddress;
import util.Parameters;
import java.io.Serializable;
import java.util.Vector;

/**
 * This class is used by the DNS to hold information about hosts.<br>
 * - Hnown addresses descriptors of this host
 * - Clock shift from host to local clock (if available)
 * - Clock accuracy from host to local clock (if available)
 * - Time to live of this host (it will be remove from DNS when this time is 0)
 *
 * @author Dalmau
 */
public class RemoteHostDescriptor implements Serializable {

    private static final long serialVersionUID = 64240040403010001L; // pour serialisation
    private HashMap<String, RemoteHostAddressDescriptor> addresses; // liste d'adresses  + informations d'acces de l'hote
    private long clockShift; // decalage d'horloge avec cet hote
    private long clockShiftAccuracy; // precision de mesure de decalage d'horloge faites pour cet hote
    private long timeToLive; // duree de conservation de cet hote dans le DNS (en s)

    /**
     *
     */
    public RemoteHostDescriptor() {
        addresses = new HashMap<String, RemoteHostAddressDescriptor>();
        clockShift = 0;
        clockShiftAccuracy = -1; // a priori le decalage d'horloge n'est pas connu a la creation
        timeToLive = Parameters.MAXIMAL_TIME_TO_LIVE; // duree de vie maximale a la creation
    }

    // Ajout ou mise a jour d'une adresse de cet hote
    // Si elle n'est pas deja connue on l'ajoute
    // Sinon selon que cet ajout est provoque par la recuperation d'une information locale ou pas
    // le champ indiquant si l'acces par cette adresse est direct ou pas est modifie.
    // En effet, lorsque l'information n'est pas locale ce champ ne peut pas etre exploite
    /**
     * Adds an address to the host. If this address if not yet stored, it is added.
     * Else information on this address is updated (gives a direct access, collected locally or received)
     * @param ajout address to add
     * @param dir this address gives a direct access to the host
     * @param ajoute this address has been collected locally or received
     */
    public synchronized void addAddress(NetworkAddress ajout, boolean dir, boolean ajoute) {
        timeToLive = Parameters.MAXIMAL_TIME_TO_LIVE;
        if (addresses.get(ajout.getNormalizedAddress()) == null) { // on n'avait pas cette adresse => on l'ajoute
            addresses.put(new String(ajout.getNormalizedAddress()), new RemoteHostAddressDescriptor(dir, ajoute));
        }
        else { // on avait deja cette @ => on met a jour l'indicateur de route directe a condition que l'information ne soit pas venue d'un autre DNS
            if ((!ajoute) && dir) addresses.get(ajout.getNormalizedAddress()).setDirect(dir);
        }
    }

    /**
     * Removes a stored address for this host
     * @param adr address to remove
     */
    public synchronized void removeAddress(NetworkAddress adr) {
        addresses.remove(adr.getNormalizedAddress());
    }

    /**
     * Returns true if the specified address is known for this host
     * @param adr address to find
     * @return true if the specified address is known for this host
     */
    public synchronized boolean isPresentAddress(NetworkAddress adr) {
        return (addresses.get(adr.getNormalizedAddress()) != null);
    }

    /**
     * Returns all the stored addresses of the host
     * @return all the stored addresses of the host
     */
    public synchronized HashMap<String, RemoteHostAddressDescriptor> getRemoteHostAddresseDescriptors() { return addresses; }

    /**
     * Returns all the stored addresses of the host that gives a direct access to it
     * @return all the stored addresses of the host that gives a direct access to it
     */
    public Vector<String> getAllDirectAddresses() {
        Vector<String> retour = new Vector<String>();
        for (String adr : addresses.keySet()) {
            if (addresses.get(adr).isDirect()) retour.addElement(adr);
        }
        return retour;
    }

    /**
     * Ajust the clock shift of this host (only if the actually store clock shift is unknown or of less accuracy)
     * @param cs new clock shift
     * @param acc new clock accuracy
     */
    public synchronized void adjustClockShift(long cs, long acc) {
        if ((clockShiftAccuracy == -1) || (clockShiftAccuracy > acc)) {
            clockShiftAccuracy = acc;
            clockShift = cs;
        }
    }
    /**
     * Gets the host's clock shift
     * @return the host's clock shift
     */
    public synchronized long getClockShift() { return clockShift; }
    /**
     * Sets the host's clock shift
     * @param cs new clock shift
     */
    public synchronized void setClockShift(long cs) { clockShift = cs; }
    /**
     * Returns the host's clock accuracy
     * @return the host's clock accuracy
     */
    public synchronized long getClockAccuracy() { return clockShiftAccuracy; }
    /**
     * Sets  the host's clock accuracy
     * @param ca  the new host's clock accuracy
     */
    public synchronized void setClockAccuracy(long ca) { clockShiftAccuracy = ca; }
    /**
     * Returns true if clock shift is available
     * @return true if clock shift is available
     */
    public synchronized boolean isClockShiftAvailable() { return ( clockShiftAccuracy != -1); }
    /**
     * Sets the host's clock shift as not available
     */
    public synchronized void setClockShiftNotAvailable() {
        clockShiftAccuracy = -1;
        clockShift = 0;
    }

    /**
     * Returns the time to live of the host
     * @return the time to live of the host
     */
    public synchronized long getTimeToLive() { return timeToLive; }
    /**
     * Sets  the time to live of the host
     * @param ttl  the new time to live of the host
     */
    public synchronized void setTimeToLive(long ttl) { timeToLive = ttl; }
    /**
     * Decrement the time to live of the host
     * @param decr value to decrease the time to live of the host
     */
    public synchronized void decrementTimeToLive(long decr) { timeToLive = timeToLive-decr; }

}
