package model.korrontea;

import network.connectors.EncapsulatedSample;
import util.Parameters;
import model.StopBCException;
import platform.servicesregister.ServiceClosedException;
import network.connectors.ConnectorEmissionThread;

/**
 * Class of the Output Unit of a connector<br>
 * Offers services for:
 * <ul>
 * <li>	Run / stop the OU (when OU is stopped trying to write in it raises an exception
 * (class StopCMException) which will stop the BC doing data circulation in the connector)
 * <li>	Accept a connection from an IU Osagaia or from a reseau emitter thread
 * <li>	Accept a disconnection from an IU Osagaia or from a reseau emitter thread.
 * That allows a consumer suspended waiting a sample in the OU to be freed
 * without a sample (used when a BC or a connector is removed).
 * <li>	Return the size of the buffer of the OU
 * <li>	Depose a sample in the buffer of the OU
 * (object of class Sample or inherited class)
 * <li>	Get a sample in the buffer of the OU
 * (object of class Sample or inherited class)
 * </ul>
 *
 * @author Dalmau
 */

// Classe generique de l'US Korrontea
public class OutputUnit  {

    /**
     * Symbolic name of the connector
     */
    protected String monNom; // nom symbolique du connecteur
    /**
     * Contol Unit of the connector
     */
    protected ControlUnit uc; // UC du connecteur (pour signaler les alarmes)
    /**
     * Indicate if the Output Unit is running
     */
    protected boolean arret; // indique si l'US est arretee
    /**
     * Buffer of the Output Unit
     */
    protected ConnectorBuffer buffer; // buffer qui reeoit les echantillons en attente dans l'US du connecteur
    private ConnectorEmissionThread sortieReseau;
    private model.osagaia.ControlUnit ucCMEnSortie;

    /**
     * Construction of a Input Unit
     *
     * @param nom symbolic name of the connector
     */
    public OutputUnit(String nom) {
        monNom=nom;
        sortieReseau = null;
        ucCMEnSortie = null;
        uc=null;
        arret = true; // l'US n'est pas encore demarree
        // creation du buffer de l'US
        buffer = new ConnectorBuffer("sortie", Parameters.WARNING_LEVEL_FOR_BUFFER, Parameters.SATURATION_LEVEL_FOR_BUFFER);
    }

    // Methode de retrait d'un echantillon dans l'US
    /**
     * Get a sample in the buffer of the OU (object of class Sample or inherited class)
     *
     * @return the sample read
     * @throws ServiceClosedException when the connector is not available
     */
    public EncapsulatedSample getSample() throws ServiceClosedException {
        // Si l'US est arretee lever une exception
        if (arret) throw new ServiceClosedException("US Korrontea "+monNom+" deconnectee");
        attendreConnecte();
        // ce retrait peut lever une exception ServiceClosedException lors d'une deconnexion
        return buffer.getSample();
    }

    private synchronized void attendreConnecte() {
        while ((ucCMEnSortie == null) && (sortieReseau == null)) {
            try { wait(); }
            catch(InterruptedException ie) {}
        }
    }

    /**
     * Accept a connection from an IU Osagaia or from a network emitter thread
     * @param consumer the Osagaia IU or the emitter thread that connects
     * @param number number of the connected IU
     */
    // Cette methode doit s'executer en exclusion mutuelle de celle de deconnexion et de la conversion d'echantillon
    public synchronized void consumerConnection(Object consumer, int number) { // methode utilise par le consommateur pour indiquer qu'il est connecte
        if (consumer instanceof model.osagaia.ControlUnit) {
            ucCMEnSortie = (model.osagaia.ControlUnit)consumer;
            buffer.consumerConnection(ucCMEnSortie, number);
            sortieReseau = null;
        }
        else {
            buffer.consumerConnection(null, number);
            sortieReseau = (ConnectorEmissionThread)consumer;
            ucCMEnSortie = null;
        }
        notifyAll(); // debloquer le consommateur en attente dans getSample (appel de attendreConnecte)
    }

    /**
     * Accept a disconnection from an IU Osagaia or from a reseau emitter thread
     */
    // Cette methode doit s'executer en exclusion mutuelle de celle de connexion et de la conversion d'echantillon
    public synchronized void consumerDisconnection() {// methode utilise par le consommateur pour indiquer qu'il se deconnecte
        buffer.consumerDisconnection();
        sortieReseau = null;
        ucCMEnSortie = null;
    }

    /**
     * Accept a connection from a listener
     * @param uc control unit
     * @param number number of the connected IU
     */
    public void listenerConnection(model.osagaia.ControlUnit uc, int number) {
        // methode utilise par le consommateur pour indiquer q'un ecouteur d'entree est connecte
        buffer.listenerConnection(uc, number);
    }
    
    // Association de l'UC du connecteur e cette US (pour signaler les evenements)
    /**
     * Associate the Control Unit of the connector
     *
     * @param uc Control Unit of the connector
     */
    final public void setUC(ControlUnit uc) {
        this.uc=uc;
        buffer.setUC(uc);
    }

    /**
     * Start the Output Unit
     */
    final public void start() { // lancement de l'US
        buffer.start(); // lancement du buffer
        arret = false; // l'US est lancee
    }

    /**
     * Stops the OU of the connectror
     */
    final public void stop() { // Arret de l'US.
        buffer.stop(); // arret du buffer
        arret=true; // quand ce booleen est a true tout appel e une ecriture dans l'US
            // provoque une exception de classe StopCMException qui sera utilisee
            // par le CM pour s'arreter
        // Desenregistrer le service offert par l'US
        // Si elle est connectee e un thread d'emission ceci terminera ce thread
    }

    /**
     * Return the size of the buffer of the Input Unit
     *
     * @return size of the buffer of the Input Unit
     * (number of samples waiting in this buffer)
     */
    public int getBufferSize() { return buffer.getSize(); }
    
    /**
     *  Returns the output buffer of the connector
     * @return the output buffer of the connector
     */
    public ConnectorBuffer getBuffer() { return buffer; }

    // Methode permettant au CM d'ecrire un echantillon dans l'US
    // Utilisable par les CM qui doivent faire des traitements sur le flux
    // Cette methode peut lever une exception si le CM doit etre arrete
    /**
     * Write an encapsulated sample or a sample in the output connector's buffer
     * @param ech the encapsulated sample to put in the connector's buffer
     * @throws StopBCException
     */
    public void writeInOutputUnit(EncapsulatedSample ech)  throws StopBCException {
        if (arret) { // Si l'US est arretee lever une exception pour arreter le CM
            throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
        }
        else {
            buffer.deposeSample(ech); // deposer l'echantillon dans le buffer
        }
    }

}