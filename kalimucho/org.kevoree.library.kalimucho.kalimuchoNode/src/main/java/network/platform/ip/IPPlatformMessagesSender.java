package network.platform.ip;

import model.interfaces.network.INetworkTraffic;
import network.platform.NetworkPlatformMessage;
import model.interfaces.network.INetworkSender;
import util.Parameters;
import java.util.Vector;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.net.UnknownHostException;
import java.net.SocketException;
import util.NetworkAddress;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.servicesregister.ServicesRegister;
import network.platform.PingService;
import platform.context.ContextManager;
import platform.context.ContextInformation;
import util.SizeCountOutputStream;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import java.net.InetSocketAddress;

/**
 * Thread that sends messages for the PF on IP
 *
 * @author Dalmau
 */

// Classe du thread qui gere les emission reseau IP faites par la PF

public class IPPlatformMessagesSender extends Thread implements INetworkSender, INetworkTraffic {

     // messages du superviseur
    private Vector<NetworkPlatformMessage> messagesDePingEnAttente, messagesEnAttente, messagesUrgentsEnAttente, messagesLentsEnAttente;
    private boolean enMarche, arrete, termine;
    private NetworkAddress monAdresse;
    private ContextManager gestionnaireContexte;
    private int sendedData;

    /**
     * Creates the thread that sends messages for the PF
     * @param adr address to which this thread is associated
     */
    public IPPlatformMessagesSender(NetworkAddress adr) {
        monAdresse = adr;
        messagesEnAttente = new Vector<NetworkPlatformMessage>();
        messagesUrgentsEnAttente = new Vector<NetworkPlatformMessage>();
        messagesLentsEnAttente = new Vector<NetworkPlatformMessage>();
        messagesDePingEnAttente = new Vector<NetworkPlatformMessage>();
        enMarche = true;
        arrete = false;
        sendedData = 0;
        gestionnaireContexte = (ContextManager)ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        setPriority(Thread.NORM_PRIORITY+2);
        start();
    }

    /**
     * Returns the local host address on the network on which this sender works
     * @return the local host address on the network on which this sender works
     */
    public NetworkAddress getSenderAddress() { return monAdresse; }

    /**
     * Post a normal message to be sent
     * @param tr message to be sent
     */
    public synchronized void posterMessage(NetworkPlatformMessage tr) {
        // Methode utilisee par la PF pour envoyer un message par IP
        messagesEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }
    /**
     * Post an urgent message to be sent
     * @param tr message to be sent
     */
    public synchronized void posterMessageUrgent(NetworkPlatformMessage tr) {
        // Methode utilisee par la PF pour envoyer un message par IP
        messagesUrgentsEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }
    /**
     * Post a slow message to be sent
     * @param tr message to be sent
     */
    public synchronized void posterMessageLent(NetworkPlatformMessage tr) {
        // Methode utilisee par la PF pour envoyer un message par IP
        messagesLentsEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }
    /**
     * Post a PING message to be sent
     * @param tr message to be sent
     */
    public synchronized void posterMessagePING(NetworkPlatformMessage tr) {
        // Methode utilisee par la PF pour envoyer un message par IP
        messagesDePingEnAttente.addElement(tr.clone());
        notifyAll(); // debloquer le thread d'envoi de messages
    }

    /**
     * Sends a normal incomplete message. This message is put in a queue and sent by the sender's thread.
     * This message is completed by the address of the sending host on this network
     * @param tr message to complete and to send
     */
    public synchronized void posterMessageIncomplet(NetworkPlatformMessage tr) {
        NetworkPlatformMessage env = tr.clone();
        env.addContent(env.getContent()+monAdresse.getNormalizedAddress());
        messagesEnAttente.addElement(env);
        notifyAll(); // debloquer le thread d'envoi de messages
    }

