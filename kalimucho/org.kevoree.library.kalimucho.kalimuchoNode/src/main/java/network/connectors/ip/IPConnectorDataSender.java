package network.connectors.ip;

import model.interfaces.network.INetworkTraffic;
import network.connectors.DataAcquitmentManager;
import network.connectors.DataAcquitmentSemaphore;
import util.Parameters;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import network.connectors.EncapsulatedSample;
import java.io.ObjectOutputStream;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import java.net.Socket;
import java.net.SocketException;
import model.interfaces.network.IConnectorDataSender;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.SizeCountOutputStream;
import network.AddressesChecker;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import util.NetworkAddress;

/**
 * Sends data for on aconnector on IP
 * @author Dalmau
 */
public class IPConnectorDataSender implements IConnectorDataSender, INetworkTraffic {

    private DataAcquitmentManager listeAcquitements;
    private Socket env;
    private ObjectOutputStream ecrire;
    private String nom;
    private ContextManager gestionnaireContexte;
    private int sendedData;

    /**
     * Creates a data sender on IP for a connector
     * @param adresseIP address of the addressee (other part of this connector) on IP
     */
    public IPConnectorDataSender(NetworkAddress adresseIP) {
        try {
            env = new Socket(adresseIP.getNormalizedAddress(), Parameters.PORT_IP_DATA_CONNECTORS);
            env.setTcpNoDelay(true);
            env.setKeepAlive(true);
            ecrire = new ObjectOutputStream(env.getOutputStream());
        }
        catch (SocketException ie) {
            System.err.println("Can't put socket in TCP no delay mode");
        }
        catch (IOException e) {
            System.err.println("Error creating IP connection for: "+nom);
        }
        sendedData = 0;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        listeAcquitements = (DataAcquitmentManager)ServicesRegisterManager.platformWaitForService(Parameters.CONNECTORS_ACKNOWLEDGE);
    }

    /**
     * Sends a message
     * @param connector connector sending the message
     * @param contenu sample sent
     */
    public void sendMessage(String connector, EncapsulatedSample contenu) {
        nom = connector;
        DataAcquitmentSemaphore connexion = listeAcquitements.findConnexionDesignation(connector);
        connexion.NotAcquited();
        int compteEssaiRestants = 3;
        while (compteEssaiRestants != 0) {
            try { // essayer d'etablir la connexion IP
                ecrire.reset(); // indispensable pour que java libere la memoire occupee par les caches du flux
                if (contenu != null) { // message normal
    //                sendedData = sendedData+getSampleSize(contenu);
                    sendedData = sendedData+contenu.size()+EncapsulatedSample.EXTRA_SIZE;
                    ecrire.writeObject(contenu);
                    compteEssaiRestants = 0;
                }
                else { // message de synchro
                    AddressesChecker addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
                    String myID = addressesChecker.getHostIdentifier();
                    ecrire.writeObject(connector+";"+myID);
                    compteEssaiRestants = 0;
                }
                ecrire.flush();
            }
            catch (InvalidClassException ice) {
                compteEssaiRestants = 0;
                gestionnaireContexte.signalEvent(new ContextInformation(nom+": error when sending data on IP: invalid class"));
            }
            catch (NotSerializableException ice) {
                compteEssaiRestants = 0;
                gestionnaireContexte.signalEvent(new ContextInformation(nom+": error when sending data on IP: not serializable class"));
            }
            catch (SocketException se) {
                try {
                    KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    dns.removeReference(new NetworkAddress(env.getInetAddress().getHostAddress()));
                }
                catch (ServiceClosedException sce) { }
                compteEssaiRestants = 0;
                // Cette exception n'est pas une erreur elle se produit quand l'autre extremite du connecteur s'arrete
            }
            catch (IOException e) {
                compteEssaiRestants--;
                if (compteEssaiRestants == 0) {
                    try {
                        KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                        dns.removeReference(new NetworkAddress(env.getInetAddress().getHostAddress()));
                    }
                    catch (ServiceClosedException sce) { }
                    gestionnaireContexte.signalEvent(new ContextInformation(nom+": error when sending data on IP: IO"));
                }
            }
        }
    }

    /**
     * Stops the data sender on IP (closes the connexion)
     */
    public void close() {
        try {
            ecrire.close();
            env.close();
        }
        catch (SocketException se) {
            // Cette exception n'est pas une erreur elle se produit quand l'autre extremite du connecteur s'arrete
        }
        catch (IOException e) {
            System.err.println("Error closing IP connection for: "+nom);
        }
    }
    
    /**
     * Returns  the number of bytes sent on network by this connector's data sender since the last call of this method.
     * @return  the number of bytes sent on network by this connector's data sender since the last call of this method.
     */
    public int getDataSize() {
        int ret = sendedData;
        sendedData = 0;
        return ret;
    }

    private int getSampleSize(EncapsulatedSample s) {
        int taille;
        SizeCountOutputStream bos = new SizeCountOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(s);
            taille = bos.getSize();
        }
        catch (IOException ioe) {
            taille = 0;
        }
        return taille;
    }

}
