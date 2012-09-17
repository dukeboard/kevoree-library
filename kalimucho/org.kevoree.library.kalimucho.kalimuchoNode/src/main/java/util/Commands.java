package util;

/**
 * Commands sent to the platform (strings).<br>
 * This class allows modifying the command in order to translate them.
 *
 * @author Dalmau
 */

// classe definissant les commandes utilisees par la PF kalimucho
// Permet de traduire ces commandes

public class Commands {

    // Commandes pouvant etre utilisees pour un deploiement/reconfiguration
    /**
     * Create component command
     */
    public static final String CREER_COMPOSANT = "CreateComponent";
    /**
     * Serialize a component command
     */
    public static final String ENVOYER_COMPOSANT = "SendComponent";
    /**
     * Run BC command (is this command usefull ?)
     */
    public static final String LANCER_COMPOSANT = "RunComponent";
    /**
     * Stop BC command (is this command usefull ?)
     */
    public static final String ARRETER_COMPOSANT = "StopComponent";
    /**
     * Create connector command
     */
    public static final String CREER_connecteur = "CreateConnector";
    /**
     * Remove component command
     */
    public static final String SUPPRIMER_COMPOSANT = "RemoveComponent";
    /**
     * Remove connector command
     */
    public static final String SUPPRIMER_connecteur = "RemoveConnector";
    /**
     * Read QoS command
     */
    public static final String LIREQDS = "ReadQoS";
    /**
     * Read host state command
     */
    public static final String LIRE_ETAT = "ReadState";
    /**
     * Read host state command
     */
    public static final String LIRE_ETAT_CONTENEUR = "ReadContainerState";
    /**
     * Send DNS command
     */
    public static final String ENVOYER_DNS = "SendDNS";
    /**
     * Disconnect BC input stream command
     */
    public static final String DECONNECTER_ENTREE_COMPOSANT = "DisconnectInputComponent";
    /**
     * Reconnect BC input stream command
     */
    public static final String RECONNECTER_ENTREE_COMPOSANT = "ReconnectInputComponent";
    /**
     * Reconnect BC output stream command
     */
    public static final String RECONNECTER_SORTIE_COMPOSANT = "DuplicateOutputComponent";

    // Commandes internes ne pouvant pas etre utilisees pour un deploiement/reconfiguration
    // Ces commandes sont echangees entre les PF pour completer l'execution d'une commande de deploiement/reconfiguration
    /**
     * Internal command for creating a part of a distributed connector
     */
    public static final String CREATE_PART_OF_CONNECTOR = "CreatePartOfConnector";
    /**
     * Internal command for removing a part of a distributed connector
     */
    public static final String REMOVE_PART_OF_CONNECTOR = "RemovePartOfConnector";
    /**
     * Internal command for creating a relay connector
     */
    public static final String CREATE_RELAY_CONNECTOR = "CreateRelayConnector";
    /**
     * Internal command for redirecting the input of a connector
     */
    public static final String REDIRECT_INPUT_OF_CONNECTOR = "RedirectInputOfConnector";
    /**
     * Internal command for redirecting the output of a connector
     */
    public static final String REDIRECT_OUTPUT_OF_CONNECTOR = "RedirectOutputOfConnector";
    // Commandes pour migrer des composants
    /**
     * Internal command for receiving a migrated component
     */
    public static final String RECEIVE_COMPONENT = "ReceiveComponent";

    // Valeurs utilisees dans les commandes
    /**
     * Input or output of a connector can be internal
     */
    public static final String ES_INTERNE = "internal";
    /**
     * Input or output of a connector can be null
     */
    public static final String ES_NULL = "null";
    /**
     * Input or output of a connector can be not used
     */
    public static final String ES_NOT_USED = "not_used";
}
