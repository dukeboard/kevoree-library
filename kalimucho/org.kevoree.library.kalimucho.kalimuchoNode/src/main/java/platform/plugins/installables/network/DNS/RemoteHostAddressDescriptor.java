package platform.plugins.installables.network.DNS;

import java.io.Serializable;

/**
 * This class holds information on addresses of hosts stored into the DNS.
 * 
 * @author Dalmau
 */

public class RemoteHostAddressDescriptor  implements Serializable {

    private static final long serialVersionUID = 64240040403010002L; // pour serialisation
    private boolean ajoute; // l'hote est connu directement ou par mise a jour de DNS
    private boolean direct; // le lien par cette adresse est direct ou non

    /**
     * Creates information for DNS host's address
     * @param dir true if this address gives a dirfect acces to the host
     * @param ajout true if this address had been added from a received DNS
     * (that means that we can't know if this address can be used by the local host)
     */
    public RemoteHostAddressDescriptor(boolean dir, boolean ajout) {
        ajoute = ajout;
        if (ajoute) direct = false;
        else direct = dir;
    }

    /**
     * Sets the direct acces information
     * @param dir the direct acces information
     */
    public void setDirect(boolean dir) {
        direct = dir;
    }

    /**
     * Returns true if the address gives a direct access to the host on network.
     * For addresses added from received DNS always return false.
     * @return true if the address gives a direct access to the host on network
     */
    public boolean isDirect() { return direct; }

    /**
     * Returns true if the address had been added from a received DNS
     * @return true if the address had been added from a received DNS
     */
    public boolean isAdded() { return ajoute; }

}


