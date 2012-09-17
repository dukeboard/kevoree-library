package model.korrontea;

import java.util.Vector;
import network.connectors.EncapsulatedSample;
import platform.servicesregister.ServiceClosedException;
import util.streams.samples.Sample;

/**
 * Buffer manager in a producer/consumer mode with semaphores.<br>
 * The buffer watches the number of samples pending.<br>
 * When this number is over the warning level defined in util.Parameters it raises,
 * via the CU, an alarm for the platform.<br>
 * When this number is over the saturation level defined in util.Parameters it raises,
 * via the CU, an alarm for the platform and starts discarding older samples.<br>
 * When this number, after been over warning level, is only 1, it raises,
 * via the CU, an alarm for the platform indicating that data circulation is now fluent.<br>
 * Offers methods to:
 * <ul>
 * <li>	Depose a sample in the buffer (object of class Sample or inherited class)
 * <li>	Get a sample in the buffer (object of class Sample or inherited class).
 * The requester is suspended on a  semaphore until:
 * <ul>
 * <li>	The requested sample is available
 * <li>	The connector is removed
 * <li>	The requester asks the buffer for a disconnection
 * </ul>
 * <li>	Accept a consumer connection
 * <li>	Accept a consumer disconnection.
 * <li>	Start
 * <li>	Stop (all suspended consumers are unlocked)
 * </ul>
 *
 * @author Dalmau
 */

// Classe pour gerer les buffers des connecteurs en mode producteur/consommateur
// Un evenement est signale e l'UC quand le buffer atteint le seuil d'alarme
// Un evenement est signale e l'UC quand le buffer est sature
// Un evenement est signale e l'UC quand le buffer est redevenu fluide
// Les seuils d'alarme et de saturation sont definis dans util.Parametres
public final class ConnectorBuffer {

    private Vector<EncapsulatedSample> buffer; // buffer qui reeoit les echantillons en attente dans le connecteur
    private boolean consommateurConnecte; // indique si le consommateur de ces buffers est connecte
    private int seuilAlarme, seuilSaturation; // seuils de declenchement des evenements vers la PF
    // les booleens qui suivent permettent de ne pas lever plusieurs fois les alarmes
    // L'alarme est levee quand on depasse le seuil d'alarme puis desamorcee
    // Elle est reamorcee quand on tombe e 1
    // La saturation est signalee quand on sature le buffer puis desamorcee
    // Elle est reamorcee quand on tombe au dessous du seuil d'alarme
    private boolean alarmeSignalee, saturationSignalee;
    private boolean arret; // pour arreter le consommateur lorsque le connecteur est detruit
    private ControlUnit uc; // UC du connecteur
    private String identifiant; // identifiant de ce buffer utilise lorsqu'il leve un evenement
    private model.osagaia.ControlUnit ucOsagaia;
    private int rangUE;

    /**
     * Buffer constructor
     *
     * @param ident idicate if this buffer is an input or an output one (only used for alarm information)
     * @param alarmLevel alarm level
     * @param saturationLevel saturation level
     */
    public ConnectorBuffer(String ident, int alarmLevel, int saturationLevel) {
        this.uc = null; // pas d'UC au depart : elle sera definie par setUC
        identifiant = ident; // type de buffer (entree ou sortie)
        seuilAlarme = alarmLevel; // seuil pour lever un evenement
        alarmeSignalee = false; // pas d'evenement leve
        seuilSaturation = saturationLevel; // seuil de saturation => le buffer perd des elements
        saturationSignalee = false; // pas d'evenement leve
        buffer = new Vector<EncapsulatedSample>(); // contenu du buffer
        arret = true; // le buffer n'est pas actif (utilise lors de la suppression d'un connecteur)
        // L'indicateur de connexion du consommateur n'est pas utilisee pour un buffer d'entree
        // car ce consommateur est le CM du connecteur et est donc connecte des que le connecteur est cree
        consommateurConnecte = false;
        rangUE = -1;
    }

    /**
     * Associates the Control Unit of the connector to the BC
     *
     * @param controlUnit Control Unit of the connector
     */
    public void setUC(ControlUnit controlUnit) {
        this.uc = controlUnit; // UC du connecteur
    }

    // lancement du buffer
    /**
     * Starts the buffer
     */
    public void start() {
        arret=false;
    }

    // Arret du buffer => debloque et arrete les threads producteurs/consommateurs
    /**
     * Stops the buffer (all suspended consumers are unlocked).
     */
    public synchronized void stop() {
        arret=true;
        notifyAll(); // pour debloquer les threads et pouvoir les arreter
    }

    /**
     * Accepts a consumer disconnection.
     */
    public synchronized void consumerDisconnection() {
        ucOsagaia = null;
        consommateurConnecte = false;
        rangUE = -1;
        notifyAll(); // pour debloquer les threads et pouvoir les arreter
    }

