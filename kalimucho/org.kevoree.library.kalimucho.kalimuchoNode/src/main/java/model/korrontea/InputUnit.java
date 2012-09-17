package model.korrontea;

import util.Parameters;
import model.StopBCException;
import platform.servicesregister.ServiceClosedException;
import network.connectors.EncapsulatedSample;

/**
 * Input Unit of a connector : offers services for:<br>
 * <ul>
 * <li>	Run / stop the IU
 * (when IU is stopped trying to read in it raises an exception
 * (class StopCMException) which will stop the BC doing data circulation in the connector)
 * <li>	Return the size of the buffer of the IU
 * <li>	Depose a sample in the buffer of the IU
 * (object of class Sample or inherited class)
 * <li>	Get a sample in the buffer of the IU
 * (object of class Sample or inherited class)
 * </ul>
 *
 * @author Dalmau
 */

// Classe generique de l'UE Korrontea
public class InputUnit  {

    /**
     * Symbolic name of the connector
     */
    protected String monNom; // nom symbolique du connecteur
    /**
     * Contol Unit of the connector
     */
    protected ControlUnit uc; // UC du connecteur (pour signaler les evenements)
    /**
     * Indicate if the Input Unit is running
     */
    protected boolean arret; // indique si l'UE est arretee
    /**
     * Indicate if the Input Unit is connected
     */
    protected boolean connecte; // indique si l'UE est connectee
    /**
     * Buffer of the Input Unit
     */
    protected ConnectorBuffer buffer; // buffer qui reeoit les echantillons en attente dans l'UE du connecteur

    /**
     * Construction of a Input Unit
     *
     * @param nom symbolic name of the connector
     */
    public InputUnit(String nom) {
        monNom = nom;
        uc = null;
        arret = true; // l'UE n'est pas encore demarree
        connecte = false; // l'UE n'a pas encore mis en place le sercice
        // creation du buffer de l'UE
        buffer = new ConnectorBuffer("entree", Parameters.WARNING_LEVEL_FOR_BUFFER, Parameters.SATURATION_LEVEL_FOR_BUFFER);
    }
    
    /**
     * Indicates if there is at least one sample available for input in this IU.
     * Takes into accont the filters (is one) associated to this input.
     * @return true if there is at least one sample available for input according to input class filter (if one)
     * @throws StopBCException if IU is stopped
     */
    public boolean isInputAvailable() throws StopBCException {
       if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
       else { // l'UE n'est pas arretee
           if (getBufferSize() != 0) return true;
           else return false;
       }
   }
    
   // Methode de depet d'un echantillon dans l'UE
    /**
     * Depose a sample in the buffer of the IU (object of class Sample or inherited class)
     *
     * @param ech sample to depose
     * @throws ServiceClosedException
     */
    public void deposeSample(EncapsulatedSample ech) throws ServiceClosedException {
        if (arret) throw new ServiceClosedException("UE Korrontea "+monNom+" deconnectee");
        else { // l'UE est en marche
            if (ech != null) { // lors d'une deconnexion on peut recevoir un echantillon null
               buffer.deposeSample(ech);
            }
        }
    }

    // Association de l'UC du connecteur e cette UE (pour signaler les evenements)
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
     * Start the Input Unit
     */
    final public void start() { // demarrage de l'UE
        buffer.start(); // demarrage du buffer
        arret = false;
    }

    /**
     * Stops the IU that will stop the BC if it tries to get an input
     */
    final public void stop() {
        buffer.stop(); // arret du buffer
        // Desenregistrement du service offert par l'UE
        // Si elle est connectee a un thread de reception ceci terminera ce thread
        arret=true; // quand ce booleen est a true tout appel a une lecture dans l'UE
            // provoque une exception de classe StopCMException qui sera utilisee
            // par le CM pour s'arreter
    }

    /**
     * Return the size of the buffer of the Input Unit
     *
     * @return size of the buffer of the Input Unit
     * (number of samples waiting in this buffer)
     */
    public int getBufferSize() { return buffer.getSize(); }

    /**
     * Reads an encapsulated sample in the input buffer of the connector
     * @return the first available encapsulated sample in the input buffer of the connector
     * @throws StopBCException if the IU is stopped this exception will stop the BC
     */
    public EncapsulatedSample readInInputUnit() throws StopBCException {
        EncapsulatedSample ech=null;
        if (arret) { // Si l'UE est arretee lever une exception pour arreter le CM
            throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
        }
        try { // recuperer l'echantillon dans le buffer
            ech = buffer.getSample();
        }
        catch (ServiceClosedException sce) { // le buffer a ete arrete
            throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
        }
        if (!arret) { // l'UE peut avoir ete arretee pendant le retrait de l'echantillon
            return ech; // echantillon recupere
        }
        else { // Si l'UE est arretee lever une exception pour arreter le CM
            throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
        }
    }

}
