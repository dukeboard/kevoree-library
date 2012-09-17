package platform.context;

import model.interfaces.control.IControlUnit;
import platform.servicesregister.ServiceClosedException;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServicesRegisterManager;
import platform.context.hostsurveillance.HostLoadWatcher;
import network.platform.PlatformMessagesEmitter;
import network.platform.PlatformMessagesReceptor;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import util.NetworkAddress;
import network.platform.NetworkPlatformMessage;
import java.util.Timer;
import util.Parameters;
import platform.context.status.HostStatus;

/**
 * Manages all context information of the platform.
 * For the moment only sends alarm to a referent PF (deplyment/reconfiguration module).
 * @author Dalmau
 */
public class ContextManager extends Thread {

    private HostLoadWatcher mesureCharge; // processus lance par timer
            // a intervalles reguliers pour mesurer l'etat de l'hote
            // memoire libre, charge CPU et niveau de batterie
    private Timer surveillance; // timer utilise pour surveiller la charge de l'hote
    private NetworkAddress referent; // adresse de la PF a laquelle signaler les evenements
        // on prendra la derniere PF qui a cree un composant ou un connecteur sur cet hote
        // sauf pour les commandes de creation d'extremite de connecteur
        // car elles ont transmise par l'hote qui a recu la demande de creation
        // et non par une PF.
    private int portReferent; // numero de port par lequel envoyer a la PF referente
    private NetworkAddress relaisReferent; // adresse de l'hote auquel signaler les evenements
        // dans le cas ou l'on n'a pas de PF redferente
    private int portRelaisReferent; // numero de port par lequel envoyer a cette PF
    private PlatformMessagesEmitter emetteurDeMessages;
    private PlatformMessagesReceptor receptionMessages;
    private NetworkEmitterContainer networkEmitterContainer;
    private NetworkReceptorContainer networkReceptorContainer;
    private boolean enMarche;

