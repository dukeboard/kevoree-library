package platform.context.status;

import java.util.Vector;
import platform.servicesregister.ServicesRegisterManager;
import network.AddressesChecker;
import platform.context.ContextManager;
import util.Parameters;
import util.NetworkAddress;
import platform.containersregister.ContainersManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * This class describes a complete status of a host. That means:
 * - unique identifier of the host
 * - addresses on all network for this host
 * - actual CPU load
 * - actual free memory amount
 * - actual battery level
 * - actual number of running threads
 * - actual number of running BCs
 * - actual number of connectors
 * - actual number of connectors whose input comes from network
 * - actual number of connectors whose output goes to network
 * - actual network input trafic due to PF activity
 * - actual network output trafic due to PF activity
 * - actual network input trafic due to application activity
 * - actual network output trafic due to application activity
 * All these informations are updated when the object is created.
 *
 * @author Dalmau
 */
public class HostStatus {

    // Valeurs statiques
    private String hostID;
    private Vector<NetworkAddress> hostAddresses;
    // Valeurs dynamiques
    private int cpuLoad;
    private int memoryLoad;
    private int batteryLevel;
    private int numberOfThreads;
    private int numberOfBCs;
    private int numberOfConnectors;
    private int numberOfConnectorsNetworkInputs;
    private int numberOfConnectorsNetworkOutputs;
    private int networkPFInputTraffic;
    private int networkPFOutputTraffic;
    private int networkApplicationInputTraffic;
    private int networkApplicationOutputTraffic;

    private transient AddressesChecker addressesChecker;
    private transient ContextManager contextManager;

    /**
     * Creates an host status that holds :
     * - a unique identifier of the host
     * - all addresses on all network for this host
     * - the actual CPU load
     * - the actual free memory amount
     * - the actual battery level
     * - the actual number of running threads
     * - the actual number of running BCs
     * - the actual number of connectors
     * - the actual number of connectors whose input comes from network
     * - the actual number of connectors whose output goes to network
     * - the actual network input trafic due to PF activity
     * - the actual network output trafic due to PF activity
     * - the actual network input trafic due to application activity
     * - the actual network output trafic due to application activity
     * These informations are collected from the context manager when creating the object.
     */
    public HostStatus() {
        addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        hostID = addressesChecker.getHostIdentifier();
        hostAddresses = addressesChecker.getAllAddresses();
        contextManager = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        cpuLoad = contextManager.getCpuLoad();
        memoryLoad = contextManager.getMemoryLoad();
        batteryLevel = contextManager.getBatteryLevel();
        numberOfThreads = contextManager.getNumberOfThreads();
        networkPFInputTraffic = contextManager.getPFInputTrafic();
        networkPFOutputTraffic = contextManager.getPFOutputTrafic();
        networkApplicationInputTraffic = contextManager.getApplicationInputTrafic();
        networkApplicationOutputTraffic = contextManager.getApplicationOutputTrafic();
        ContainersManager gestionnaireDeConteneurs = (ContainersManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTAINERS_MANAGER);
        numberOfBCs = gestionnaireDeConteneurs.getComposants().getComponentsNumber();
        numberOfConnectors = gestionnaireDeConteneurs.getConnecteurs().getConnectorsNumber();
        numberOfConnectorsNetworkInputs = gestionnaireDeConteneurs.getConnecteurs().getNetworkInputConnectorsNumber();
        numberOfConnectorsNetworkOutputs = gestionnaireDeConteneurs.getConnecteurs().getNetworkOutputConnectorsNumber();
    }

