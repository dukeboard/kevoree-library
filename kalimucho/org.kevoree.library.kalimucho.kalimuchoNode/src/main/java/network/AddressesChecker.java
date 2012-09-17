package network;

import util.NetworkAddress;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
// pour le bluetooth avec la bibliotheque bluecove
//import javax.bluetooth.LocalDevice;
//import javax.bluetooth.BluetoothStateException;

/**
 * This class is used by the platform to find all the addresses of the host (on all accessible networks)
 * It also define a unique identifier for the host (mac address)
 *
 * @author Dalmau
 */
public class AddressesChecker {

    private Vector<NetworkAddress> mesAdresses; // toutes les adresses de cet hote
    private boolean avecBluetooth = false;
    private String hostMacAddress;

    /**
     * Find all the addresses of the host (on all accessible networks)
     * Also find an unique identifier for the host (mac address)
     */
    public AddressesChecker() {
        try {
            NetworkInterface neti = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = neti.getHardwareAddress();
            StringBuffer sb = new StringBuffer();
            if (mac != null) {
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                }
            }
            hostMacAddress = sb.toString();
        } catch (UnknownHostException e) {
            hostMacAddress = "unknown";
            System.err.println("Network addresses checker Service : can't get my host mac address");
        } catch (SocketException e) {
            hostMacAddress = "unknown";
            System.err.println("Network addresses checker Service : can't get my host mac address");
        }

