package model.korrontea;

import model.interfaces.control.IBusinessComponent;
import model.interfaces.control.IControlUnit;
import util.Parameters;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import platform.context.ContextManager;
import platform.context.ContextInformation;

/**
 * Class of the Control Unit of a connector container<br>
 *  Offers services for:
 * <ul>
 * <li>	Raise an alarm to the platform (method used by the buffers of the IE and OE
 * when a saturation occurs or when they become fluent after a saturation)
 * <li>	Run the BC (circulation of data into the connector)
 * <li>	Stop the BC (no more circulation of data into the connector)
 * <li>	Wait for BC to terminate
 * <li>	Send the connectores level of QoS (volume of data waiting in the buffers)
 * </ul>
 *
 * @author Dalmau
 */

// Classe de l'UC Korrontea
public final class ControlUnit implements IControlUnit {

    private String monNom; // nom symbolique du connecteur
    private InputUnit ue = null; // UE
    private OutputUnit us = null; // US
    private IBusinessComponent cc = null; // CM
    private ConnectorContainer conteneur;
    private ContextManager pf; // service auquel sont signalees les alarmes
    private int compte;
    private float debitMoyen;

    /**
     * Constuction of the Control Unit
     *
     * @param nom name of the connector (symbolic name used to appoint it by the platform)
     * @param ue Input Unit of the connector
     * @param cc Business component of the connector
     * @param us Output Unit of the connector
     * @param cont The container of this CU
     */
    public ControlUnit(String nom, InputUnit ue, IBusinessComponent cc, OutputUnit us, ConnectorContainer cont) {
        monNom=nom;
        conteneur = cont;
        compte = 0;
        debitMoyen = 0F;
        this.ue = (InputUnit)ue;
        this.ue.setUC(this); // associer l'UC e l'UE pour les evenements
        this.us = (OutputUnit)us;
        this.us.setUC(this); // associer l'UC e l'US pour les evenements
		this.cc = cc;
        try {       // Acces au service de la PF
           pf = (ContextManager)(ServicesRegisterManager.lookForService(Parameters.CONTEXT_MANAGER));
        }
        catch (ServiceClosedException sce) {
            System.err.println("UC of component "+monNom+" can't get acces to the plaform service");
        }
    }

    /**
     * Return the symbolic name of the container
     *
     * @return symbolic name of the container
     */
    public String getName() { return monNom; }

    /**
     * Return the Input Unit of the container
     *
     * @return the Input Unit of the container
     */
    public InputUnit getIU() { return ue; }

    /**
     * Return the Output Unit of the container
     *
     * @return the Output Unit of the container
     */
    public OutputUnit getOU() { return us; }

    /**
     * Returns the container in which this CU is
     * @return  the container in which this CU is
     */
    public ConnectorContainer getContainer() { return conteneur; }

    // Methode appelee pour lever des evenements d'etat du buffer
    /**
     * Method used par the Control Unit to send an alarm to the platform
     *
     * @param alarm alarm message to the platform
     * (connector full, connector saturated, connector fluent again)
     */
    public void raiseAlarm(String alarm) { // Envoi d'un evenement via la PF
       pf.signalEvent(new ContextInformation("uc "+monNom+" "+alarm)); // signaler l'alarme e la PF
    }

    // Methodes appelables par la PF (services)

	//*********************
	// CONTROLE UE et US
	//*********************

    /**
     * Start the Input and Output Units of the connector
     */
    public void startUEandUS() { // Methode pour demarrer l'UE et l'US
        us.start();
        ue.start();
    }

    // Methode pour arreter l'UE et l'US
    /**
     * Stop the Input and Output Units of the connector
     */
    public void stopUEandUS() {
        // L'arret de l'UE et de l'US provoquera celui du CM de transfert de flux (par Exception)
        us.stop(); // arreter l'US => exception au CM s'il tente d'ecrire
        ue.stop(); // arreter l'UE => exception au CM s'il tente de lire
    }

     //***********************************
    // CONTROLE COMPOSANT METIER
    //***********************************

    /**
     * Start the BC of the connector
     */
    public void start_BC() { // Methode pour lancer le CM de transfert de flux (thread)
        if (cc!=null) cc.start();
    }

    /**
     * Wait for terminaison of the BC of the connector
     */
    public void join_BC() { // attente de terminaison du CM de transfert de flux
        if (cc!=null) cc.join();
    }

     /**
     * Return the QoS level of the connector
      *
      * @return number of samples staying in the connector
     */

    public float sendBCQoSLevel() { // renvoi de la QdS e la PF (pour l'instant c'est la taille des buffers)
/*        if (cc!=null) return cc.levelStateQdS(); // celle retournee par le CM
        else return 1.0f; // ou 1 s'il n'y a pas de CM (connecteur e entree par reseau)
*/
        return ((float)(ue.getBufferSize()+us.getBufferSize()));
    }

    /**
     * Update the average network traffic of the connector
     * @param debit number of bytes (received/sent) by this connector since last measure
     */
    public void setDebit(int debit) {
        debitMoyen = ((debitMoyen*compte)+debit)/(compte+1);
        compte++;
    }

    /**
     * Returns the average network traffic of the connector
     * @return the actual average network traffic of the connector
     */
    public float getDebitMoyen() { return debitMoyen; }

}
