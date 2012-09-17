package model.osagaia;

import java.util.Vector;
import model.interfaces.inputoutput.InputListener;
import util.streams.samples.Sample;
import model.StopBCException;

/**
 * Manages alls the listeners associated by a BC to some input units
 *
 * @author Dalmau
 */
public class InputListenersManager extends Thread {

    private Vector<InputUnit> todo;
    private boolean arret;
    private BCModel cm;

    /**
     * Creates the listeners manager
     * @param bc BC for which listeners are activated
     */
    public InputListenersManager(BCModel bc) {
        cm = bc;
        todo = new Vector<InputUnit>();
        arret = false;
    }

    /**
     * Depose a request for activation a listener
     * @param toRead the input unit on which data is avaiulable or null for the listener of platforms' replies
     */
    public synchronized void deposeListener(InputUnit toRead) {
        // Depot d'une demande de lancement d'un ecouteur d'evenements pour l'UE toRead
        todo.addElement(toRead);
        notifyAll();
    }

    private synchronized void waitForListenerToRun() {
        while ((!arret) && (todo.size()==0)) {
            try { wait(); }
            catch (InterruptedException ie) { arret = true; }
        }
    }

    /**
     * Runs the waiting listeners
     */
    @Override
    public void run() {
        while (!arret) {
            waitForListenerToRun();
            if (!arret) {
                try {
                    InputUnit toRead = todo.firstElement();
                    todo.removeElementAt(0);
                    if (toRead != null) {
                        InputListener ecouteur = toRead.getInputListener();
                        if (ecouteur != null) { // cette UE a un ecouteur d'evenements associe
                            Sample ech = toRead.readOnEvent(); // si oui tenter de le recuperer
                            if (ech != null) { // l'UE n'est pas connectee ou l'echantillon n'est pas de la bonne classe => pas d'echantillon a traiter
                                ecouteur.performSample(ech);
                            }
                        }
                    }
                }
                catch (StopBCException scm) {
                    cm.CM_terminatedByPF(); // terminaison du CM
                    arret = true;
                }
                catch (InterruptedException ie) {
                    arret = true;
                }
            }
        }
    }

    /**
     * Stops the listeners managers when the BC is stopped
     */
    public synchronized void stopThread() {
        arret = true;
        notifyAll();
    }

    /**
     * Indicates if the listeners manager is running
     * @return true if manager is running
     */
    public boolean isRunning() {
        return !arret;
    }

}
