package model.osagaia;

//import model.interfaces.control.IBusinessComponent;
import model.interfaces.control.IControlUnit;
import util.streams.samples.Sample;
import model.StopBCException;

/**
 * Control Unit of a BC container<br>
 * Service offering methods to:
 * <ul>
 * <li>	Connect/reconnect input or output to a connector.
 * Reconnections suspend the process on a semaphore until the connector is available.
 * They are done into a thread in order to not suspend the platform
 * when a connector is not yet created.
 * <li>	Disconnect input or output from the connector.
 * <li>	Run the BC
 * <li>	Stop the BC
 * <li>	Wait for the BC to terminate
 * <li>	Return the BCes QoS level
 * </ul>
 *
 * @author Dalmau
 */

// Classe de l'UC d'un composant osagaia
public final class ControlUnit implements IControlUnit  {

    private InputUnit[] ue; // UEs du composant
    private OutputUnit us; // US du composant
    private BCModel cc; // CM
    private String[] nomconnecteurSortie; // connecteurs auxquels il est connecte
    private String[] nomconnecteurEntree; // connecteurs auxquels il est connecte
    private boolean arret;
    private int indexEntree;
        
    /**
     * Construction of a Control Unit
     * @param inputConnector names of the connectors in input
     * @param outputConnector names of the connectors at output
     * @param inputUnit Input Units of the container
     * @param bc BC
     * @param outputUnit Output Unit of the container
     */
    public ControlUnit(String[] inputConnector, String[] outputConnector, InputUnit[] inputUnit, BCModel bc, OutputUnit outputUnit) {
        ue = (InputUnit[])inputUnit;
            for (int i=0; i<ue.length; i++) {
                if (ue[i] != null) ue[i].setControlUnit(this);
            }
        us = (OutputUnit)outputUnit;
        cc = bc;
        nomconnecteurEntree = inputConnector;
        nomconnecteurSortie = outputConnector;
        arret = false;
        indexEntree = 0;
	}

    // Methodes appelables par la PF
    //*********************
    // CONTROLE UC
    //*********************
    /**
     * Stops the UC when the BC is to be stopped
     */
    public synchronized void stop() {
        arret = true;
        notifyAll();
    }
    
    //*********************
    // CONTROLE UE et US
    //*********************
    /**
     * Connects the container to a connector as input<br>
     * This connection is done in a thread which waits until the connector is available
     *
     * @param numero number of IU to connect
     * @param inputConnector name of the connector for input
     */
    public synchronized void connectIU(int numero, String inputConnector) {
        if( ue[numero] != null) { // connecter l'UE
            // La connexion est faite dans un thread parce qu'elle est bloquante
            // La PF appelle cette methode mais ne doit pas etre bloquee
            new InputUnitConnectionThread(ue[numero], inputConnector).start();
        }
    }

    /**
     * Reconnectes au IU afer redirecting the connectors (migration)
     * @param numero number of the IU to reconnect
     * @param inputConnector connector to connet to this IU
     */
    public void connectIUAfterRedirection(int numero, String inputConnector) {
        if( ue[numero] != null) { // connecter l'UE
            // La connexion est faite dans un thread parce qu'elle est bloquante
            // La PF appelle cette methode mais ne doit pas etre bloquee
            new InputUnitConnectionAfterRedirectionThread(ue[numero], inputConnector, nomconnecteurEntree, nomconnecteurSortie).start();
        }
    }

    /**
     * Connects the container to a connector as output<br>
     * This connection is done in a thread which waits until the connector is available
     *
     * @param outputConnector name of the connector for output
     */
    public synchronized void connectOU(String outputConnector) {
        if( us != null) { // connecter l'US
            // La connexion est faite dans un thread parce qu'elle est bloquante
            // La PF appelle cette methode mais ne doit pas etre bloquee
            new OutputUnitConnectionThread(us, outputConnector).start();
        }
    }

