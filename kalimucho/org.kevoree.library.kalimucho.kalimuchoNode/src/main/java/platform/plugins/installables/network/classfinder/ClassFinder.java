package platform.plugins.installables.network.classfinder;

import model.interfaces.platform.IPlatformPlugin;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import network.platform.NetworkPlatformMessage;
import java.util.Vector;
import java.util.TimerTask;
import java.util.Timer;
import util.NetworkAddress;
import platform.ClassManager.KalimuchoClassLoader;
import platform.plugins.installables.jarRepository.JarRepositoryManager;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import network.platform.PlatformMessagesReceptor;

/**
 * Finds the java byte code for a class.
 * This code is finded in already loaded jar files or downloaded from network.
 * The download uses a broadcast/multicast message in order to find an host that can send this code.
 * @author Dalmau
 */
public class ClassFinder extends Thread implements IPlatformPlugin, IClassFinder {

    private PlatformMessagesReceptor boiteDeReponses;
    private Vector<NetworkPlatformMessage> boiteALettres; // stocke les reponses recues
    private boolean fin; // pour terminer lorsqu'il n'y a pas de route
    private int tentatives; // nombre de tentatives de recherche de classe
    private NetworkReceptorContainer rec;
    private NetworkEmitterContainer nec;
    static private boolean actif;