    // Methode semaphore de traitement des demandes d'emission de messages par la PF
    private synchronized void traiterDemandes() {
        while ((messagesEnAttente.size() == 0) && ((messagesUrgentsEnAttente.size() == 0)) && ((messagesDePingEnAttente.size() == 0))) {
            try {
                wait(); // si pas de message se bloquer
                if ((messagesEnAttente.size() == 0) && (messagesUrgentsEnAttente.size() == 0) && (messagesLentsEnAttente.size() == 0) && (messagesDePingEnAttente.size() == 0) && arrete) {
                    enMarche = false;
                    return;
                }
            } 
            catch (InterruptedException ie) { }
        }
        // on a un message a envoyer (deblocage du thread)
        NetworkPlatformMessage envoi;
        boolean isPing = false;
        if (messagesDePingEnAttente.size() >0) {
            isPing = true;
            envoi = messagesDePingEnAttente.elementAt(0);
            messagesDePingEnAttente.removeElementAt(0); // enlever le message
        }
        else {
            if (messagesUrgentsEnAttente.size() >0) {
                envoi = messagesUrgentsEnAttente.elementAt(0);
                messagesUrgentsEnAttente.removeElementAt(0); // enlever le message
            }
            else {
                if (messagesEnAttente.size() >0) {
                    envoi = messagesEnAttente.elementAt(0);
                    messagesEnAttente.removeElementAt(0); // enlever le message
                }
                else {
                    envoi = messagesLentsEnAttente.elementAt(0);
                    messagesLentsEnAttente.removeElementAt(0); // enlever le message
                }
            }
        }
//System.out.println(" a envoyer pour "+tr.getOwner()+" au port : "+tr.getPortNumber()+" reonse au port :"+tr.getReplyPort()) ;
        if (envoi.getPortNumber() == 0) envoi.setPortNumber(Parameters.PORT_IP_COMMANDS_PF);
        int numPort = envoi.getPortNumber();
        if (envoi.getFinalPort() == 0) envoi.setFinalPort(Parameters.PORT_IP_COMMANDS_PF);
        String adresseIP = envoi.getAddress();
        int compteEssaiRestants = 3;
        Socket env = null;
        while (compteEssaiRestants > 0) {
             try { // essayer d'etablir la connexion IP
                env = new Socket();
                env.connect(new InetSocketAddress(adresseIP, numPort), Parameters.IP_SOCKET_CONNEXION_TIME_OUT);
                try { env.setTcpNoDelay(true); }
                catch (SocketException ie) {
                    System.err.println("Can't put socket in TCP no delay mode");
                }
                // creation du flux d'objets pour emettre par reseau
                ObjectOutputStream ecrire = new ObjectOutputStream(env.getOutputStream());
                if (envoi.getExpeditorAddress().equals("")) envoi.setExpeditorAddress(monAdresse.getNormalizedAddress());
                if (envoi.getExpeditorPort() == 0) envoi.setExpeditorPort(Parameters.PORT_IP_COMMANDS_PF);
                sendedData = sendedData+getMessageSize(envoi);
                ecrire.writeObject(envoi);
                ecrire.flush(); ecrire.close();
                env.close();
                compteEssaiRestants = -1;
            }
            catch (SecurityException e) {
                compteEssaiRestants = 0;
                if (!arrete) {
                    try {
                        KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                        dns.removeReference(new NetworkAddress(adresseIP));
                    }
                    catch (ServiceClosedException sce) { }
                    gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: security"));
                }
            }
            catch (SocketException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete || isPing) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(new NetworkAddress(adresseIP));
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: socket"));
                    }
                }
            }
            catch (NotSerializableException e) {
                compteEssaiRestants = 0;
                if (!arrete) gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: not serializable class"));
            }
            catch (UnknownHostException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete || isPing) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(new NetworkAddress(adresseIP));
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: unknown host"));
                    }
                }
            }
            catch (InvalidClassException e) {
                compteEssaiRestants = 0;
                if (!arrete) gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: invalid class"));
            }
            catch (IOException e) {
                if (env !=  null) try { env.close(); } catch (IOException sce) {}
                if (arrete || isPing) compteEssaiRestants =0;
                else {
                    compteEssaiRestants--;
                    if (compteEssaiRestants == 0) {
                        try {
                            KalimuchoDNS dns = (KalimuchoDNS)ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                            dns.removeReference(new NetworkAddress(adresseIP));
                        }
                        catch (ServiceClosedException sce) { }
                        gestionnaireContexte.signalEvent(new ContextInformation("Error when sending PF message: IO"));
                    }
                }
            }
        }
        if ((compteEssaiRestants == 0) && isPing) { // le ping a echoue => debloquer l'attente
            try {
                PingService pingService = (PingService) ServicesRegister.lookForService(Parameters.NETWORK_PING_SERVICE);
                pingService.abandonner();
            }
            catch (ServiceClosedException sce) {}
        }
    }

    /**
     * Sends the messages for the PF
     */
    @Override
    public void run() {
        // boucle de traitement des messages a envoyer
        termine = false;
        while (enMarche) {
            if (!arrete) traiterDemandes();
            else {
                while ((messagesEnAttente.size() != 0) || (messagesUrgentsEnAttente.size() != 0)) traiterDemandes();
                enMarche = false;
            }
        }
        terminer();
    }

    private synchronized void terminer() {
        termine = true;
        notifyAll();
    }

    /**
     * Stops this thread. It is necessary to wait until all pending messages heve beedn sent
     * before stopping this thread.
     */
    public synchronized void stopThread() {
        arrete = true;
        notifyAll();
    }

    /**
     * Waits until there is no more messages to send
     */
    public synchronized void waitForBufferEmpty() {
        while (!termine) {
            try { wait(); }
            catch (InterruptedException ie) {}
        }
    }

    /**
     * Returns  the number of bytes sent on network by the PF since the last call of this method.
     * @return  the number of bytes sent on network by the PF since the last call of this method.
     */
    public int getDataSize() {
        int ret = sendedData;
        sendedData = 0;
        return ret;
    }

    private int getMessageSize(NetworkPlatformMessage s) {
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
