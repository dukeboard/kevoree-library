
package network.connectors;

import java.util.Vector;

/**
 * Manages the semaphores associated to acknowledges for connectors.
 * Each connector is linked to a semaphore locked when data is sent
 * and unlocked when an acknowledge is received for this data.
 *
 * @author Dalmau
 */
public class DataAcquitmentManager {

    private Vector<DataAcquitmentSemaphore> acks;

    /**
     * Creates a collection of semaphores
     */
    public DataAcquitmentManager() {
        acks = new Vector<DataAcquitmentSemaphore>();
    }
    
    /**
     * Adds a new semaphore to manage.
     * @param name name of the new connector
     */
    public synchronized void addConnexionDesignation(String name) {
         acks.addElement(new DataAcquitmentSemaphore(name));
    }

    /**
     * Finds the semaphore of a given connector
     * @param name name of the connector
     * @return the semaphore associated to this connector (locked when data is sent, unlocked when ack is received)
     */
    public synchronized DataAcquitmentSemaphore findConnexionDesignation(String name) {
        int rang = trouverRangConnexionDesignation(name);
        if (rang == -1) return null;
        else return acks.elementAt(rang);
    }

    /**
     * Removes the semaphore of a given connector
     * @param name name of the connector
     */
    public synchronized void removeConnexionDesignation(String name) {
        int rang = trouverRangConnexionDesignation(name);
        if (rang != -1)  {
            (acks.elementAt(rang)).close();
            acks.removeElementAt(rang);
        }
    }

    private int trouverRangConnexionDesignation(String name) {
        boolean trouve = false;
        int i=0;
        while((!trouve)&&(i<acks.size())) {
            if ((acks.elementAt(i)).getName().equals(name)) trouve = true;
            else i++;
        }
        if (trouve) return i;
        else return -1;
    }

}