    /**
     * Creates the plugin of the platform that manage finding classes on network
     */
    public ClassFinder() {
        boiteALettres = new Vector<NetworkPlatformMessage>();
        nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        rec = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        boiteDeReponses = rec.getPlatformMessagesReceptor();
        actif = false;
        try { // enregistrer le service de recherche de classes
            ServicesRegisterManager.registerService(Parameters.CLASS_FINDER_SERVICE, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Class finder service created twice");
        }
    }

    /**
     * Starts the plugin
     */
    public void startPlugin() {
        if (actif) return;
        actif = true;
        boiteDeReponses.inscription(Parameters.CLASS_FINDER_SERVICE);
        start();
    }

    /**
     * Stops the server that receives broadcast messages and the server thar receives replies to these broadcast messages
     */
    public void stopPlugin() {
        if (!actif) return;
        actif = false;
        try {
            ServicesRegisterManager.removeService(Parameters.CLASS_FINDER_SERVICE);
        }
        catch (ServiceClosedException sce) {}
        boiteDeReponses.stop();
        boiteDeReponses.desinscription(Parameters.CLASS_FINDER_SERVICE);
    }


    /**
     * Performs the treatment associated to a received request for getting a class
     * @param message the message sent by the host that requires a specific class
     */
    private void traiterDemandes(NetworkPlatformMessage message) {
       String[] parties = message.getContent().split(";");
       String classeDemandee = parties[0];
       String typeDHote = parties[1];
       String demandeur = parties[2];
       NetworkAddress destinataire = new NetworkAddress(demandeur);
       NetworkAddress expediteur = new NetworkAddress(message.getExpeditorAddress());
       try { // essayer de trouver la classe demandee
           try {
               JarRepositoryManager depot = (JarRepositoryManager)ServicesRegisterManager.lookForService(Parameters.JAR_REPOSITORY_MANAGER);
               byte[] code = depot.findByteCodeForClass(typeDHote, classeDemandee);
               // si on n'a pas eu d'exception on l'a => envoyer la reponse
               NetworkPlatformMessage reponse = new NetworkPlatformMessage();
               if (destinataire.equals(expediteur)) reponse.setFinalAddress("local");
               else reponse.setFinalAddress(destinataire.getNormalizedAddress());
               reponse.setFinalPort(0);
               reponse.addContent(classeDemandee);
               reponse.setAddress(expediteur.getNormalizedAddress());
               reponse.setPortNumber(0);
               reponse.setOwner(Parameters.CLASS_FINDER_SERVICE);
               reponse.setReplyTo(Parameters.CLASS_FINDER_SERVICE);
               reponse.setSerializedObject(code);
               System.out.println("Sending class: "+reponse.getContent()+" for "+demandeur+" to "+reponse.getFinalAddress()+" via "+reponse.getAddress());
               nec.getPlatformMessagesEmitter().postDirectMessage(reponse);
               }
           catch (ServiceClosedException sce){ // re-diffuser la demande
                if (destinataire.equals(expediteur)) { // faire passer le broadcast
                    // renvoyer les demandes non traitees en broadcast
                    message.setExpeditorAdressWhenSending();
                    nec.getPlatformMessagesEmitter().postBroadcastMessage(message);
                }
            }
       }
       catch (ClassNotFoundException cnfe) { // re-diffuser la demande
                if (destinataire.equals(expediteur)) { // faire passer le broadcast
                    // renvoyer les demandes non traitees en broadcast
                    message.setExpeditorAdressWhenSending();
                    nec.getPlatformMessagesEmitter().postBroadcastMessage(message);
                }
        }

    }

    /**
     * Deposes a reply to a broadcasted message used for requesting a class on network
     * @param message the reply to depose
     */
    public synchronized void depose(NetworkPlatformMessage message) {
        // depot d'une reponse recue au routage
        boiteALettres.addElement(message);
        notifyAll();
    }

    private synchronized NetworkPlatformMessage retire(String classToFind) {
        // retrait d'une reponse au routage
        boolean trouve = false;
        NetworkPlatformMessage recu = null;
        try {
            while (!trouve) { // attente de reponse a la question posee
                while ((boiteALettres.size() == 0) && (!fin)) {
                    wait(); // attente d'une reponse
                }
                if (!fin) { // le delai d'attente n'est pas ecoule
                    recu = boiteALettres.firstElement();
                    boiteALettres.removeElementAt(0);
                    // verifier qu'il s'agisse bien d'une reponse a la question posee
                    // Ce test est necessaire car des reponses a une question anterieure
                    // peuvent encore arriver.
                    if (recu.getContent().equals(classToFind)) trouve = true;
                    else recu = null;
                }
                else trouve = true; // on s'arrete faute de reponse
            }
            return recu; // route recue ou null si fin du delai
        }
        catch (InterruptedException ie) {
            return null;
        }
    }

    /**
     * Sends a broadcasted message used for requesting a class on network and
     * returns the reply
     * @param classeDemandee requested class
     * @return the reply
     * @throws ClassNotFoundException when there is no reply
     */
    public byte[] findClass(String classeDemandee) throws ClassNotFoundException {
        // recherche d'une classe passee en parametre
        System.out.println("Finding class: "+classeDemandee);
        boiteALettres.removeAllElements(); // virer les reponses anterieures
        Timer delai = new Timer(); // timer pour éviter d'attendre trop longtemps
        NetworkPlatformMessage recu = null;
        boolean trouve = false; // pas de reponse pour le moment
        tentatives = 0;
        while ((tentatives < Parameters.NUMBER_RETRIES_FOR_CLASSFINDER) && (!trouve)) { // faire une tentative
            // Envoi de le requete sur chaque reseau
            NetworkPlatformMessage envoi = new NetworkPlatformMessage();
            envoi.setFinalAddress("local");
            envoi.addContent(classeDemandee+";"+KalimuchoClassLoader.MON_TYPE+";");
            envoi.setOwner(Parameters.CLASS_FINDER_SERVICE);
            envoi.setReplyTo(Parameters.CLASS_FINDER_SERVICE);
            envoi.setExpeditorAdressWhenSending();
            nec.getPlatformMessagesEmitter().postBroadcastIncompleteMessage(envoi);
            delai.schedule(new WaitForReply(), Parameters.MAXIMAL_WAIT_FOR_RETRY_CLASSFINDER);
            fin = false; // le delai d'attente n'est pas ecoule
            recu = null;
            while ((!fin) && (!trouve)) { // attente reponse ou fon de delai
                recu = retire(classeDemandee); // retirer la reponse
                if (recu != null) trouve = true; // on a trouve la classee
                delai.cancel(); // arrêter le timer
            }
            if (!trouve) { // reessayer une fois
                delai = new Timer();
                tentatives++;
                System.out.println("Retrying finding class: "+classeDemandee);
            }
        }
        if (trouve) { // on a obtenu une reponse => classe trouvee
            System.out.println("Class found: "+classeDemandee);
            return recu.getSerializedObject(); // renvoyer la classe trouvee
        }
        else { // si pas de classe apres n tentatives lever une exception
            throw new ClassNotFoundException();
        }
    }

    private synchronized void arreter() {
        notifyAll(); // debloquer le thread qui attend une route
    }

    // Mise en place d'un delai de garde pour l'attente de reponses au broadcast
    private class WaitForReply extends TimerTask {
         public void run() { // le delai d'attente est passe
             fin = true;
             arreter(); // debloquer le thread demandeur
         }
    }

    /**
     * Waits for messages concerning classes.<br>
     * When a message asks for a class, this thread tries to replie by sending the code if it has it or
     * relaying the request if not.<br>
     * When a message is a reply, it contains the requested class.
     */
    @Override
    public void run() {
        while (actif) {
            NetworkPlatformMessage recu = boiteDeReponses.retirerMessage(Parameters.CLASS_FINDER_SERVICE);
            if (recu != null) {
                String msg = recu.getContent();
                if (msg.contains(";")) { // message de demande
                    traiterDemandes(recu);
                }
                else { // message de reponse
                    depose(recu);
                }
            }
        }
    }

}