        mesAdresses = new Vector<NetworkAddress>();
/*
        //Methode de decouverte de l'adresse IP ne decouvrant qu'une seule adresse
        // remplacee par une methode trouvant toutes les adresses (voir plus bas)
        try { // recherche de l'adresse IP de la machine ne traite pas les adresses multiples
            monAdresseIP = new NetworkAddress(InetAddress.getLocalHost().getHostAddress().toString());
            ajouteAdresse(new NetworkAddress(monAdresseIP));
        }
        catch (UnknownHostException uh) {
            System.err.println("Network addresses checker Service : can't get my IP address");
        }
*/
        // A utiliser si la machine peut avoir plusieurs adresses IP
        try { // recherche des adresses IP de la machine par getAllByName
            InetAddress[] allMyIps = InetAddress.getAllByName(InetAddress.getLocalHost().getCanonicalHostName());
            if (allMyIps != null && allMyIps.length > 1) {
                for (int i = 0; i < allMyIps.length; i++) {
                    ajouteAdresse(new NetworkAddress(allMyIps[i].getHostAddress().toString()));
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Network addresses checker Service : can't get my host name");
        }
        try { // recherche des adresses IP de la machine par interfaces reseau
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
//System.out.println("carte : "+intf.getDisplayName());
                if (intf.isUp()) { // si l'interface est activee on recupere ses adresses
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress courant = enumIpAddr.nextElement();
//System.out.println("adresse : "+courant.getHostAddress());
                        ajouteAdresse(new NetworkAddress(courant.getHostAddress().toString()));
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Network addresses checker Service : can't get network interfaces");
        }

        // Reseau bluetooth
/*        if (avecBluetooth) {
            try {
                String adr = LocalDevice.getLocalDevice().getBluetoothAddress();
                ajouteAdresse(new NetworkAddress(adr));
            }
            catch (BluetoothStateException bse) {
                System.err.println("Erreur d'acces au bluetooth");
            }
            catch (IOException bse) {
                System.err.println("Erreur d'acces au bluetooth");
            }
        }
*/
        // faire de meme pour les autres types de reseau accessibles

        try { // enregistrer ce service
            ServicesRegisterManager.registerService(Parameters.NETWORK_ADDRESSES, this);
        } catch (ServiceInUseException siue) {
            System.err.println("Network addresses checker service created twice");
        }
        System.out.println("Host identifier: " + hostMacAddress);
    }

    /**
     * Returns a vector with all the addresses of the host (on all accessible networks)
     *
     * @return all the addresses of the host (on all accessible networks)
     */
    public Vector<NetworkAddress> getAllAddresses() {
        return mesAdresses;
    }

    /**
     * Gets all host's addresses of a given type
     *
     * @param type The type of addresses to find (network type IPV4 ...)
     * @return a Vector containing all host's addresses of the given type
     */
    public Vector<NetworkAddress> getAllMyAddresses(int type) {
        Vector<NetworkAddress> retour = new Vector<NetworkAddress>();
        for (int i = 0; i < mesAdresses.size(); i++) {
            if (mesAdresses.elementAt(i).getType() == type) retour.addElement(mesAdresses.elementAt(i));
        }
        return retour;
    }

    /**
     * Gets the first host's addresses of a given type of network
     *
     * @param type type of network (IPV4 ...)
     * @return the first host's addresses of the given type
     */
    public NetworkAddress getMyFirstAddress(int type) {
        NetworkAddress retour = null;
        int i = 0;
        boolean trouve = false;
        while ((i < mesAdresses.size()) && (!trouve)) {
            if (mesAdresses.elementAt(i).getType() == type) {
                trouve = true;
                retour = mesAdresses.elementAt(i);
            } else i++;
        }
        return retour;
    }

    /**
     * Indicate if an address is one of the actual host's addresses
     *
     * @param adr adress to check
     * @return true if this address is on of the actual host's addresses
     */
    public boolean isPresentAddress(NetworkAddress adr) {
        switch (adr.getType()) { // traiter les adresses speciale "localhost"
            case NetworkAddress.TYPE_IPV4:
                if (adr.equals(new NetworkAddress("127.0.0.1"))) return true;
            case NetworkAddress.TYPE_IPV6:
                if (adr.equals(new NetworkAddress("0:0:0:0:0:0:0:1"))) return true;
        }
        // rechercher l'adresse parmi celles de l'hote
        int i = 0;
        boolean trouve = false;
        while ((i < mesAdresses.size()) && (!trouve)) {
            if (mesAdresses.elementAt(i).equals(adr)) trouve = true;
            else i++;
        }
        return trouve;
    }

    /**
     * Returns the unique identifier associated to the local host (normally a MAC address)
     *
     * @return the unique identifier associated to the local host
     */
    public String getHostIdentifier() {
        return hostMacAddress;
    }

    // ajoute une adresse a la liste en evitant les doublons
    // et en verifiant la validite des adresses IP pour creer des connexions
    private void ajouteAdresse(NetworkAddress adr) {
//System.out.println("trouve "+adr.getNormalizedAddress());
        if (exclusions(adr)) return;
        // recherche des doublons
        int i = 0;
        boolean trouve = false;
        while ((i < mesAdresses.size()) && (!trouve)) {
            if (mesAdresses.elementAt(i).equals(adr)) trouve = true;
            else i++;
        }
        if (!trouve) { // cette adresse n'est pas deja enregistree
            if (adr.isIPv4()) {
                if (!isValidIPAddress(adr.getNormalizedAddress()))
                    return; // si c'est une adresse IP verifier qu'elle soir utilisable
            }
            mesAdresses.addElement(adr);
            System.out.println("Found local address : " + adr.getNormalizedAddress());
        }
    }

    private boolean isValidIPAddress(String adr) { // teste si une adresse IP est utilisable pour creer une socket
        try {
            ServerSocket essai = new ServerSocket();
            essai.bind(new InetSocketAddress(adr, Parameters.PORT_IP_ACK_CONNECTORS));
            essai.close();
            return true;
        } catch (Exception e) { // pas d'ouverture de socket possible sur cette adresse
            return false;
        }
    }

    // traitement des adresses a exclure
    private boolean exclusions(NetworkAddress adr) {
        if (adr.equals(new NetworkAddress("127.0.0.1"))) return true; // local host en IPV4
        // Decommenter la ligne suivante pour eviter de lancer des serveurs sur la carte de bouclage
        if (adr.equals(new NetworkAddress("222.222.222.222"))) return true; // adresse de bouclage a l'IUT
        if (adr.equals(new NetworkAddress("0:0:0:0:0:0:0:1"))) return true; // adresse de bouclage en IPV6
        return false;
    }

}
