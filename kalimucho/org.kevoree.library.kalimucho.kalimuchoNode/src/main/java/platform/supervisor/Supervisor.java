package platform.supervisor;

/**
 *
 * @author Dalmau
 */

import platform.ClassManager.ClassLoaderFromJarFile;
import platform.containersregister.ContainersManager;
import platform.containersregister.ConnectorsRegistration;
import platform.containersregister.OsagaiaContainerDescriptor;
import platform.containersregister.ConnectedInput;
import platform.containersregister.KorronteaContainerDescriptor;
import platform.containersregister.ComponentsRegistration;
import model.interfaces.IContainer;
import model.osagaia.BCContainer;
import model.osagaia.BCModel;
import model.korrontea.ConnectorContainer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.servicesregister.ServiceInUseException;

import platform.context.ContextManager;
import platform.context.ContextInformation;
import platform.context.status.HostStatus;

import network.NetworkEmissionService;
import network.NetworkReceptionService;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import network.platform.PlatformMessagesEmitter;
import network.platform.PlatformMessagesReceptor;
import network.platform.NetworkPlatformMessage;
import network.AddressesChecker;
import util.NetworkAddress;
import platform.InternalPlatformMessage;
import platform.plugins.installables.network.routing.ReplyForRouteMessage;
import platform.plugins.installables.network.routing.NoRouteException;
import platform.plugins.installables.network.DNS.KalimuchoDNS;
import platform.plugins.installables.application.BCCommandSenderPlugin;

import util.Commands;
import util.Parameters;

/**
 * The supervisor is a thread which listens for messages coming from others platforms and executes these command.
 * The supervisor execute command for:<br>
 * creating/removing a component or a connector<br>
 * migrate a component<br>
 * connect/disconnect inputs/outputs of components or connectors<br>
 * reply to request about host/component/connectors state.
 *
 * @author Dalmau
 */

public class Supervisor extends Thread {

    private AddressesChecker addressesChecker;
    // liens du superviseur avec les autres services de la PF 
    private NetworkEmissionService serviceEmissionReseau;
    private NetworkReceptionService serviceReceptionReseau;
    private PlatformMessagesEmitter emetteurDeMessages;
    private PlatformMessagesReceptor receptionMessages;
    private ContextManager gestionnaireContexte;
    // Enregistrement des composants et des connecteurs
    private ContainersManager gestionnaireDeConteneurs;
    private ComponentsRegistration composants; // Liste des composants crees
    private ConnectorsRegistration connecteurs; // Liste des connecteurs crees
    // Indicateur d'activite du superviseur
    private boolean enMarche;

    /**
     * Construction of the supervisor<br>
     * Find the services, run the host load watcher
     */
    public Supervisor() {
        // Creation des listes de composants et de connecteurs
        gestionnaireDeConteneurs = (ContainersManager) ServicesRegisterManager.platformWaitForService(Parameters.CONTAINERS_MANAGER);
        connecteurs = gestionnaireDeConteneurs.getConnecteurs(); // liste des connecteurs
        composants = gestionnaireDeConteneurs.getComposants(); // liste des conteneurs de CM
        // Recherches des services
        // Service d'acces aux adresses de cette plateforme
        addressesChecker = (AddressesChecker) ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        // Services d'acces aux reseaux
        NetworkEmitterContainer nec = (NetworkEmitterContainer) ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        serviceEmissionReseau = nec.getNetworkEmissionService();
        emetteurDeMessages = nec.getPlatformMessagesEmitter();
        NetworkReceptorContainer nrc = (NetworkReceptorContainer) ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        serviceReceptionReseau = nrc.getNetworkReceptionService();
        receptionMessages = nrc.getPlatformMessagesReceptor();
        receptionMessages.inscription(Parameters.SUPERVISOR);
        // Service de gestion du contexte
        gestionnaireContexte = (ContextManager) ServicesRegisterManager.platformWaitForService(Parameters.CONTEXT_MANAGER);
        // Enregistrer le service superviseur
        try {
            ServicesRegisterManager.registerService(Parameters.SUPERVISOR, this);
        } catch (ServiceInUseException mbiue) {
            System.err.println("Supervisor service created twice");
        }
    }

    // arret du superviseur lorsque l'on arrete la PF

    /**
     * Stops the supervisor.<br>
     * A signal is sent indicationg tha this platform is stopped.
     * All the connectors are destroyed.
     */
    public void stopThread() {
        // lever une alarme d'arret
        gestionnaireContexte.signalEvent(new ContextInformation("Kalimucho platform stopped by user"));
        // arreter le run du superviseur (n'accepte plus de commandes)
        enMarche = false;
        receptionMessages.desinscription(Parameters.SUPERVISOR);
        // supprimer tous les connecteurs
        while (connecteurs.getConnectorsNumber() != 0) {
            supprimerconnecteur(connecteurs.getConnectorAt(0).getNom());
        }
    }

    // Boucle de traitement des messages reeus par la PF

    /**
     * Wait for command and execute them
     */
    @Override
    public void run() {
        try {
            System.out.println("Supervisor ready: (IP address = " + addressesChecker.getMyFirstAddress(NetworkAddress.TYPE_IPV4).getNormalizedAddress() + ")");
        } catch (Exception ignore) {
            //Don't crash if no address
            System.out.println("Supervisor ready : localhost only");

        }
        enMarche = true;
        while (enMarche) { // La PF peut etre arretee par stopThread
            // traitement des commandes reeues par reseau
            String cmd = null;
            // Attendre une commande sur la reseau
            NetworkPlatformMessage lu = receptionMessages.retirerMessage(Parameters.SUPERVISOR);
            if (lu != null) {
                cmd = lu.getContent(); // recuperer la chaene de cars contenant la commande reeue
                if (cmd != null) { // si on a reeu une commande la traiter
                    traiterCommande(lu);
                }
            }
        }
    }

    // Methodes internes de fonctionnement de la PF
    // creation/suppression de conteneur
    // Deconnexion de l'entree ou la sortie d'un composant
    // Reconnexion de l'entree ou la sortie d'un composant (e un nouveau connecteur)
    // Lancement et arret un CM
    // Renvoi de la QdS d'un CM d'un conteneur
    // Renvoi de l'etat de l'hete

