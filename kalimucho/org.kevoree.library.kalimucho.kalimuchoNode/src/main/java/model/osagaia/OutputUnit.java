package model.osagaia;

import util.streams.samples.Sample;
import model.StopBCException;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.Commands;
import network.connectors.EncapsulatedSample;
import java.util.Vector;


/**
 * Osagaia containeres Output Unit<br>
 * Offers methods to:<br>
 * <ul>
 * <li>	Connect to the output connector.
 * That means waiting until this connector is available.
 * <li> Disconnect from the output connector
 * <li>	Stop => raises an exception to the BC at the next try
 * of writing to output stream (that will stop the BC)
 * <li>	Write a sample (class Sample or inherited)
 * in the output stream. Suspends the BC on a  semaphore until either:
 * <ul>
 * <li>	Sample is sent to the connector
 * <li>	Output connector has been removed
 * <li>	BCes output has been disconnected
 * <li>	Output  unit has been stopped
 * </ul></ul><br>
 * When BCes output is disconnected or when output connector is removed,
 * the output unit waits for a reconnection.
 *
 * @author Dalmau
 */

// Clase de l'US Osagaia
public class OutputUnit {

    private Vector<String> nomsconnecteursSortie; // nom symbolique de ce connecteur
    private boolean arret; // indique si l'US est arretee
    private boolean connecte; // indique si l'US est connectee

    /**
     * Construction of an Output Unit
     */
    public OutputUnit() {
        arret=false; // l'US est prete e fonctionner
        connecte=false; // l'US n'est pas connectee
        nomsconnecteursSortie = new Vector<String>();
    }

    /**
     * Stops the output Unit<br>
     * raises an exception to the BC at the next try
     * of writing to output stream (that will stop the BC)
     */
    public synchronized void stop() {
         /* utilise pour que l'US cesse de fonctionner
          * quand ce booleen est a true tout appel e une ecriture dans l'US
          * provoque une exception de classe StopCMException qui sera utilisee
          * par le CM pour s'arreter
          */
        arret=true;
        notifyAll(); // debloquer le CM s'il est en attente de reconnexion
    }

    /**
     * Connects the Output Unit to a connector
     *
     * @param nom name of the connector to connect with
     */
    public synchronized void connection(String nom) {
        if (!nom.equals(Commands.ES_NOT_USED)) {
            // ouverture du service de l'UE du connecteur en sortie
            model.korrontea.ControlUnit accesUE = (model.korrontea.ControlUnit)ServicesRegisterManager.platformWaitForService(nom);
            nomsconnecteursSortie.addElement(nom);
            connecte=true; // l'US est connectee
            notifyAll(); // debloquer le CM s'il est en attente de reconnexion
        }
    }

    /**
     * Disconnects the Output Unit from all connectors
     */
    public synchronized void disconnection() {
        nomsconnecteursSortie.removeAllElements();
        connecte=false; // l'US n'est plus connectee
    }
    
    /**
     * Writes a sample produced by the BC in the OU of the BC container
     * @param o the sample produced by the BC
     * @throws StopBCException if the OU is stopped, that will stop the BC if it tries to produce a sample
     */
    public void writeInOutputUnit(Sample o) throws StopBCException {
        synchronized(this) { // semaphore debloque par une reconnexion ou un arret de l'US
           while (!connecte && !arret){ // attendre d'etre connecte ou arrete
               try { wait(); } catch(InterruptedException ie) {}
           }
        }
        // si l'US est arretee lever une exception pour arreter le CM
        if (arret) throw(new StopBCException("CM arrete lors d'une ecriture dans l'US"));
        else { // l'US n'est pas arretee
            boolean nonEnvoye=true;
            while((nonEnvoye) && connecte && (!arret)) { // tenter d'envoyer l'echantillon dans un connecteur en sortie
                            // l'US peut ne pas pouvoir envoyer l'echantillon si aucun connecteur n'existe
                int i = 0;
                while (i<nomsconnecteursSortie.size()) {
                    String nomconnecteurSortie = nomsconnecteursSortie.elementAt(i);
                    try { // essayer de l'envoyer
                        model.korrontea.ControlUnit accesUE = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurSortie);
                        model.korrontea.InputUnit serveur = accesUE.getIU();
                        o.setExpeditor("localhost");
                        serveur.deposeSample(new EncapsulatedSample(o));
                        nonEnvoye=false; // l'envoi a ete fait
                        i++; // on essaye le connecteur suivant
                    }
                    catch (ServiceClosedException ace) { // Le connecteur n'existe plus
                        nomsconnecteursSortie.removeElementAt(i); // on l'enleve
                    }
                }
                if (nonEnvoye) { // on n'a reussi e l'envoyer sur aucun connecteur
                    connecte=false; // l'US n'est plus connectee => attendre une reconnexion ou un arret
                    synchronized(this) { // semaphore debloque par une reconnexion ou un arret de l'US
                       while (!connecte && !arret){ // attendre d'etre connecte ou arrete
                           try {
                               wait();
                           } catch(InterruptedException ie) {}
                       }
                    }
                }
            }
            // si l'US a ete arretee pendant une tentative de depet de l'echantillon il faut arreter le CM
            if (arret) throw(new StopBCException("CM arrete lors d'une ecriture dans l'US"));
        }
     }



}