    /**
     * Collects all context information comming from:
     * connectors
     * BCs
     * Host surveillance
     * Network
     */
    public ContextManager() {
        try { // enregistrer le service de gestion du contexte
            ServicesRegisterManager.registerService(Parameters.CONTEXT_MANAGER, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Context Manager service created twice");
        }
    }

    /**
     * Used to indicate the platform to send context information to
     * @param expediteur address of the platform to send context information to
     * @param portEnvoi port number used to send this context information
     */
    public void setReferentPlatform(String expediteur, int portEnvoi) {
        // Pour le moment on n'accepte comme PF de reference que le module de deploiement/reconfiguration
        if (portEnvoi == 1098) {
            referent = new NetworkAddress(expediteur); // la PF a laquelle signaler les evenements
            portReferent = portEnvoi; // le port sur lequel les envoyer
        }
        else setRelayReferentPlatform(expediteur, portEnvoi);
    }

    /**
     * Used to indicate a platform that can relays context information
     * @param expediteur address of the platform that can relays context information
     * @param portEnvoi port numbber to sent this context information to the relay platform
     */
    public void setRelayReferentPlatform(String expediteur, int portEnvoi) {
        relaisReferent = new NetworkAddress(expediteur);
        portRelaisReferent = portEnvoi; // le port sur lequel les envoyer
    }


    /** 
     * Performs context information.
     * For the moment this information is sent to a referent platform (deployment/reconfiguration module)
     * On ly to be displayed.
     * In later versions this information must be performed in order to decide reconfigurations.
     *
     * @param alarme alarm message to be sent
     */
    public synchronized void signalEvent(ContextInformation alarme) {
        performEvent(alarme);
    }

    /**
     * Returns the QoS indicated by a BC (application or connector BC)
     * @param nom name of the component or connector
     * @return the QoS indicated by the component or connector
     */
    public float getComponentQdS(String nom) {
        // recuperation de la valeur de QdS retournee par un CM (osagaia)
        // ou par un CM de transfert de flux (Korrontea)
        try {
           IControlUnit uc = (IControlUnit)(ServicesRegisterManager.lookForService(nom));
           float rep=uc.sendBCQoSLevel(); // acces au service de l'UC
           return rep; // reponse obtenue
        }
        catch (ServiceClosedException sce) {
            //System.err.println("Lecture QdS "+nom+" : composant inconnu");
            return -2f;
        }
    }

    /**
     * Returns the actual network traffic average for a given connector (in KB/s)
     * @param nom name of the connector
     * @return the actual network traffic average for this connector (in KB/s)
     */
    public int getConnectorTrafficAverage(String nom) {
        try {
           IControlUnit uc = (IControlUnit)(ServicesRegisterManager.lookForService(nom));
           if (uc instanceof model.korrontea.ControlUnit) {
                   return (int)(((model.korrontea.ControlUnit)uc).getDebitMoyen())/Parameters.LOAD_MEASURE_RATE;
            }
           else return 0;
        }
        catch (ServiceClosedException sce) {
            //System.err.println("Lecture QdS "+nom+" : composant inconnu");
            return 0;
        }
    }

    /**
     * Returns the actual CPU load (in %)
     * @return the actual CPU load (in %)
     */
    public int getCpuLoad() { return mesureCharge.getCPULoad(); }
    /**
     * Returns the actual amount of free memory (in %)
     * @return the actual amount of free memory (in %)
     */
    public int getMemoryLoad() { return mesureCharge.getFreeMemory(); }
    /**
     * Returns the actual battery level (in %)
     * @return the actual battery level (in %)
     */
    public int getBatteryLevel() { return mesureCharge.getBatteryLevel(); }
    /**
     * Returns the actual number of running threads
     * @return the actual number of running threads
     */
    public int getNumberOfThreads() { return mesureCharge.getThreadsActifs(); }
    /**
     * Gets the network input trafic for the platform (in KB/s)
     * @return  the network input trafic for the platform (in KB/s)
     */
    public int getPFInputTrafic() { return mesureCharge.getNetworkPlatformReceptions(); }
    /**
     * Gets  the network output trafic for the platform (in KB/s)
     * @return  the network output trafic for the platform (in KB/s)
     */
    public int getPFOutputTrafic() { return mesureCharge.getNetworkPlatformEmissions(); }
    /**
     * GEts the network output trafic for the application ie all connectors (in KB/s)
     * @return the network output trafic for the application ie all connectors (in KB/s)
     */
    public int getApplicationOutputTrafic() { return mesureCharge.getNetworkConnectorsEmissions(); }
    /**
     *  Gets the network input trafic for the application ie all connectors (in KB/s)
     * @return the network input trafic for the application ie all connectors (in KB/s)
     */
    public int getApplicationInputTrafic() { return mesureCharge.getNetworkConnectorsReceptions(); }
    /**
     *  Gets the actual complete host status (see HostStatus for details)
     * @return the actual complete host status
     */
    public HostStatus getHostStatus() { return new HostStatus(); }

    /**
     * Returns the actual host state (CPU, memory, battery, threads)
     * @return the actual host state (CPU, memory, battery, threads)
     */
    public String getHostState() {
        return new HostStatus().toMessage();
    }

    /**
     * Starts the contaext manager: start listening to events (internal and network received)
     */
    public void startThread() {
        // Recherche du service d'envoi de messages de la PF
        networkEmitterContainer = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        emetteurDeMessages = networkEmitterContainer.getPlatformMessagesEmitter();
        networkReceptorContainer = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        receptionMessages = networkReceptorContainer.getPlatformMessagesReceptor();
        receptionMessages.inscription(Parameters.CONTEXT_MANAGER);
        // Lancer la surveillance de l'hote
        mesureCharge = new HostLoadWatcher(this, networkEmitterContainer, networkReceptorContainer);
        surveillance = new Timer();
        surveillance.scheduleAtFixedRate(mesureCharge, Parameters.LOAD_MEASURE_RATE, Parameters.LOAD_MEASURE_RATE);
        start();
    }
    /**
     * Stops the context manager
     */
    public void stopThread() {
        surveillance.cancel();
        mesureCharge.cancel();
        enMarche = false;
        receptionMessages.desinscription(Parameters.CONTEXT_MANAGER);
    }

    /**
     * Waiting for context information comming from networks
     * This information is performed like local one
     */
    @Override
    public void run() {
        enMarche = true;
        while (enMarche) { // La PF peut etre arretee par stopThread
            // traitement des alarmes recues par reseau
            String infoDeContexte = null;
            NetworkPlatformMessage recu=null;
            // Attendre une commande sur le reseau
            recu = receptionMessages.retirerMessage(Parameters.CONTEXT_MANAGER);
            if ((recu != null) && (enMarche)) {
                infoDeContexte = recu.getContent(); // recuperer la chaine de cars contenant l'alarme recue
                if (infoDeContexte != null) { // si on a recu une info de contexte
                    signalEvent(new ContextInformation(infoDeContexte+" from: "+recu.getExpeditorAddress()));
                }
            }
        }
    }

    private void performEvent(ContextInformation alarme) {
        if (enMarche) {
            System.out.println("ContextManager performing event :"+alarme.getInformation());
            if (referent!=null) { // envoyer le message par reseau a la PF referente
                NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.CONTEXT_MANAGER, referent.getNormalizedAddress());
                envoi.setPortNumber(portReferent);
                envoi.addContent(alarme.getInformation());
                emetteurDeMessages.postDirectMessage(envoi);
                System.out.println("ContextManager event :"+alarme.getInformation()+" sended to: "+referent.getNormalizedAddress()+":"+portReferent);
            }
            else { // si on n'a pas de PF referente
                // envoyer le message par reseau un hote qui fera relais si on en a un
                if (relaisReferent!=null) {
                    NetworkPlatformMessage reponse = new NetworkPlatformMessage(Parameters.CONTEXT_MANAGER, relaisReferent.getNormalizedAddress());
                    reponse.setPortNumber(portRelaisReferent);
                    reponse.addContent(alarme.getInformation());
                    emetteurDeMessages.postDirectMessage(reponse);
                    System.out.println("ContextManager event :"+alarme.getInformation()+" sended to: "+relaisReferent.getNormalizedAddress()+":"+portRelaisReferent);
                }
            }
        }
    }

}
