package model.korrontea;

import model.interfaces.control.IBusinessComponent;
import model.StopBCException;
import network.connectors.EncapsulatedSample;
/**
 * Abstract class from which data circulation BCs of connectors inherit.
 * In order to create a different data circulation politic in a connector
 * it is possible to write a specific BC which extends this class
 * and has the same structure than an Osagaia BC.
 * This class offers methods for reading and writing samples.<br>
 * This class offers methods to:
 * <ul>
 * <li> Stop the BC
 * <li>	Wait for the BC to terminate
 * <li>	Read and write samples
 * </ul>
 * The model of a Korrontea BC is a thread which:
 * <ul>
 * <li>	Calls the init method of the BC
 * <li>	Calls the run_CM method of the BC (infinite loop)
 * <li>	Calls the destroy method of the BC if an exception stops the BC.
 * This exception is raised when the BC try to read in the IU
 * or to write in the OU as the platform is removing the connector.
 * </ul>
 * 
 * @author Dalmau
 */

// Classe abstraite permettant de creer ses propres CM de transfert de flux
public abstract class ConnectorBCModel implements IBusinessComponent, Runnable {

    private Thread current = null; // thread java contenant le CM
    private InputUnit ue = null; // lien avec l'UE
    private OutputUnit us = null; // lien avec l'US
    private boolean enMarche; // indique si le CM de transfert est en marche

    // La methode run_CM doit etre surchagee elle contient la boucle de transfert de flux
    /**
     * This method need to be overwriten<br>
     * It is an infinite loop that transfers data inside the connector<br>
     * Called after init and before destroy
     *
     * @throws StopBCException  when the BC is stopped by the platform
     */
    abstract public void run_CM() throws StopBCException;
    
    // La methode levelStateQdS doit etre surchagee pour retourner la QdS du CM de transfert de flux
    /**
     * Method called by the platform to get the BC QoS level (0 to 1)
     * @return the BC's QoS level (0 for worst to 1 for best)
     */
    abstract public float levelStateQoS();

    // Methodes qui peuvent etre reecrites par le concepteur
    // par defaut elles ne font rien
    /**
     * Method called when the BC is started (to be writed)
     */
    public void init() {} // appelee e la creation du CM de transfert
    
    /**
     * Method called when the BC is stopped (to be writed)
     */
    public void destroy() {} // appelee e la suppression du CM de transfert
    
    /**
     * Test if a sample is available on the input stream<br>
     *
     * @return true if a sample is available
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public boolean isSampleAvailableOnInput() throws StopBCException {
        // Regerder si un echantillon est disponible dans l'UE
        return ue.isInputAvailable();
    }
    /**
     * Waits for a sample of the class defined by the <b>setInputClassFilter</b> method in the input stream.
     * This method suspends the BC until a sample assignment-compatible with this class is available.<br>
     * The name of the class of samples to be read is the one previously defined by the
     * <b>setInputClassFilter</b> method of the BC. If the sample actually present in the connector
     * is not assignment-compatible with this class it is discarded and
     * the Input Unit waits for another one.
     * 
     * @return the sample read
     * @throws StopBCException when the BC is stopped by the platform
     */
    final public EncapsulatedSample readSample() throws StopBCException {
        // lire un echantillon dans l'UE
        return ue.readInInputUnit();
    }
    
    /**
     * Writes a sample to the output stream
     *
     * @param sample the sample to write
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public void writeSample(EncapsulatedSample sample) throws StopBCException {
        // ecrire un echantillon dans l'US
        us.writeInOutputUnit(sample);
    }

    /**
     * Calls init, then run_CM. When the BC is stopped calls destroy
     */
    final public void run() {// methode d'execution d'un CM de transfert
        // le lancement du CM appelle init puis run_CM
        // en cas d'exception provoquee par l'UE ou l'US appel de destroy
        init();
        try { run_CM(); }
        catch (StopBCException scme) {
            destroy();
            enMarche=false; // le CM est arrete
        }
    }

    /**
     * Starts the BC of the connector
     */
    final public void start() { // lancement du CM (thread)
        enMarche=true;
        if (current==null) {
            current = new Thread(this);
            current.start(); // le CM est en marche
        }
    }

    /**
     * Waits for the connector's BC to terminate
     */
    final public void join() { // attente de terminaison du CM
        if (current != null) {
            while (enMarche) {} // attendre la terminaison du run_CM
        }
    }

    /**
     * Set the input and ouput units of this connector's BC
     * @param inputUnit input unit
     * @param outputUnit output unit
     */
    final public void setInputOutputUnits(InputUnit inputUnit, OutputUnit outputUnit) {
        // association du CM de transfert e son UE et son US
        this.ue = inputUnit;
        this.us = outputUnit;
    }

    // Methodes permettant au CM de transfert d'acceder e son UE et son US
    /**
     * Returns the Input Unit of the connector
     *
     * @return the Input Unit of the connector
     */
    final public InputUnit getIU() { return (InputUnit)ue; }
    /**
     * Returns the Output Unit of the connector
     *
     * @return the Output Unit of the connector
     */
    final public OutputUnit getOU() { return (OutputUnit)us; }

}