    /**
     * Accepts a consumer connection.
     * @param uc control unit
     * @param number number of the connected IU
     */
    public void consumerConnection(model.osagaia.ControlUnit uc, int number) {
        ucOsagaia = uc;
        rangUE = number;
        consommateurConnecte = true; // appele via l'UC du connecteur par le consommateur lorsqu'il se connecte
    }

    /**
     * Accepts a listener connection
     * @param uc control unit
     * @param number number of the connected IU
     */
    public void listenerConnection(model.osagaia.ControlUnit uc, int number) {
        ucOsagaia = uc;
        rangUE = number;
        for (int i=0; i<buffer.size(); i++) { // faire vider le buffer par l'ecouteur d'entree
            if (ucOsagaia != null ) ucOsagaia.setInputAvailable(rangUE);
        }
    }

    /**
     * Returns the size of the buffer
     *
     * @return number of samples waiting in the buffer
     */
    public int getSize() {
        return buffer.size();
    }

    /**
     * Indicates if a sample of a given class associated to a given class loader is present in the buffer
     * @param sampleClass class of sample
     * @param cl associated class loader
     * @return true if there is at least one sample of the given class associated to the given class loader
     */
    public synchronized boolean isSampleOfClassAvailable(Class<?> sampleClass, ClassLoader cl) {
        boolean trouve = false;
        int i=0;
        while ((!trouve) && (i<buffer.size())) {
            EncapsulatedSample element = buffer.elementAt(i); // donnee dans le buffer de sortie du connecteur
            Sample test = element.getSample(cl);
            // si elle est compatible avec la classe donnee en parametre OK, sinon voir les autres
            if (sampleClass.isInstance(test)) trouve = true;
            else i++;
        }
        return trouve;
    }

    // Methode appelee par le consommateur pour retirer un element du buffer
    /**
     * Get a sample in the buffer (object of class Sample or inherited class).
     * The requester is suspended on a  semaphore until:
     * <ul>
     * <li>	The requested sample is available
     * <li>	The connector is removed
     * <li>	The requester asks the buffer for a disconnection
     * </ul>
     *
     * @return the sample
     * @throws ServiceClosedException when the connector is not available
     */
    public synchronized EncapsulatedSample getSample() throws ServiceClosedException {
        consommateurConnecte = true; // si cette methode est appelee c'est que le consommateur est connecte
        while ((buffer.size()==0) && (!arret) && consommateurConnecte) { 
            try {
                wait(); // se bloquer si buffer vide et que le connecteur est en marche et que le consommateur est connecte
            }
            catch (InterruptedException ie) {}
        }
        if (!consommateurConnecte) { // le consommateur a ete debloque parce qu'il a demande sa deconnexion
            // une exception est levee pour le consommateur
            throw new ServiceClosedException("US Korrontea deconnectee");
        }
        if (!arret) { // si le connecteur n'est pas arrete (il a pu l'etre pendant qu'on etait bloque)
            // recuperer puis enlever un echantillon du buffer
            EncapsulatedSample trouve = buffer.firstElement();
            buffer.removeElementAt(0);
            // traiter le reamoreage des evenements
            int tailleActuelle = buffer.size();
            if (alarmeSignalee && (tailleActuelle <= 1)) {
                uc.raiseAlarm(identifiant+" fluide");
                alarmeSignalee = false; // reamorcer l'evenement
            }
            if (tailleActuelle <= seuilAlarme) {
                saturationSignalee = false; // reamorcer l'evenement
            }
            if ((!arret) && consommateurConnecte) return trouve; // element retire du buffer
            else throw new ServiceClosedException("US Korrontea deconnectee");
        }
        else // si le buffer a ete arrete => une exception est levee pour le consommateur
            throw new ServiceClosedException("US Korrontea deconnectee");
    }

    // Methode utilisee par les producteurs pour deposer un element dans le buffer
    /**
     * Deposes a sample in the buffer (object of class Sample or inherited class)
     *
     * @param ech sample to depose in the buffer
     */
    public void deposeSample(EncapsulatedSample ech) {
        int tailleActuelle = buffer.size();
        // Traiter les alarmes
        if ((tailleActuelle==seuilAlarme) && (!alarmeSignalee)) {
            // lever un evenement via l'UC car le buffer commence e se remplir trop
            uc.raiseAlarm(identifiant+" seuilAlarme");
            alarmeSignalee = true;
        }
        if (tailleActuelle==seuilSaturation) {
            // on perd les echantillons les plus anciens
            buffer.removeElementAt(0);
            // lever un evenement via l'UC car le buffer est sature
            if   (!saturationSignalee) {
                uc.raiseAlarm(identifiant+" seuilSaturation");
                saturationSignalee = true;
            }
        }
        ajouteSample(ech);
        if (ucOsagaia != null ) ucOsagaia.setInputAvailable(rangUE);
    }

    private synchronized void ajouteSample(EncapsulatedSample sample) {
        buffer.addElement(sample); // d?poser l'?chantillon
        notifyAll(); // d?bloquer les consommateurs
    }

}
