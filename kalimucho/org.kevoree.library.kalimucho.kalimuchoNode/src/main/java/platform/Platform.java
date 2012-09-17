package platform;

import platform.supervisor.Supervisor;
import platform.context.ContextManager;
import platform.containersregister.ContainersManager;
import network.AddressesChecker;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import network.platform.PingService;
import platform.plugins.PlatformPluginsLauncher;

/**
 * The platform Kalimucho, it runs:<br>
 * The containers for network communications<br>
 * The context manager<br>
 * The supervisor<br>
 * The plugins launcher
 * @author Dalmau
 */

// Classe de la PF Kalimucho
public class Platform {

    public Supervisor getSupervisor(){
        return sup;
    }


    private NetworkEmitterContainer emetteur;
    private NetworkReceptorContainer recepteur; // services de la plate-forme
    private PingService pingService;
    private ContainersManager gestionnaireDeConteneurs;
    private Supervisor sup;
    private ContextManager contexte;
    private PlatformPluginsLauncher plugins;

    /**
     * The Kalimucho PF.
     * Starts the supervisor and create the routing service.
     */
    public Platform() {
        // Lancer le service d'acces aux adresses de cet hote
        new AddressesChecker();
        // Creation du gestionnaire de contexte
        contexte = new ContextManager();
        // Lancer l'emetteur reseau, le recepteur reseau
        emetteur = new NetworkEmitterContainer();
        recepteur = new NetworkReceptorContainer();
        // lancement du gestionnaire de contexte
        contexte.startThread();
        pingService = new PingService();
        // Creation du superviseur de la PF
        gestionnaireDeConteneurs = new ContainersManager();
        sup = new Supervisor();
        // Lancement des plugins
        plugins = new PlatformPluginsLauncher(); // creer tous les plugins
        // La PF se met a l'ecoute des messages recus 
        sup.start(); // Lancement du superviseur de la PF
    }

    /**
     * Stops the supervisor and the routing service.
     */
    public void stop() {
        // Arret du service superviseur (detruit les connecteurs et envoie un message d'arret)
        contexte.stopThread();
        sup.stopThread();
        pingService.stopThread();
        // Arret des plugins
        plugins.stopAllPlugins();
        recepteur.stop();
        emetteur.stop();
    }

}