    // ************************
    // Creation d'un composant
    // ************************
    public void creerComposant(String nom, String classeCM, CommandAnalyser cmde) {
        // reeoit le nom symbolique du composant osagaia
        // le nom de la classe du CM qu'il contient
        // les connexions en entree : liste de noms de connecteurs
        // les connexions en sortie : liste de noms de connecteurs
        // creer un conteneur de CM
        BCContainer osagaia = new BCContainer(nom, classeCM, cmde.getSplittedEntryList(), cmde.getSplittedOutputList());
        // Enregistrer ce conteneur de CM dans la liste
        composants.ajouterConteneur(new OsagaiaContainerDescriptor(osagaia, osagaia.getClassLoader(), nom, cmde.getSplittedEntryList(), cmde.getSplittedOutputList()));
    }

    // *************************
    // Creation d'un connecteur
    // *************************
    private void creerconnecteur(String name, String input, String output) {
        // reeoit le nom symbolique du connecteur korrontea
        // la connexion en entree = interne ou @ de l'emetteur
        // la connexion en sortie = interne ou @ oe envoyer
        // Pour les connecteurs en 2 parties demander e la PF qui accueille l'autre extremite du connecteur de la creer
        new ThreadCreationConnecteur(name, input, output);
    }

    // ******************************
    // Creation d'un cnnecteur relai
    // ******************************
    private void creerconnecteurRelais(String nom, String entree, String sortie, String sender) {
        // reeoit le nom symbolique du connecteur korrontea
        // la connexion en entree = @ de l'emetteur
        // la connexion en sortie = @ oe envoyer
        if ((entree.equals(Commands.ES_INTERNE)) || (sortie.equals(Commands.ES_INTERNE))) {
            System.err.println("Cant create relai connector from: " + entree + " to: " + sortie);
        } else { // il s'agit bien d'un connecteur dont l'entree et la sortie sont externes (relais)
            // On ne verifie pas que l'entree et la sortie soient accessibles car cette commande est toujours envoyee
            //  par une PF qui cree un connecteur et qui a deja verifie que ce soit faisable
            // Demander aux PFs qui accueillent les autres extremites du connecteur de les creer
            // Sauf a celle qui a envoye la commande
            NetworkAddress ent = new NetworkAddress(entree);
            NetworkAddress sort = new NetworkAddress(sortie);
            NetworkAddress send = null;
            if (sender != null) send = new NetworkAddress(sender);
            if (!ent.equals(send)) { // l'entree ne correspond pas a celui qui a envoye la commande
                NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + Commands.ES_INTERNE + " " + addressesChecker.getMyFirstAddress(ent.getType()).getNormalizedAddress());
                emetteurDeMessages.postMessage(envoi);
            }
            if (!sort.equals(send)) { // la sortie ne correspond pas a celui qui a envoye la commande
                // envoyer la commande de creation e l'autre PF pour la sortie
                NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + addressesChecker.getMyFirstAddress(sort.getType()).getNormalizedAddress() + " " + Commands.ES_INTERNE);
                emetteurDeMessages.postMessage(envoi);
            }
            creerExtremiteconnecteur(nom, entree, sortie); // creation de la partie locale du connecteur
        }
    }

    // *****************************************************
    // Creation de la partie locale d'un connecteur reparti
    // *****************************************************
    private void creerExtremiteconnecteur(String nom, String entree, String sortie) {
        // reeoit le nom symbolique du connecteur korrontea
        // la connexion en entree = interne ou @ de l'emetteur
        // la connexion en sortie = interne ou @ oe envoyer
        // creer un conteneur de flux
        ConnectorContainer korrontea = new ConnectorContainer(nom);
        // Enregistrer ce conteneur de flux dans la liste
        connecteurs.ajouterConteneur(new KorronteaContainerDescriptor(korrontea, nom, entree, sortie));
        // Creer, si necessaire le thread emetteur ou recepteur
        if (!entree.equals(Commands.ES_INTERNE)) { // entree par reseau => creer un thread recepteur
            // Creation du thread de reception par reseau
            serviceReceptionReseau.createConnectorReceptionThread(nom, new NetworkAddress(entree)); // lancer un processus pour cette entree reseau
        }
        if (!sortie.equals(Commands.ES_INTERNE)) { // sortie par reseau => creer un thread emetteur
            // Creation du thread d'emission sur le reseau
            serviceEmissionReseau.createConnectorEmissionThread(nom, new NetworkAddress(sortie)); // lancer un processus pour cette sortie reseau
        }
        try {
            model.korrontea.ControlUnit uc = (model.korrontea.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.startUEandUS(); // lancement de l'UE (attente de connexion du composant osagaia)
            uc.start_BC(); // lancement du CM de transfert de flux
        } catch (ServiceClosedException sce) {
            System.err.println("Can't create connector : " + nom);
        }
    }

    // ****************************************
    // Deconnexion d'une entree d'un composant
    // ****************************************
    private void deconnecterEntreeComposant(String nom, int numero) {
        // deconnexion de l'entree d'un conteneur de CM
        // le CM n'est pas arrete il attent une reconnexion e un autre connecteur
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.disconnectIU(numero); // deconnecter l'UE du conteneur de CM
            composants.changerEntreeConteneur(nom, numero, ComponentsRegistration.DECONNECTE);
        } catch (ServiceClosedException sce) {
            System.err.println("Component input deconnection: " + nom + " unknown component");
        }
    }

    // ****************************************
    // Reconnexion d'une entree d'un composant
    // ****************************************
    private void reconnecterEntreeComposant(String nom, int numero, String entree) {
        // reconnexion de l'entree d'un conteneur de CM qui a ete deconnecte
        // soit par la PF soit par suppression du connecteur auquel il etait connecte
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.connectIU(numero, entree); // reconnecter l'UE du conteneur de CM
            composants.changerEntreeConteneur(nom, numero, entree);
        } catch (ServiceClosedException sce) {
            System.err.println("Component input reconnection: " + nom + " unknown component");
        }
    }

    // ****************************************
    // Duplication de la sortie d'un composant
    // ****************************************
    private void dupliquerSortieComposant(String nom, String sortie) {
        // ajout de connexion sur la sortie d'un conteneur de CM
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.connectOU(sortie); // connecter l'US du conteneur de CM
            composants.ajouterSortieConteneur(nom, sortie);
        } catch (ServiceClosedException sce) {
            System.err.println("Component output reconnection: " + nom + " unknown component");
        }
    }

    // ********************************************************************************
    // Redirection de l'entree d'un connecteur vers un autre hote ou vers l'hote local
    // ********************************************************************************
    private void redirigerEntreeconnecteur(String nom, String vers, String sender) {
        // Utilise lors de la migration d'un composant :
        new ThreadRedirigerEntreeConnecteur(nom, vers, sender);
    }

    // *********************************************************************************
    // Redirection de la sortie d'un connecteur vers un autre hote ou vers l'hote local
    // *********************************************************************************
    private void redirigerSortieconnecteur(String nom, String vers, String sender) {
        // Utilise lors de la migration d'un composant :
        // la sortie du connecteur peut etre locale ou distante mais l'entree est obligatoirement locale
        new ThreadRedirigerSortieConnecteur(nom, vers, sender);
    }

    // ***************************
    // Suppression d'un composant
    // ***************************
    private void supprimerComposant(String nom) {
        OsagaiaContainerDescriptor designation = composants.trouverConteneur(nom);
        if (designation == null) return; // le composant n'existe pas => on nefait rien
        // suppression d'un conteneur de CM
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            try { // desenregistrer ce CM s'il envoie des commandes a la PF
                BCCommandSenderPlugin commandes = (BCCommandSenderPlugin) ServicesRegisterManager.lookForService(Parameters.APPLICATION_COMMAND_SERVICE);
                commandes.removeReplyListener(nom);
            } catch (ServiceClosedException sce) {
            } // le service d'envoi de commandes n'est pas installe
            uc.stop();
            uc.disconnection(); // deconnecter le composant de ses connecteurs d'entree et de sortie
            uc.stop_CM(); // arreter le CM
            uc.join_CM(); // attendre que le CM s'arrete
            designation.stopConteneur(); // arreter le conteneur de CM
            composants.enleverConteneur(nom); // supprimer ce conteneur de la liste
        } catch (ServiceClosedException sce) {
            System.err.println("Component remove: " + nom + " unknown component");
        }
    }

    // ****************************
    // Suppression d'un connecteur
    // ****************************
    /*
     * La version de supprimerconnecteur qui suit permet de supprimer un connecteur distribue ou relais
     * en n'envoyant la commande de suppression qu'a une seule plates-formes accueillant ce connecteur.
     * Cette PF transmet a l'autre PF un ordre de suppression du connecteur.
     * ATTENTION car cette commande n'arrivant pas de la PF qui
     * supervise cet hote, elle peut arriver dans un ordre quelconque vis a vis des commandes
     * de cette PF ce qui fait qu'un connecteur supprime par un hote distant puis recree
     * par la PF peut etre recree avant d'etre supprime.
     */
    private void supprimerconnecteur(String nom) {
        KorronteaContainerDescriptor designation = connecteurs.trouverConteneur(nom);
        if (designation == null) return; // le connecteur n'existe pas => on nefait rien
        // Pour les connecteurs en 2 ou 3 parties
        // demander aux PFs qui accueillent les autres extremites du connecteur de la supprimer
        if (!designation.getSortie().equals(Commands.ES_INTERNE)) {
            // envoyer e la PF qui accueille la sortie du connecteur une demande de suppression
            String sortie = designation.getSortie();
            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
            envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
            emetteurDeMessages.postDirectMessage(envoi);
        }
        if (!designation.getEntree().equals(Commands.ES_INTERNE)) {
            // envoyer e la PF qui accueille l'entree du connecteur une demande de suppression
            String entree = designation.getEntree();
            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
            envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
            emetteurDeMessages.postDirectMessage(envoi);
        }
        supprimerExtremiteconnecteur(nom, null); // supprimer le connecteur : commande locale
    }

    // **********************************************************
    // Suppression de la partie locale d'un connecteur distribue
    // **********************************************************
    private void supprimerExtremiteconnecteur(String nom, String sender) {
        // Suppression d'un connecteur ou d'une partie locale de connecteur
        // Rechercher le connecteur dans la liste
        KorronteaContainerDescriptor designation = connecteurs.trouverConteneur(nom);
        if (designation == null) return; // le connecteur n'existe pas => on ne fait rien
        String sortie = designation.getSortie();
        String entree = designation.getEntree();
        String expediteur = sender;
        NetworkAddress ent = new NetworkAddress(entree);
        NetworkAddress sort = new NetworkAddress(sortie);
        NetworkAddress send = null;
        if (sender != null) send = new NetworkAddress(sender);
        if (sender != null) { // la commande est arrivee par reseau
            if (send.isIPv4()) {
                expediteur = send.getNormalizedAddress(); // normaliser l'@ de l'expediteur
            }
            if (!sortie.equals(Commands.ES_INTERNE)) {
                // envoyer e la PF qui accueille la sortie du connecteur une demande de suppression
                if (!sort.equals(send)) { // si ce n'est pas l'expediteur
                    NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                    envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
                    emetteurDeMessages.postDirectMessage(envoi);
                }
            }
            if (!entree.equals(Commands.ES_INTERNE)) {
                // envoyer e la PF qui accueille l'entree du connecteur une demande de suppression
                if (!ent.equals(send)) { // si ce n'est pas l'expediteur
                    NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                    envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
                    emetteurDeMessages.postDirectMessage(envoi);
                }
            }
        }
        try {
            model.korrontea.ControlUnit uc = (model.korrontea.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.stopUEandUS(); // arreter l'UE, l'US et le CM
            uc.join_BC(); // attendre que le CM s'arrete
            if (!sortie.equals(Commands.ES_INTERNE)) {
                // sortie par reseau => arreter le thread d'emission et attendre qu'il soit arrete
                serviceEmissionReseau.removeConnectorEmissionThread(nom);
            }
            if (!entree.equals(Commands.ES_INTERNE)) {
                // entree par reseau => arreter le thread de reception et attendre qu'il soit arrete
                serviceReceptionReseau.removeConnectorReceptionThread(nom);
            }

            designation.stopConteneur(); // supprimer le connecteur
            connecteurs.enleverConteneur(nom); // supprimer ce conteneur de la liste
            composants.deconnecterESConteneur(nom); // deconnecter les CM qui etaient connectes e ce connecteur
        } catch (ServiceClosedException sce) {
            System.err.println("Connector remove: " + nom + " unknown component");
        }
    }

    // ********************************************************************************
    // Envoi d'un composant migre
    // Ceci n'est fait qu'apres avoir recu l'autorisation de migration du destinataire
    // ********************************************************************************
    private void envoyerComposant(String nom, String vers) {
        // Serialiser un composant
        OsagaiaContainerDescriptor designation = composants.trouverConteneur(nom);
        if (designation == null) { // le composant n'existe pas => on nefait rien
            System.err.println("Send component: " + nom + " unknown component");
        } else {
            // Envoyer le composant au destinataire
            BCModel cm = ((BCContainer) designation.getContainer()).getBC();
            String entree = designation.getEntryList(); // liste des entrees de ce composant
            String sortie = designation.getOutputList(); // liste des sorties de ce composant
            supprimerComposant(nom); // supprimer le composant envoye
            InternalPlatformMessage tr = new InternalPlatformMessage(Parameters.SUPERVISOR, vers, cm);
            tr.addContent(Commands.RECEIVE_COMPONENT + " " + nom + " " + entree + " " + sortie); // creer le message
            emetteurDeMessages.postMessage(new NetworkPlatformMessage(tr));
            // rediriger les sorties de tous les connecteurs en entree de ce composant
            for (int i = 0; i < designation.getEntree().length; i++) {
                if ((!designation.getEntree()[i].equals(Commands.ES_NULL)) && (!designation.getEntree()[i].equals(Commands.ES_NOT_USED))) {
                    redirigerSortieconnecteur(designation.getEntree()[i], vers, null);
                }
            }
            // rediriger les entrees de tous les connecteurs en sortie de ce composant
            for (int i = 0; i < designation.getSortie().length; i++) {
                if ((!designation.getSortie()[i].equals(Commands.ES_NULL)) && (!designation.getSortie()[i].equals(Commands.ES_NOT_USED))) {
                    redirigerEntreeconnecteur(designation.getSortie()[i], vers, null);
                }
            }
        }
    }

    // **************************************************************
    // Reception d'un composant migre et relancement de ce composant
    // **************************************************************
    private void recevoirComposant(String nom, CommandAnalyser decoupage, NetworkPlatformMessage recu) {
        try {
            InternalPlatformMessage complet = new InternalPlatformMessage(recu);
            if (complet.getBC() != null) {
                ClassLoaderFromJarFile cl = (ClassLoaderFromJarFile) complet.getClassLoader();
                cl.addLink();
                BCContainer osagaia = new BCContainer(nom, complet.getBC(), cl, decoupage.getSplittedEntryList(), decoupage.getSplittedOutputList());
                osagaia.setSerialized();
                // Enregistrer ce conteneur de CM dans la liste
                composants.ajouterConteneur(new OsagaiaContainerDescriptor(osagaia, cl, nom, decoupage.getSplittedEntryList(), decoupage.getSplittedOutputList()));
            } else {
                System.err.println("Cant load migrated BC: " + nom);
            }
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Cant find class for migrated BC: " + nom);
        }
    }

    // *******************************
    // Lancement d'un composant
    // Cette methode est-elle utile ?
    // *******************************
    private void lancerCM(String nom) {
        // lancement du CM place dans le conteneur dont le nom est passe en parametre
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.start_CM();
        } catch (ServiceClosedException sce) {
            System.err.println("Component run: " + nom + " unknown component");
        }
    }

    // *******************************
    // Arret d'un composant
    // Cette methode est-elle utile ?
    // *******************************
    private void arreterCM(String nom) {
        // arret du CM place dans le conteneur dont le nom est passe en parametre
        try {
            model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(nom));
            uc.stop_CM();
        } catch (ServiceClosedException sce) {
            System.err.println("Component stop: " + nom + " unknown component");
        }
    }

    //**************************************************************
    // Methode generale de traitement des commandes de la PF
    // Analyse du message reeu et execution de la commande associee
    //**************************************************************
    public void traiterCommande(NetworkPlatformMessage recu) {
        String cmd = recu.getContent();
        String expediteur = recu.getExpeditorAddress();
        if (expediteur.equals("")) expediteur = recu.getAddress();
        int portReponse = recu.getExpeditorPort();
        if (recu.getReplyTo().equals(Parameters.SUPERVISOR)) {
            System.out.println("***** Executing command :" + cmd + " sent by " + expediteur + " ID: " + recu.getSenderID());
//            System.out.println("***** Date :"+System.currentTimeMillis());
        }
        try {
            // Decoupage de la commande en champs
            CommandAnalyser decoupage = new CommandAnalyser(cmd);
            long tempsDebut;
            if (Parameters.TIME_MEASURE) {
                tempsDebut = System.nanoTime();
            }
            if (decoupage.getSplittedCommand().length < 1) {
                System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                return;
            }
            String commande = decoupage.getSplittedCommand()[0]; // la partie commande
            // Commande de creation d'un composant
            if (commande.equals(Commands.CREER_COMPOSANT)) { // creation d'un conteneur de CM
                if ((decoupage.getSplittedCommand().length < 5) || (decoupage.getSplittedEntryList().length < 1) || (decoupage.getSplittedOutputList().length < 1)) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (4)
                String nom = decoupage.getSplittedCommand()[1];
                String classeCM = decoupage.getSplittedCommand()[2];
                creerComposant(nom, classeCM, decoupage); // execution de la commande
            }
            // Commande de lancement d'un composant
            else if (commande.equals(Commands.LANCER_COMPOSANT)) { // lancement d'un CM
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                lancerCM(nom); // execution de la commande
            }
            // Commande d'arret d'un composant
            else if (commande.equals(Commands.ARRETER_COMPOSANT)) { // Arret d'un CM
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                arreterCM(nom); // execution de la commande
            }
            // Commande de creation d'un connecteur
            else if (commande.equals(Commands.CREER_connecteur)) { // Creation d'un conteneur de flux
                if (decoupage.getSplittedCommand().length < 4) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (3)
                String nom = decoupage.getSplittedCommand()[1];
                String entree = decoupage.getSplittedCommand()[2];
                String sortie = decoupage.getSplittedCommand()[3];
                creerconnecteur(nom, entree, sortie); // execution de la commande
            }
            // Commande de creation d'un connecteur relai
            else if (commande.equals(Commands.CREATE_RELAY_CONNECTOR)) { // Creation d'un conteneur de flux
                if (decoupage.getSplittedCommand().length < 4) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (3)
                String nom = decoupage.getSplittedCommand()[1];
                String entree = decoupage.getSplittedCommand()[2];
                String sortie = decoupage.getSplittedCommand()[3];
                creerconnecteurRelais(nom, entree, sortie, expediteur); // execution de la commande
            }
            // Commande de creation de la partie locale d'un conecteur reparti
            else if (commande.equals(Commands.CREATE_PART_OF_CONNECTOR)) {
                if (decoupage.getSplittedCommand().length < 4) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                // Commande envoyee par la PF qui a cree l'autre extremite d'un connecteur reparti
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (3)
                String nom = decoupage.getSplittedCommand()[1];
                String entree = decoupage.getSplittedCommand()[2];
                String sortie = decoupage.getSplittedCommand()[3];
                creerExtremiteconnecteur(nom, entree, sortie); // execution de la commande
            }
            // Commande de creation de la partie locale d'un conecteur reparti utilisant un reali
            // Commande de suppression d'un composant
            else if (commande.equals(Commands.SUPPRIMER_COMPOSANT)) { // Suppression d'un conteneur de CM
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                supprimerComposant(nom); // execution de la commande
            }
            // Commande de suppression d'un connecteur
            else if (commande.equals(Commands.SUPPRIMER_connecteur)) { // Suppresssion d'un conteneur de flux
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                supprimerconnecteur(nom); // execution de la commande
            }
            // Commande de suppression de la partie locale d'un connecteur reparti
            else if (commande.equals(Commands.REMOVE_PART_OF_CONNECTOR)) {
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                // Commande envoyee par la PF qui a supprime l'autre extremite du connecteur
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                supprimerExtremiteconnecteur(nom, expediteur); // execution de la commande
            }
            // Commande d'envoi de la QdS d'un composant ou d'un connecteur
            else if (commande.equals(Commands.LIREQDS)) { // envoi de la QdS d'un conteneur de flux ou de CM
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                String qds = String.valueOf(gestionnaireContexte.getComponentQdS(nom)); // lecture de la QdS via l'UC
                NetworkPlatformMessage reponse = new NetworkPlatformMessage(recu.getReplyTo(), expediteur);
                reponse.setPortNumber(portReponse);
                reponse.addContent(qds);
                emetteurDeMessages.postMessage(reponse); // envoyer ea par reseau
            }
            // Commande d'envoi de l'etat de l'hote
            else if (commande.equals(Commands.LIRE_ETAT)) { // envoi de l'etat de l'hote
                if (decoupage.getSplittedCommand().length < 1) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF a laquelle signaler les evenements
                NetworkPlatformMessage reponse = new NetworkPlatformMessage(recu.getReplyTo(), expediteur);
                reponse.setPortNumber(portReponse);
                HostStatus etat = gestionnaireContexte.getHostStatus();
                reponse.addContent(etat.toMessage());
                reponse.setSerializedObject(etat.toByteArray());
                emetteurDeMessages.postMessage(reponse); // envoyer ea par reseau
            }
            // Commande d'envoi de l'etat d'un composant ou d'un connecteur
            else if (commande.equals(Commands.LIRE_ETAT_CONTENEUR)) { // envoi de l'etat d'un conteneur
                if (decoupage.getSplittedCommand().length < 2) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                String nom = decoupage.getSplittedCommand()[1];
                String etat = "";
                OsagaiaContainerDescriptor designationComposant = composants.trouverConteneur(nom);
                if (designationComposant != null) { // c'est un composant
                    etat = new String("Osagaia container input=" + designationComposant.getEntryList() + " output=" + designationComposant.getOutputList());
                } else {
                    KorronteaContainerDescriptor designationconnecteur = connecteurs.trouverConteneur(nom);
                    if (designationconnecteur != null) { // c'est un connecteur
                        etat = new String("Korrontea container input=" + designationconnecteur.getEntree() + " output=" + designationconnecteur.getSortie() + " debit: " + gestionnaireContexte.getConnectorTrafficAverage(nom));
                    } else etat = new String("Unknown container: " + nom);
                }
                NetworkPlatformMessage reponse = new NetworkPlatformMessage(recu.getReplyTo(), expediteur);
                reponse.setPortNumber(portReponse);
                reponse.addContent(etat);
                emetteurDeMessages.postMessage(reponse); // envoyer ea par reseau
            }
            // Commande deconnexion de l'entree d'un composant
            else if (commande.equals(Commands.DECONNECTER_ENTREE_COMPOSANT)) {
                if (decoupage.getSplittedCommand().length < 3) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                // deconnexion de l'entree d'un conteneur de CM
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation du parametre de la commande
                String nom = decoupage.getSplittedCommand()[1];
                int numero = -1;
                try {
                    numero = Integer.parseInt(decoupage.getSplittedCommand()[2]);
                } catch (NumberFormatException nfe) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                deconnecterEntreeComposant(nom, numero); // execution de la commande
            }
            // Commande reconnexion de l'entree d'un composant
            else if (commande.equals(Commands.RECONNECTER_ENTREE_COMPOSANT)) {
                if (decoupage.getSplittedCommand().length < 4) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                // Reconnexion de l'entree d'un conteneur de CM e un nouveau connecteur
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (2)
                String nom = decoupage.getSplittedCommand()[1];
                int numero = -1;
                try {
                    numero = Integer.parseInt(decoupage.getSplittedCommand()[2]);
                } catch (NumberFormatException nfe) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                String entree = decoupage.getSplittedCommand()[3];
                reconnecterEntreeComposant(nom, numero, entree); // execution de la commande
            }
            // Commande reconnexion ou de duplication de la sortie d'un composant
            else if (commande.equals(Commands.RECONNECTER_SORTIE_COMPOSANT)) {
                if (decoupage.getSplittedCommand().length < 3) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                // Reconnexion de la sortie d'un conteneur de CM e un nouveau connecteur
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (2)
                String nom = decoupage.getSplittedCommand()[1];
                String sortie = decoupage.getSplittedCommand()[2];
                dupliquerSortieComposant(nom, sortie); // execution de la commande
            }
            // Commande d'envoi d'un composant en migration
            else if (commande.equals(Commands.ENVOYER_COMPOSANT)) {
                if (decoupage.getSplittedCommand().length < 3) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, true); // la PF e laquelle signaler les evenements
                String nom = decoupage.getSplittedCommand()[1];
                String vers = decoupage.getSplittedCommand()[2];
                envoyerComposant(nom, vers);
            }
            // Commande de reception d'un composant migre
            else if (commande.equals(Commands.RECEIVE_COMPONENT)) { // creation d'un conteneur de CM
                if ((decoupage.getSplittedCommand().length < 4) || (decoupage.getSplittedEntryList().length < 1) || (decoupage.getSplittedOutputList().length < 1)) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                // Recuperation des parametres de la commande (4)
                String nom = decoupage.getSplittedCommand()[1];
                recevoirComposant(nom, decoupage, recu); // execution de la commande
            }
            // Commande de redirection de l'entree d'un connecteur vers un autre hote ou vers cet hote
            else if (commande.equals(Commands.REDIRECT_INPUT_OF_CONNECTOR)) {
                if (decoupage.getSplittedCommand().length < 3) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                String nom = decoupage.getSplittedCommand()[1];
                String vers = decoupage.getSplittedCommand()[2];
                redirigerEntreeconnecteur(nom, vers, expediteur);
            }
            // Commande de redirection de la sortie d'un connecteur vers un autre hote ou vers cet hote
            else if (commande.equals(Commands.REDIRECT_OUTPUT_OF_CONNECTOR)) {
                if (decoupage.getSplittedCommand().length < 3) {
                    System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
                    return;
                }
                setReferentPlatform(expediteur, portReponse, false); // la PF e laquelle signaler les evenements
                String nom = decoupage.getSplittedCommand()[1];
                String vers = decoupage.getSplittedCommand()[2];
                redirigerSortieconnecteur(nom, vers, expediteur);
            } else if (commande.equals(Commands.ENVOYER_DNS)) {
                setReferentPlatform(expediteur, portReponse, false); // la PF a laquelle signaler les evenements
                NetworkPlatformMessage reponse = new NetworkPlatformMessage(recu.getReplyTo(), expediteur);
                reponse.setPortNumber(portReponse);
                try {
                    KalimuchoDNS dns = (KalimuchoDNS) ServicesRegisterManager.lookForService(Parameters.KALIMUCHO_DNS_MANAGER);
                    reponse.addContent(addressesChecker.getHostIdentifier());
                    reponse.setSerializedObject(dns.getHostsAsByteArray());
                    // dns.dump();
                } catch (ServiceClosedException sce) {
                    reponse.addContent("No DNS active for " + addressesChecker.getHostIdentifier());
                }
                emetteurDeMessages.postMessage(reponse); // envoyer ea par reseau
                //gestionnaireContexte.getHostStatus().dump();
            }
            // Commande inconnue => erreur
            else {
                System.err.println("Supervisor: unknown command: " + cmd + " received from: " + expediteur);
            }
            // Partie utilisee pour faire des mesures de temps d'execution des commandes par la PF
            if (Parameters.TIME_MEASURE) {
                float val = System.nanoTime();
                val = (val - tempsDebut) / 1000000;
                System.out.println("temps d'execution de la commande " + cmd + " : " + val + " ms");
            }
        } catch (CommandSyntaxException cse) { // commande syntaxiquement incorrecte => erreur
            System.err.println("Supervisor: incorrect command: " + cmd + " received from: " + expediteur);
        }
    }

    //*******************************************************************************************************
    // Methode permettant d'indiquer au gestionnaire de contexte a quelle PF envoyer les infos de contexte
    // Ceci est provisoire et ne sert que parce les informations de contexte ne sont pas traitees localement
    //*******************************************************************************************************
    private void setReferentPlatform(String expediteur, int portReponse, boolean ref) {
        NetworkAddress exp = new NetworkAddress(expediteur);
        if (ref) {
            gestionnaireContexte.setReferentPlatform(expediteur, portReponse);
        } else {
            gestionnaireContexte.setRelayReferentPlatform(expediteur, portReponse);
        }
    }

    private class ThreadCreationConnecteur extends Thread {
        private String name;
        private String input;
        private String output;

        public ThreadCreationConnecteur(String nom, String entree, String sortie) {
            name = nom;
            input = entree;
            output = sortie;
            start();
        }

        @Override
        public void run() {
            String nom = name;
            String entree = input;
            String sortie = output;
            NetworkAddress ent = new NetworkAddress(entree);
            entree = ent.getNormalizedAddress();
            NetworkAddress sort = new NetworkAddress(sortie);
            sortie = sort.getNormalizedAddress();
            boolean aFinir = true; // indique si l'on doit creer localement un connecteur
            // ce ne sera pas le cas si on n'a pas de route directe => creation d'un connecteur relai
            // verifier que les adresses fournies ne soient pas les miennes
            // Si c'est le cas l'entree ou la sortie sont internes
            if (!entree.equals(Commands.ES_INTERNE)) { // l'entree n'est pas locale
                if (addressesChecker.isPresentAddress(ent)) entree = Commands.ES_INTERNE;
            }
            if (!sortie.equals(Commands.ES_INTERNE)) { // la sortie n'est pas locale
                if (addressesChecker.isPresentAddress(sort)) sortie = Commands.ES_INTERNE;
            }
            if (!entree.equals(Commands.ES_INTERNE)) { // l'entree n'est pas locale
                try {
                    ReplyForRouteMessage route = emetteurDeMessages.findRoute(ent); // chercher une route vers cette entree
                    if (sortie.equals(Commands.ES_INTERNE)) { // entree externe sortie locale
                        if (route.isDirect()) { // entree externe sortie locale et la route pour l'entree est directe
                            // on va creer on connecteur reparti dont l'autre extremite est a l'entree et qui envoie ici
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                            envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + Commands.ES_INTERNE + " " + addressesChecker.getMyFirstAddress(ent.getType()).getNormalizedAddress());
                            emetteurDeMessages.postDirectMessage(envoi);
                        } else { // entree externe sortie locale et la route pour l'entree est indirecte
                            // on va faire creer un  connecteur relai dont l'entree est sur 'entree' et la sortie ici
                            NetworkAddress via = new NetworkAddress(route.getVia());
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, route.getVia());
                            envoi.addContent(Commands.CREATE_RELAY_CONNECTOR + " " + nom + " " + entree + " " + addressesChecker.getMyFirstAddress(via.getType()).getNormalizedAddress());
                            emetteurDeMessages.postDirectMessage(envoi);
                            ent = via;
                            entree = via.getNormalizedAddress();
                        }
                    } else { // l'entree est externe la sortie aussi => c'est un connecteur relai
                        if (route.isDirect()) { // il faut que l'entree soit accessible directement
                            ReplyForRouteMessage routeSortie = emetteurDeMessages.findRoute(new NetworkAddress(sortie));
                            if (routeSortie.isDirect()) { // l'entre et la sortie sont accessibles directement
                                sortie = routeSortie.getVia();
                                sort = new NetworkAddress(sortie);
                                creerconnecteurRelais(nom, entree, sortie, null); // on cree ici le connecteur relais de entree vers sortie
                                aFinir = false; // la commande est traitee => c'est termine
                            } else { // connecteur relais non faisable car pas d'acces direct a la sortie
                                System.err.println("Creation a connector: can't reach " + sortie);
                                aFinir = false;
                            }
                        } else { // connecteur relais non faisable car pas d'acces direct a l'entree
                            System.err.println("Creation a connector: can't reach " + entree);
                            aFinir = false;
                        }
                    }
                } catch (NoRouteException nr) { // on ne peut pas atteindre l'entree ou la sortie
                    System.err.println("Creation a connector: can't reach " + entree + " or " + sortie);
                    aFinir = false;
                }
            } else { // l'entree est interne
                if (!sortie.equals(Commands.ES_INTERNE)) { // entree interne sortie externe
                    try {
                        ReplyForRouteMessage routeSortie = emetteurDeMessages.findRoute(sort);
                        if (routeSortie.isDirect()) { // entree interne sortie externe et la sortie est directement accessible
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                            envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + addressesChecker.getMyFirstAddress(sort.getType()).getNormalizedAddress() + " " + Commands.ES_INTERNE);
                            emetteurDeMessages.postDirectMessage(envoi);
                        } else { // l'entree est interne et la sortie n'est pas directement accessible
                            // on va faire creer un  connecteur reparti dont l'entree est ici et la sortie sur 'sortie'
                            NetworkAddress via = new NetworkAddress(routeSortie.getVia());
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, routeSortie.getVia());
                            envoi.addContent(Commands.CREATE_RELAY_CONNECTOR + " " + nom + " " + addressesChecker.getMyFirstAddress(via.getType()).getNormalizedAddress() + " " + sortie);
                            emetteurDeMessages.postDirectMessage(envoi);
                            sort = via;
                            sortie = sort.getNormalizedAddress();
                        }
                    } catch (NoRouteException nr) { // on ne peut pas atteindre la sortie
                        System.err.println("Creation a connector: can't reach " + sortie);
                        aFinir = false;
                    }
                }
            }
            if (aFinir) creerExtremiteconnecteur(nom, entree, sortie); // creation de la partie locale du connecteur
        }
    }

    private class ThreadRedirigerEntreeConnecteur extends Thread {
        private String nom;
        private String vers;
        private String sender;

        public ThreadRedirigerEntreeConnecteur(String name, String to, String exp) {
            nom = name;
            vers = to;
            sender = exp;
            start();
        }

        @Override
        public void run() {
            KorronteaContainerDescriptor designation = connecteurs.trouverConteneur(nom);
            if (designation == null) return; // le connecteur n'existe pas => on ne fait rien
            String sortie = designation.getSortie();
            String entree = designation.getEntree();
            NetworkAddress ent = new NetworkAddress(entree);
            NetworkAddress sort = new NetworkAddress(sortie);
            NetworkAddress send = null;
            if (sender != null) send = new NetworkAddress(sender);
            NetworkAddress to = new NetworkAddress(vers);
            boolean aFinir = true; // indique si l'on doit creer localement un connecteur

            if (!entree.equals(Commands.ES_INTERNE)) { // l'entre de ce connecteur arrivait par reseau
                // entree par reseau => arreter le thread de reception et attendre qu'il soit arrete
                serviceReceptionReseau.removeConnectorReceptionThread(nom);
                // demander a la PF qui etait connectee a cette entre de supprimer son morceau de connecteur
                if (!ent.equals(send)) {
                    NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                    envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
                    emetteurDeMessages.postDirectMessage(envoi);
                }
            }
            vers = to.getNormalizedAddress();
            if (!addressesChecker.isPresentAddress(to)) {
                if (!sortie.equals(Commands.ES_INTERNE)) { // la sortie de ce connecteur partait sur reseau
                    // supprimer la partie locale du connecteur et transmettre la commande a 'sortie'
                    supprimerExtremiteconnecteur(nom, null);
                    if (!sort.equals(send)) {
                        NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                        envoi.addContent(Commands.REDIRECT_INPUT_OF_CONNECTOR + " " + nom + " " + vers);
                        emetteurDeMessages.postDirectMessage(envoi);
                    }
                    aFinir = false; // on n'a rien de plus a faire
                } else {
                    try { // relier l'entree de ce connecteur a 'vers'
                        ReplyForRouteMessage route = emetteurDeMessages.findRoute(to); // chercher une route vers cette entree
                        ent = new NetworkAddress(route.getVia());
                        entree = ent.getNormalizedAddress();
                        if (route.isDirect()) { // entree externe sortie locale et la route pour l'entree est directe
                            // on va creer on connecteur reparti dont l'autre extremite est a l'entree et qui envoie ici
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                            envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + Commands.ES_INTERNE + " " + addressesChecker.getMyFirstAddress(ent.getType()).getNormalizedAddress());
                            emetteurDeMessages.postDirectMessage(envoi);
                        } else { // entree externe sortie locale et la route pour l'entree est indirecte
                            // on va faire creer un  connecteur relai dont l'entree est sur 'vers' et la sortie ici
                            NetworkAddress via = new NetworkAddress(route.getVia());
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, route.getVia());
                            envoi.addContent(Commands.CREATE_RELAY_CONNECTOR + " " + nom + " " + vers + " " + addressesChecker.getMyFirstAddress(via.getType()).getNormalizedAddress());
                            emetteurDeMessages.postDirectMessage(envoi);
                        }
                    } catch (NoRouteException nr) { // on ne peut pas atteindre l'entree ou la sortie
                        System.err.println("Creation a connector: can't reach " + to.getNormalizedAddress());
                        aFinir = false;
                    }
                }
                if (aFinir) {
                    // lancer un processus pour cette entree reseau
                    serviceReceptionReseau.createConnectorReceptionThread(nom, new NetworkAddress(entree));
                    designation.setEntree(entree);
                }
            } else { // l'entree du connecteur est devenue interne
                designation.setEntree(Commands.ES_INTERNE);
            }
        }
    }

    private class ThreadRedirigerSortieConnecteur extends Thread {
        private String nom;
        private String vers;
        private String sender;

        public ThreadRedirigerSortieConnecteur(String name, String to, String exp) {
            nom = name;
            vers = to;
            sender = exp;
            start();
        }

        @Override
        public void run() {
            KorronteaContainerDescriptor designation = connecteurs.trouverConteneur(nom);
            if (designation == null) return; // le connecteur n'existe pas => on ne fait rien
            String sortie = designation.getSortie();
            String entree = designation.getEntree();
            NetworkAddress ent = new NetworkAddress(entree);
            NetworkAddress sort = new NetworkAddress(sortie);
            NetworkAddress send = null;
            if (sender != null) send = new NetworkAddress(sender);
            NetworkAddress to = new NetworkAddress(vers);
            boolean aFinir = true; // indique si l'on doit creer localement un connecteur
            if (!sortie.equals(Commands.ES_INTERNE)) { // la sortie de ce connecteur arrivait par reseau
                // sortiee par reseau => arreter le thread d'emission et attendre qu'il soit arrete
                serviceEmissionReseau.removeConnectorEmissionThread(nom);
                // demander a la PF qui etait connectee a cette sortie de supprimer son morceau de connecteur
                if (!sort.equals(send)) {
                    NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                    envoi.addContent(Commands.REMOVE_PART_OF_CONNECTOR + " " + nom);
                    emetteurDeMessages.postDirectMessage(envoi);
                }
            }
            vers = to.getNormalizedAddress();
            if (!addressesChecker.isPresentAddress(to)) {
                if (!entree.equals(Commands.ES_INTERNE)) { // l'entree de ce connecteur partait sur reseau
                    // supprimer la partie locale du connecteur et transmettre la commande a 'sortie'
                    supprimerExtremiteconnecteur(nom, null);
                    if (!ent.equals(send)) {
                        NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, entree);
                        envoi.addContent(Commands.REDIRECT_OUTPUT_OF_CONNECTOR + " " + nom + " " + vers);
                        emetteurDeMessages.postDirectMessage(envoi);
                    }
                    aFinir = false; // on n'a rien de plus a faire
                } else {
                    try { // relier la sortie de ce connecteur a 'vers'
                        ReplyForRouteMessage route = emetteurDeMessages.findRoute(to); // chercher une route vers cette entree
                        sort = new NetworkAddress(route.getVia());
                        sortie = sort.getNormalizedAddress();
                        if (route.isDirect()) { // entree externe sortie locale et la route pour l'entree est directe
                            // on va creer on connecteur reparti dont l'autre extremite est a l'entree et qui envoie ici
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, sortie);
                            envoi.addContent(Commands.CREATE_PART_OF_CONNECTOR + " " + nom + " " + addressesChecker.getMyFirstAddress(sort.getType()).getNormalizedAddress() + " " + Commands.ES_INTERNE);
                            emetteurDeMessages.postDirectMessage(envoi);
                        } else { // entree externe sortie locale et la route pour l'entree est indirecte
                            // on va faire creer un  connecteur relai dont l'entree ici et la sortie sur 'vers'
                            NetworkAddress via = new NetworkAddress(route.getVia());
                            NetworkPlatformMessage envoi = new NetworkPlatformMessage(Parameters.SUPERVISOR, route.getVia());
                            envoi.addContent(Commands.CREATE_RELAY_CONNECTOR + " " + nom + " " + addressesChecker.getMyFirstAddress(via.getType()).getNormalizedAddress() + " " + vers);
                            emetteurDeMessages.postDirectMessage(envoi);
                        }
                    } catch (NoRouteException nr) { // on ne peut pas atteindre l'entree ou la sortie
                        System.err.println("Creation a connector: can't reach " + to.getNormalizedAddress());
                        aFinir = false;
                    }
                }
                if (aFinir) {
                    // lancer un processus pour cette sortie reseau
                    serviceEmissionReseau.createConnectorEmissionThread(nom, new NetworkAddress(sortie));
                    designation.setSortie(sortie);
                }
            } else { // la sortie du connecteur est devenue interne il faut y reconnecter le composant
                designation.setSortie(Commands.ES_INTERNE);
                ConnectedInput comp = composants.trouveComposantEntreeSur(nom);
                try {
                    model.osagaia.ControlUnit uc = (model.osagaia.ControlUnit) (ServicesRegisterManager.lookForService(comp.getName()));
                    uc.connectIUAfterRedirection(comp.getInput(), nom);
                } catch (ServiceClosedException sce) {
                    System.err.println("Component input reconnection: " + nom + " unknown component");
                }
            }
        }
    }

}
