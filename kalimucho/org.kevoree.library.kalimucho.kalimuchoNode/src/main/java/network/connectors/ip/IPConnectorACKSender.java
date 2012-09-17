package network.connectors.ip;

import java.net.Socket;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import model.interfaces.network.IConnectorACKSender;
import util.Parameters;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import network.AddressesChecker;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import util.NetworkAddress;

/**
 * Sends acknowledges for connectors on IP
 * @author Dalmau
 */
public class IPConnectorACKSender implements IConnectorACKSender {

    private Socket pourAck;
    private ObjectOutputStream ecrire;
    private String nomConnecteur;
    private ContextManager gestionnaireContexte;
    private String myID;

    /**
     * Creates an acknowledges sender for a connector on IP
     * @param dialogue the socket to send acknowledges
     * @param connecteur the name of the connector
     */
    public IPConnectorACKSender(Socket dialogue, String connecteur) {
        nomConnecteur = connecteur;
        AddressesChecker addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        myID = addressesChecker.getHostIdentifier();
        try { // creation de la socket pour acquiter ces donnees
             pourAck =  new Socket(dialogue.getInetAddress().getHostAddress(), Parameters.PORT_IP_ACK_CONNECTORS);
             pourAck.setKeepAlive(true);
             pourAck.setTcpNoDelay(true);
             ecrire = new ObjectOutputStream(pourAck.getOutputStream());
        }
        catch (IOException e) {
            System.err.println("Error creating ACK IP connection for: "+nomConnecteur);
            e.printStackTrace();
        }
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
    }

    /**
     * Send ans acknowledge message for a connector on IP
     */
    public void sendACK() {
            // renvoyer l'acquitement de cet echantillon
        int compteEssaiRestants = 3;
        while (compteEssaiRestants != 0) {
            try { // essayer d'etablir la connexion IP
                ecrire.reset(); // indispensable pour que java libere la memoire occupee par les caches du flux
                ecrire.writeObject(nomConnecteur+";"+myID);
                ecrire.flush();
                compteEssaiRestants = 0;
            }
            catch (SocketException se) {
                try {
                    KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    dns.removeReference(new NetworkAddress(pourAck.getInetAddress().getHostAddress()));
                }
                catch (ServiceClosedException sce) { }
                compteEssaiRestants = 0;
                // Cette exception n'est pas une erreur elle se produit quand l'autre extremite du connecteur s'arrete
            }
            catch (InvalidClassException ice) {
                compteEssaiRestants = 0;
               gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP ACK message: invalid class"));
            }
            catch (NotSerializableException ice) {
                compteEssaiRestants = 0;
               gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP ACK message: not serializable class"));
            }
            catch (IOException e) {
                compteEssaiRestants--;
                if (compteEssaiRestants == 0) {
                    try {
                        KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                        dns.removeReference(new NetworkAddress(pourAck.getInetAddress().getHostAddress()));
                    }
                    catch (ServiceClosedException sce) { }
                   gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP ACK message: IO"));
                }
            }
        }
    }

    /**
     * Sends a NACK message if a reception error as occured when reading data (not used for the moment).
     */
    public void sendNACK() {
            // renvoyer un  non acquitement de cet echantillon
        try { // essayer d'etablir la connexion IP
            ecrire.reset(); // indispensable pour que java libere la memoire occupee par les caches du flux
            ecrire.writeObject("!"+nomConnecteur+";"+myID);
            ecrire.flush();
        }
        catch (SocketException se) {
            try {
                KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                dns.removeReference(new NetworkAddress(pourAck.getInetAddress().getHostAddress()));
            }
            catch (ServiceClosedException sce) { }
            // Cette exception n'est pas une erreur elle se produit quand l'autre extremite du connecteur s'arrete
        }
        catch (InvalidClassException ice) {
           gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP NACK message: invalid class"));
        }
        catch (NotSerializableException ice) {
           gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP NACK message: not serializable class"));
        }
        catch (IOException e) {
            try {
                KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                dns.removeReference(new NetworkAddress(pourAck.getInetAddress().getHostAddress()));
            }
            catch (ServiceClosedException sce) { }
           gestionnaireContexte.signalEvent(new ContextInformation(nomConnecteur+" error when sending IP NACK message: IO"));
        }
    }

    /**
     * Stops the acknoledges sender (close the connexion on IP)
     */
    public void close() {
        try {
            ecrire.close();
            pourAck.close();
        }
        catch (SocketException se) {
            // Cette exception n'est pas une erreur elle se produit quand l'autre extremite du connecteur s'arrete
        }
        catch (IOException ie) {
            System.err.println("Can't close connexion for ACK");
        }
    }

}