    /**
     * Disconnects the container from one connector in input
     * @param numero number of Iu to disconnect
     */
    public void disconnectIU(int numero) {
        // deconnexion de l'entree du conteneur de CM
        // La PF appelle cette methode qui n'est pas bloquante
        if( ue[numero] != null) ue[numero].disconnection();
    }

    /**
     * Disconnects the container from the connectors in input and output.
     */
    public void disconnection() { // l'UE et l'US se deconnectent des connecteurs
        // La PF appelle cette methode qui n'est pas bloquante
        for (int i=0; i<ue.length; i++) {
            if( ue[i] != null) ue[i].disconnection();
        }
        if( us != null) us.disconnection();
 	}

    //***********************************
    // CONTROLE COMPOSANT METIER
    //***********************************
    /**
     * Starts the BC
     */
    public void start_CM() { // lancement du CM
        cc.start();
	}

    /**
     * Stops the BC
     */
    public void stop_CM() { // arret du CM via l'UE et l'US => exception
         cc.stop();
         for (int i=0; i<ue.length; i++) {
             if( ue[i] != null) {
                ue[i].stop(); // arreter l'UE => exception au CM s'il tente de lire
             }
         }
         if( us != null) {
            us.stop(); // arreter l'US => exception au CM s'il tente d'ecrire
         }
	}

    /**
     * Wait for BC to terminate
     */
    public void join_CM() { // attente de terminaison du CM
        cc.join();
	}

    /**
     * reads the actual BC's Qos level (as the BC indicates it by its method levelStateQoS())
     * @return the actual BC's Qos level (as the BC indicates it by its method levelStateQoS())
     */
    public float sendBCQoSLevel() { // envoi de la QdS e la PF
        return cc.readQoS(); // QdS transmise par le CM
	}

    // Methodes non appelables par la PF mais utilisees par le conteneur de CM
    /**
     *
     * Connects the container to connectors for input and output<br>
     * These connections are done in a threads which wait until the connectors are vailable
     */
    public void connection() { // l'UE et l'US decouvrent les connecteurs et s'y connectent
        // Le conteneur de CM appelle cette methode dans un thread car elle est bloquante
        for (int i=0; i<nomconnecteurSortie.length; i++) {
            if( us != null) connectOU(nomconnecteurSortie[i]);
        }
        for (int i=0; i<ue.length; i++) {
            if( ue[i] != null) connectIU(i, nomconnecteurEntree[i]);
        }
    }

    /**
     * Returns the first available input in any input of the container
     * @return the first available input in any input of the container
     * @throws StopBCException when the BC is to be stopped
     */
    public synchronized Sample getFirstInput() throws StopBCException {
        boolean trouve = false;
        int index = indexEntree;
        while ((!trouve) && (!arret)) {
            if (ue[index] != null) {
                if (ue[index].isInputAvailable()) trouve = true;
                else {
                    index++;
                    if (index == ue.length) index = 0;
                }
            }
            if ((!trouve) && (index == indexEntree)) {
                try { 
                    wait();
                }
                catch (InterruptedException ie) {}
            }
        }
        if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
        else {
            indexEntree = index +1;
            if (indexEntree == ue.length) indexEntree = 0;
            Sample ech = ue[index].readInInputUnit();
            ech.setInputNumber(index);
            return ech;
        }
    }
    
    // Methode utilisee par les buffers des connecteurs en entree pour signaler qu'ils ont quelque chose.
    /**
     * Used to indicate to the CU that an input is available
     * @param number number of the input on which data is available
     */
    public synchronized void setInputAvailable(int number) {
        if (number != -1) { // le buffer connait l'UE
            if (ue[number] != null) { // il exite bien une UE de ce numero
                cc.deposeListener(ue[number]); // appel de l'ecouteur d'evenements
            }
        }
        notifyAll(); // debloquer le CM s'il est bloque dans getFirstInput
    }

    /**
     * The BC controled by this CU
     * @return the BC controled by this CU
     */
    public BCModel getCM() { return cc; }

}
