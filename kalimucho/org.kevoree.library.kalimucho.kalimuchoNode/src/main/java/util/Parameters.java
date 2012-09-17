package util;

/**
 * This static class contains the parameters for the Kalimucho platform:<br>
 * - network ports numbers<br>
 * - timeouts for network services<br>
 * - timeout for waiting a BC to stop<br>
 * - rate for host state measurement<br>
 * - threesholds for alarm on host's state
 * - connectors buffers alarm and saturation levels<br>
 * - names of services of the platform
 *
 * @author Dalmau
 */

// classe definissant les constantes utilisees par la PF kalimucho

public class Parameters {

    // La ligne qui suit permet de simuler une non connectivite avec un hote
    // Elle contient la fin de l'adresse IP de cet hote
    // Tout message recu de cet hote sera ignore (en direct comme en broadcast)
    // Pour n'exclure aucun hote y metre **
    /**
     * For test pupose only. This allow to simulate a non connectivity whith a specified host.
     */
    public static final String NETWORK_EXCLUSION = "**";

    /**
     * Set to true if broadcast is available on this host
     * Otherwise a referent platform is needed, normally broadcasted messages are sent to this referent
     */
    public static final boolean BROADCAST_AVAILABLE = true;
    /**
     * Server address of referent host used when no broadcast is available
     */
    public static String REFERENT_SERVER_IP_ADDRESS = "192.168.0.30";


    // time out pour connexion de socket en IP (en ms)
    /**
     * Time out for connecting a client socket
     */
    public static final int IP_SOCKET_CONNEXION_TIME_OUT = 400;

    // Numeros de ports utilises par la PF
    /**
     * IP Port number for platform to special deployment platform communications
     */
    public static final int PORT_IP_DEPLOYMENT_PF = 1098; // numero de port utilise par la PF de deploiement en IP
    /**
     * IP Port number for platform to platform communications
     */
    public static final int PORT_IP_COMMANDS_PF = 1099; // numero de port utilise par la PF en IP pour les commandes
    /**
     * UDP Port number for multicast messages
     */
    public static final int PORT_UDP_BROADCAST_PF = 4098;
    /**
     * Multicast address  for the plarform
     */
    public static final String UDP_BROADCAST_GROUP_PF = "230.20.20.22";
    /**
     * UDP messages maximal size
     */
    public static final int MAX_BROADCAST_MESSAGE_SIZE = 1400;
    /**
     * IP Port number for connector's data
     */
    public static final int PORT_IP_DATA_CONNECTORS = 1097; // numero de port utilise par la PF en IP pour les donnees
    /**
     * IP Port number for connector's acknolegments
     */
    public static final int PORT_IP_ACK_CONNECTORS = 1096;

    // Constantes pour le routage
    /**
     * Maximum time for waiting a reply to a multicast find route message before retrying (in ms).
     * The fists try waits for this delay
     * The second try wait for two times this delay
     * So if NUMBER_RETRIES_FOR_ROUTE = 3 the total waiting time for a reply is 6*MAXIMAL_WAIT_FOR_RETRY_ROUTE
     */
    public static final int MAXIMAL_WAIT_FOR_RETRY_ROUTE = 700; // delai max d'attente de reponse au routage (en ms)
    /**
     * Maximal number of retries done when finding a route
     */
    public static final int NUMBER_RETRIES_FOR_ROUTE = 3; // nombre de tentatives pour trouver une route

    // Constantes pour la recherche de classes
    /**
     * Maximum time for waiting a reply to a multicast find class message befor retrying (in ms).
     */
    public static final int MAXIMAL_WAIT_FOR_RETRY_CLASSFINDER = 1000; // delai max d'attente de reponse a une demande de classe (en ms)
    /**
     * Maximal number of retries done when finding a class
     */
    public static final int NUMBER_RETRIES_FOR_CLASSFINDER = 3; // nombre de tentatives pour trouver une classe

    // Constantes pour le ping
    /**
     * Ping message
     */
    public static final String PING_MESSAGE = "__PING__";
    /**
     * Ping reply message
     */
    public static final String PING_REPLY_MESSAGE = "__PING__REPLY__";
    /**
     * Maximun delay (in ms) for waiting to a ping reply.
    */
    public static final int MAXIMAL_WAIT_FOR_PING = 600;

    // Constantes pour le DNS
    /**
     * Maximun time (in s) for keeping an host in the DNS
    */
    public static final int MAXIMAL_TIME_TO_LIVE = 600; //10 min
    /**
     * Rate time (in s) for updating the DNS (time to live of host is decremented at this rate)
    */
    public static final int DNS_UPDATE_RATE = 10; // 10 s
    /**
     * Rate time (in s) for sending the DNS to other hosts
    */
    public static final int DNS_DIFFUSION_RATE = 300; // 5 min

    // Constantes pour les composants metier
    /**
     * Maximun delay (in ms) for waiting to a BC to terminate when a component is destroyed
    * After this delay the BC is destroyed whithout allowing it to execute the destroy method.
    */
    public static final int MAXIMAL_WAIT_FOR_CM = 1000;