    /**
     * Creates an host status from a serialized one. Used when received by network.
     * @param content the serialized host status to create from
     */
    public HostStatus(byte[] content) {
        addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        contextManager = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            hostID = (String)ois.readObject();
            int taille = ((Integer)ois.readObject()).intValue();
            hostAddresses =  new Vector<NetworkAddress>();
            for (int i=0; i<taille; i++) hostAddresses.addElement(new NetworkAddress((String)ois.readObject()));
            cpuLoad = ((Integer)ois.readObject()).intValue();
            memoryLoad = ((Integer)ois.readObject()).intValue();
            batteryLevel = ((Integer)ois.readObject()).intValue();
            numberOfThreads = ((Integer)ois.readObject()).intValue();
            numberOfBCs = ((Integer)ois.readObject()).intValue();
            numberOfConnectors = ((Integer)ois.readObject()).intValue();
            numberOfConnectorsNetworkInputs = ((Integer)ois.readObject()).intValue();
            numberOfConnectorsNetworkOutputs = ((Integer)ois.readObject()).intValue();
            networkPFInputTraffic = ((Integer)ois.readObject()).intValue();
            networkPFOutputTraffic = ((Integer)ois.readObject()).intValue();
            networkApplicationInputTraffic = ((Integer)ois.readObject()).intValue();
            networkApplicationOutputTraffic = ((Integer)ois.readObject()).intValue();
        }
        catch (IOException ioe) {
            System.err.println("Error converting a received host state : IOError");
        }
        catch (ClassNotFoundException ioe) {
            System.err.println("Error converting a received host state : class not found");
        }
    }

    /**
     * Returns the host's unique identifier
     * @return the host's unique identifier
     */
    public String getHostID() { return hostID; }
    /**
     * Returns all the addresses of this host on all available networks
     * @return all the addresses of this host on all available networks
     */
    public Vector<NetworkAddress> getHostAddresses() { return hostAddresses; }
    /**
     * Returns all the addresses of this host on a specified network
     * @param type type of network for which the addresses are to be collected
     * @return all the addresses of this host on the specified network
     */
    public Vector<NetworkAddress> getHostAddresses(int type) {return addressesChecker.getAllMyAddresses(type); }
    /**
     * Returns the CPU load (in %)
     * @return the CPU load (in %)
     */
    public int getCpuLoad() { return cpuLoad; }
    /**
     * Returns the amount of free memory (in %)
     * @return the amount of free memory (in %)
     */
    public int getMemoryLoad() { return memoryLoad; }
    /**
     * Returns the battery level
     * @return the battery level
     */
    public int getBatteryLevel() { return batteryLevel; }
    /**
     * Returns the number of running threads
     * @return the number of running threads
     */
    public int getNumberOfThreads() { return numberOfThreads; }
    /**
     * Returns the number of installed BCs
     * @return the number of installed BCs
     */
    public int getNumberOfBCs() { return numberOfBCs; }
    /**
     * Returns the number of installed connectors
     * @return  the number of installed connectors
     */
    public int getNumberOfConnectors() { return numberOfConnectors; }
    /**
     * Returns the number of installed connectors having their input comming from network
     * @return the number of installed connectors having their input comming from network
     */
    public int getNumberOfConnectorsNetworkInputs() { return numberOfConnectorsNetworkInputs; }
    /**
     * Returns the number of installed connectors having their output going to network
     * @return the number of installed connectors having their output going to network
     */
    public int getNumberOfConnectorsNetworkOutputs() { return numberOfConnectorsNetworkOutputs; }
    /**
     * Returns the network input trafic due to the platform
     * @return the network input trafic due to the platform
     */
    public int getNetworkPFInputTraffic() { return networkPFInputTraffic; }
    /**
     * Returns the network output trafic due to the platform
     * @return the network output trafic due to the platform
     */
    public int getNetworkPFOutputTraffic() { return networkPFOutputTraffic; }
    /**
     * Returns the network input trafic due to the application
     * @return the network input trafic due to the application
     */
    public int getNetworkApplicationInputTraffic() { return networkApplicationInputTraffic; }
    /**
     * Returns the network output trafic due to the application
     * @return the network output trafic due to the application
     */
    public int getNetworkApplicationOutputTraffic() { return networkApplicationOutputTraffic; }

    /**
     * Serialize the object in a byte array
     * @return a byte array containing the serialized object
     */
    public byte[] toByteArray() {
        byte[] retour;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(new String(hostID));
            oos.writeObject(new Integer(hostAddresses.size()));
            for (int i=0; i<hostAddresses.size(); i++) oos.writeObject(new String(hostAddresses.elementAt(i).getNormalizedAddress()));
            oos.writeObject(new Integer(cpuLoad));
            oos.writeObject(new Integer(memoryLoad));
            oos.writeObject(new Integer(batteryLevel));
            oos.writeObject(new Integer(numberOfThreads));
            oos.writeObject(new Integer(numberOfBCs));
            oos.writeObject(new Integer(numberOfConnectors));
            oos.writeObject(new Integer(numberOfConnectorsNetworkInputs));
            oos.writeObject(new Integer(numberOfConnectorsNetworkOutputs));
            oos.writeObject(new Integer(networkPFInputTraffic));
            oos.writeObject(new Integer(networkPFOutputTraffic));
            oos.writeObject(new Integer(networkApplicationInputTraffic));
            oos.writeObject(new Integer(networkApplicationOutputTraffic));
            retour = bos.toByteArray();
        }
        catch (IOException ioe) {
            System.err.println("Error converting a host status to byte array");
            retour =  null;
        }
        return retour;
    }

    /**
     * Converts the object into a readable form.
     * @return a string holding  a readable form of the host status.
     */
    public String toMessage() {
        String mem = "Mem: "+memoryLoad+"%";
        String cpu = " CPU: "+cpuLoad+"%";
        String bat = " Bat: "+batteryLevel+"%";
        String threads = "Threads: "+numberOfThreads;
        String nbCM = "BCs: "+numberOfBCs;
        String nbConn = "Connectors: "+numberOfConnectors;
        String emiss = "Sent PF:appli "+networkPFOutputTraffic+":"+networkApplicationInputTraffic+" KB/s";
        String rec = "Rcvd PF:appli "+networkPFInputTraffic+":"+networkApplicationInputTraffic+" KB/s";
        return mem+"|"+cpu+"|"+threads+"|"+bat+"|"+nbCM+"|"+nbConn+"|"+emiss+"|"+rec; // recuperation de l'etat de l'hote
    }

    /**
     * For test pupose only: dumps the content of the object on console.
     */
    public void dump() {
        System.out.println("ID : "+hostID);
        for (int i=0; i<hostAddresses.size(); i++) System.out.println(" - addresse : "+hostAddresses.elementAt(i).getNormalizedAddress());
        System.out.println("CPU : "+cpuLoad);
        System.out.println("Memoire : "+memoryLoad);
        System.out.println("Batterie : "+batteryLevel);
        System.out.println("Threads : "+numberOfThreads);
        System.out.println("CMs : "+numberOfBCs);
        System.out.println("connecteurs : "+numberOfConnectors);
        System.out.println("connecteurs entree reseau : "+numberOfConnectorsNetworkInputs);
        System.out.println("connecteurs sortie reseau : "+numberOfConnectorsNetworkOutputs);
        System.out.println("Traffic PF entree : "+networkPFInputTraffic);
        System.out.println("Traffic PF sortie : "+networkPFOutputTraffic);
        System.out.println("Traffic application entree : "+networkApplicationInputTraffic);
        System.out.println("Traffic application sortie : "+networkApplicationOutputTraffic);
    }
}