    // Constantes de surveillance de l'hote
    /**
     * Rate (in ms) for the platform to measure the state of the host (memory, CPU ...)
     */
    public static final int LOAD_MEASURE_RATE = 1000; // en ms
    /**
     * Number of values from which are calculated the average values
     * of CPU load and free memory for raising an alarm.<br>
     */
    public static final int NUMBER_MEASURES_FOR_AVERAGE = 10;
    /** 
     * Level used by connectors to:<br>
    * - Raise an alarm to the PF when the buffer is full
    */
    public static final int WARNING_LEVEL_FOR_BUFFER = 20;
    /**
    * The connectors starts loosing data when the buffer size is over this value
    * (an other alarm is raised)
    */
    public static final int SATURATION_LEVEL_FOR_BUFFER = 40;
    /**
    * Free memory level (in %) to raise an alarm
    */
    public static final int MEMORY_ALARM_LEVEL = 10; // alarme quand memoire libre < 10%
    /**
    * Free memory level (in %) to allow a new alarm to be raised
    */
    public static final int END_MEMORY_ALARM_LEVEL = 60;
    /**
    * CPU load level (in %) to raise an alarm
    */
    public static final int CPU_LOAD_ALARM_LEVEL = 80; //  alarme quand charge CPU > 80%
    /**
    * CPU load level (in %) to allow a new alarm to be raised
    */
    public static final int END_CPU_LOAD_ALARM_LEVEL = 60;
    /**
    * Battery level (in %) to raise an alarm
    */
    public static final int BATTERY_ALARM_LEVEL = 20; //  alarme quand batterie < 20%
    /**
    * Battery level (in %) to allow a new alarm to be raised
    */
    public static final int END_BATTERY_ALARM_LEVEL = 30;

    // Constantes liees aux traces
    /**
    * If true traces are done when executing Kalimucho's application
    */
    public static final boolean DEBUG_APPLICATION = false; // true => traces de l'application
    /**
    * If true time measurement is done when executing Kalimucho's command
    */
    public static final boolean TIME_MEASURE = false; // true => mesures de temps

    // Constantes relatives au depot de composants
    /**
     * Path for the components' code repository
     */
    public static final String COMPONENTS_REPOSITORY = "depotComposants"; // chemin du depot de donnees

    // Noms des services de la PF
    /**
     * Name of the service that collects all network addresses of the host
     */
    public static final String NETWORK_ADDRESSES = "ADRESSES_RESEAU";
    /**
     * Name of the service that receives connectors replies to SYNC messages
     */
    public static final String CONNECTORS_ACKNOWLEDGE = "ACQUITEMENTS_RESEAU";
    /**
     * Name of the service that receives SYNC messages from connectors and creates mailboxes for connectors
     */
    public static final String CONNECTORS_DATA_MAILBOX = "BAL_CONNECTEURS";
    /**
     * Name of the service that manages all network emissions (for the application and for the platform)
     */
    public static final String NETWORK_EMISSIONS_CONTAINER = "EMETTEUR_RESEAU";
    /**
     * Name of the service that manages all network receptions (for the application and for the platform)
     */
    public static final String NETWORK_RECEPTIONS_CONTAINER = "RECEPTEUR_RESEAU";
    /**
     * Name of the service that manages context information (local and distant)
     */
    public static final String CONTEXT_MANAGER = "GESTIONNAIRE_CONTEXTE";
    /**
     * Name of the service that manages application containers
     */
    public static final String CONTAINERS_MANAGER = "GESTIONNAIRE_CONTENEURS";
    /**
     * Name of the service that manages reconfigutations
     */
    public static final String SUPERVISOR = "SUPERVISEUR";
    /**
     * Name of the service that starts and stops the platform's plugins
     */
    public static final String PLUGINS_LAUNCHER = "PLUGINS_LAUNCHER_SERVICE";
    /**
     * Name of the service that find routes on the networks
     */
    public static final String NETWORK_ROUTING_SERVICE = "NEIGHBORHOOD_SERVICE";
    /**
     * Name of the ping like service used to test direct connexions
     */
    public static final String NETWORK_PING_SERVICE = "PING_SERVICE";
    /**
     * Name of the service that find the byte code for classes on the network (repositories)
     */
    public static final String CLASS_FINDER_SERVICE = "CLASSFINDER_SERVICE";
    /**
     * Name of the service that manages the byte code repository
     */
    public static final String JAR_REPOSITORY_MANAGER = "JAR_REPOSITORY_MANAGER";
    /**
     * Name of the service that manages the Platform DNS
     */
    public static final String KALIMUCHO_DNS_MANAGER = "KALIMUCHO_DNS_MANAGER";
    /**
     * Name of the service that allows BC to send command to local or distant platforms and get replies
     */
    public static final String APPLICATION_COMMAND_SERVICE = "BCCommandSenderPlugin";

}
