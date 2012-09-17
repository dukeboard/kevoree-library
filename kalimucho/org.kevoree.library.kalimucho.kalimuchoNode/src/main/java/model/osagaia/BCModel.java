package model.osagaia;

import java.io.Serializable;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import platform.servicesregister.ServiceClosedException;
import model.interfaces.control.IBusinessComponent;
import util.streams.samples.Sample;
import model.StopBCException;
import java.util.TimerTask;
import java.util.Timer;
import util.Parameters;
import model.interfaces.inputoutput.InputListener;
import platform.servicesregister.ServicesRegisterManager;
import platform.ClassManager.JarRessources;
import platform.context.ContextManager;
import platform.context.ContextInformation;

// Classe de base permettant d'ecrire les composants metier
/**
 * Abstract class from which BCs inherits.<br>
 * This class manages the life cycle of the BC and
 * offers a method to read a sample on the input stream
 * and a method to write a sample on the output stream.<br><br>
 * The BC designer had to write the method <b>run_BC</b>
 * and the method <b>levelStateQoS</b> which returns the actual QoS level of the BC (0 to 1)<br>
 * <br><br><br>
 * The method run_CM looks like this:<br>
 * public void run_CM() throws StopCMException, InterruptedException {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;  // Initis to do after a migration<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;  while (isRunning()) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// treatment that uses:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// Sample readSample()  to read in the input streams
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// or register listeners to inputs<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// and writeSample(Sample)  to write to the output stream<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// these methods can raise a StopCMException exception<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * }<br>
 * <br>
 * The BC designer can write the method <b>init</b> called when the BC is started
 * and the method <b>destroy</b> called when the BC is stopped.<br><br>
 * In order to define oner's own samples it is necessary to create
 * a class inherited from <b>util.streams.samples.Sample</b> which had to be serializable.
 *
 * @author Dalmau
 */
public abstract class BCModel implements IBusinessComponent, Runnable, Serializable {

    private static final long serialVersionUID = 64240020300000001L; // pour serialisation
    transient private Thread current = null; // Thread accueillant le CM
    /**
     * Thread that manages the listeners associated to this BC
     */
    transient protected InputListenersManager listenerManager;
    transient private InputUnit[] iu = null; // UE
    transient private OutputUnit ou = null; // US
    transient private ControlUnit uc = null; // UC
    transient private String monNom; // nom symbolique du composant
    transient private BCContainer container; // conteneur du CM
    /**
     * Indicates if the BC is running
     */
    transient protected boolean BCRunning; // indique si le CM est en cours d'execution
    transient private boolean BCAllowedToRun; // indique si le CM peut continuer ee s'executer
    transient private boolean runStopped; // indique si le CM a ete arrete pendant son run
    transient private boolean listenerManagerRunning; // indique si le listener manager est lance
    transient private InputListener[] listeListeners; // ecouteyrs d'entrees
   
    // Proprietes serialisees
    private boolean serialized; // indicateur de CM serialise ou cree
    private String[] listeFiltres; // filtres associes aux entrees

    /**
     * Constuction with no parameters 
     */
    public BCModel() {
        current = null;
        iu = null;
        ou = null;
        container = null;
        serialized = false;
        BCRunning = false;
    }

    //Methodes qui doivent etre reecrites par le developpeur de CM
    /**
     * Returns the QoS level of the BC
     *
     * @return the QoS level of the BC
     */
    abstract public float levelStateQoS(); 
    
    /**
     * Infinite loop controled by a call to <b>isRunning</b> method.
     * The BC reads on the input streams, performs a treatment then writes results to the output stream.
     *
     * @throws StopBCException  when the BC is stopped by the platform
     * @throws java.lang.InterruptedException
     * Exceptions used to stop the BC not to be caught
     */
    // rele du CM (boucle infinie)
    abstract public void run_BC() throws StopBCException, InterruptedException;
    
    // Methodes qui peuvent etre reecrites par le concepteur de CM
    /**
     * Called when the platform starts the BC
     * @throws StopBCException when the BC is stopped by the platform
     * @throws InterruptedException when the BC is stopped by the platform
     */
    public void init() throws StopBCException, InterruptedException {} // appelee e la creation du CM
    /**
     * Called when the platform stops the BC
     * @throws StopBCException when the BC is stopped by the platform
     * @throws InterruptedException when the BC is stopped by the platform
     */
    public void destroy() throws StopBCException, InterruptedException {} // appelee e la suppression du CM
    
    /**
     * The BC calls this method if all its work is done into listeners
     * @throws InterruptedException  when the BC is stopped by the platform
     */
    protected final synchronized void idle() throws InterruptedException {
        wait();
    }

    // Methodes utilisees par les CM pour les E/S avec les connecteurs
    // L'exception StopCMException ne doit pas etre recuperee par le CM
    // Elle est utilisee par la PF pour arreter le CM (voir run ci-dessous)

    // designe la classe des echantillons d'entree
    /**
     * Define the name of the class of samples to be read by the BC in  the input stream.<br>. 
     * When <b>readInInputUnit</b> is called without parameter, it is assumed that 
     * the BC reads samples assignment-compatible with the class defined by this method.
     * If the sample actually present in the connector
     * is not assignment-compatible with this class it is discarded and
     * the Input Unit waits for another one.<br>
     * This method is usefull for BCs that reads allways the same class of samples: 
     * Giving this class name by this method allows to use the <b>readSample</b> without parameter.
     * If this method is not called it is assumed that BC accept any <b>Sample</b> class 
     * assignment-compatible kind of samples.<BR>
     * The class defined by this method is also used by the method <b>isSampleAvailableOnInput</b>.
     * That means that this methods returne true only if a sample of this class is available.<br>
     *
     * @param numero number of the Input Unit to which the filter is associated
     * @param className name of the class used as input filter for this Input Unit
     */
    public void setInputClassFilter(int numero, String className) {
        if (numero < iu.length) {
            if (iu[numero] != null) {
                try { 
                    iu[numero].setInputClassFilter(className);
                    listeFiltres[numero] = className;
                }
                catch (ClassNotFoundException cnfe) {
                    try { 
                        iu[numero].setInputClassFilter(Sample.class.getName());
                        listeFiltres[numero] = Sample.class.getName();
                    }
                    catch (ClassNotFoundException impossible) {}
                }
            }
        }
    }

    /**
     * Removes the filter on the class of samples to be read by the BC in the input stream.<br>. 
     * The BC will now accept any <b>Sample</b> class 
     * assignment-compatible kind of samples.<BR>
     * 
     * @param numero number of the Input Unit from which the filter is removed
     *
     */
    public void removeInputClassFilter(int numero) {
        if (numero < iu.length) {
            if (iu[numero] != null) {
                iu[numero].removeInputClassFilter();
                listeFiltres[numero] = "";
            }
        }
    }
    
    /**
     * Waits for a sample of the class defined by the <b>setInputClassFilter</b> method in one input stream.
     * This method suspends the BC until a sample assignment-compatible with this class is available.<br>
     * The name of the class of samples to be read is the one previously defined by the
     * <b>setInputClassFilter</b> method. If the sample actually present in the connector
     * is not assignment-compatible with this class it is discarded and
     * the Input Unit waits for another one.
     * 
     * @param numero number of the Input Unit to read in
     * @return the sample read
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public Sample readSample(int numero) throws StopBCException {
        // lire un echantillon dans l'UE
        if (numero >= iu.length)throw new StopBCException("BC tries to read in an unknown input stream");
        if (iu[numero] != null) {
            Sample ech = iu[numero].readInInputUnit();
            ech.setInputNumber(numero);
            return ech;
        }
        else throw new StopBCException("BC tries to read in an unknown input stream");
    }
    /**
     * Waits for a sample of a given class in one input stream. 
     * The requester is locked on a semaphore until a sample is available.<br>
     * If the sample actually present in the connector is not assignment-compatible 
     * with the class given in parameter it is discarded and the Input Unit waits for another one.
     * 
     * @param numero number of the Input Unit to read in
     * @param className name of the class of sample to be read. If the sample actually present 
     * in the connector is not assignment-compatible with this class it is discarded and 
     * the Input Unit waits for another one.
     * @return the sample read
     * @throws StopBCException  when the BC is stopped by the platform
     * @throws ClassNotFoundException If the class name in parameter could not be found
     */
    final public Sample readSample(int numero, String className) throws StopBCException , ClassNotFoundException {
        // lire un echantillon dans l'UE
        if (numero >= iu.length)throw new StopBCException("BC tries to read in an unknown input stream");
        if (iu[numero] != null) {
            Sample ech = iu[numero].readInInputUnit(className);
            ech.setInputNumber(numero);
            return ech;
        }
        else throw new StopBCException("BC tries to read in an unknown input stream");
    }
    
    /**
     * Waits for a sample in one input stream. This sample needs to be assignment-compatible 
     * with the filter associated to this input stream. 
     * The requester is locked on a semaphore until a sample is available.<br>
     * The number of the stream on which the returned sample has been read is returned
     * by the method <b>getInputNumber</b> of the returned sample.
     * @return the sample read
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public Sample readFirstAvailableSample() throws StopBCException {
        return uc.getFirstInput();
    }
    
    /**
     * Test if a sample is available on one input stream.<br>
     * This method uses the class of samples defined by <b>setInputClassFilter</b>.
     * It returns true only if a sample of this class is available on the given input stream.
     *
     * @param numero number of the Input Unit to read in
     * @return true if a sample of the class defined by defined by <b>setInputClassFilter</b> is available.<br>
     * A true return value means that a of this class is in the stream. But not that it is the first one in this input stream.
     *  
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public boolean isSampleAvailableOnInput(int numero) throws StopBCException {
        // Regerder si un echantillon est disponible dans l'UE
        if (numero >= iu.length)throw new StopBCException("BC tries to read in an unknown input stream");
        if (iu[numero] != null) return iu[numero].isInputAvailable();
        else throw new StopBCException("BC tries to read in an unknown input stream");
    }
    
    /**
     * Writes a sample to all output streams
     *
     * @param sample the sample to write
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public void writeSample(Sample sample) throws StopBCException {
        // ecrire un echantillon dans une US
        if (ou != null) ou.writeInOutputUnit(sample);
    }
    
    // Methodes liees au cycle de vie du CM ne peuvent pas etre surchargees
    /**
     * Method called the BC at each loop of treatment.
     * This method allows to stop the BC by the platform<br>
     * 
     * @return true if the BC can go on running
     * @throws StopBCException  when the BC is stopped by the platform
     */
    final public boolean isRunning() throws StopBCException {
        if (BCAllowedToRun) {
            Thread.yield();
            return true;
        }
        else throw new StopBCException("Stopped by time out");
    }

    /**
     * Adds an input listener to an input stream.
     * @param number number of the input stream
     * @param listener listener to associate to this input stream
     */
    public final void addInputListener(int number, InputListener listener) {
        if (iu[number] != null) {
            if (!listenerManagerRunning) { // lancer le manager si ca n'a pas ete deja fait
                listenerManagerRunning = true;
                listenerManager.start();
            }
            iu[number].addInputListener(listener);
            listeListeners[number] = listener;
        }
    }

    /**
     * Removes an input listener fromo an input stream.
     * @param number number of the input stream
     */
    public final void removeInputListener(int number) {
        if (iu[number] != null) {
            iu[number].removeInputListener();
            listeListeners[number] = null;
        }
    }

    /**
     * Returns an Input stream that gives acces to a resource
     * @param name name of the resource
     * @return the Input stream that gives acces to this resource
     */
    public final InputStream getResourceAsStream(String name) {
        InputStream retour;
        JarRessources ressources = new JarRessources(container.getClassLoader().getJarFileName());
        try {
            retour = ressources.getRessourceAsStream("classes/"+name);
        }
        catch (IOException ioe) {
            retour = null;
        }
        if (retour == null) {
            retour = getClass().getResourceAsStream(name);
        }
        return retour;
    }

    /**
     * Returns a resource as a byte array
     * @param name name of the resource
     * @return this resource as a byte array
     * @throws IOException when the resource can't be read
     */
    public final byte[] getResourceAsByteArray(String name) throws IOException {
        BufferedInputStream lect = new BufferedInputStream(getResourceAsStream(name));
        ByteArrayOutputStream octs = new ByteArrayOutputStream();
        byte[] lu = new byte[8*1024];
        int cpt;
        while ((cpt = lect.read(lu, 0, lu.length)) != -1) {
            octs.write(lu, 0, cpt);
        }
        octs.flush();
        return octs.toByteArray();
    }
    
    /**
     * 
     * This method returns the symbolic name of the BC<br>
     * 
     * @return symbolic name of the BC
     */
    final public String getName() {
        return monNom;
    }

    /**
     * Method called by the platform to run the BC<br>
     * First calls the init method of the BC (done only if the BC has not been serialized)<br>
     * Then calls the run_CM method of the BC<br>
     * Before the BC is stopped by the platform, the destroy method is called
     */
    final public void run() { // methode d'execution d'un CM
        // lancement du CM appel de init (si non migre) puis de run
        // pour terminer appel de destroy
        BCRunning = true; // le CM est lance
        BCAllowedToRun = true; // Le CM a le droit de continuer
        runStopped = false; // la methode run_BC du CM n'a pas ete arretee par la PF
        if (!serialized) { // si le CM n'a pas ete migre on execute init
            try { init(); } // init du CM
            catch (StopBCException scme) { // arret dans une E/S pendant l'init
                terminate_CM(); // debloquer la PF si elle est en attente dans join
                return;
            }
            catch (InterruptedException scme) { // arret brutal pendant l'init
                terminate_CM(); // debloquer la PF si elle est en attente dans join
                return; 
            }
        }
        try { // L'init a ete fait maintenant ou avant la migration
            run_BC(); // boucle de travail du CM
        }
        catch (InterruptedException scme) { // arret brutal
            runStopped = true; // la methode run_BC du CM a ete arretee par la PF
            CM_terminatedByPF(); // terminaison du CM (appel de destroy)
        }
        catch (StopBCException scme) { // arret dans une E/S
            runStopped = true; // la methode run_BC du CM a ete arretee par la PF
            cleanException(); // pour eliminer une InterruptedException qui aurait ete levee
            CM_terminatedByPF(); // terminaison du CM (appel de destroy)
        }
        if (!runStopped) { // Si le Run s'est termine sans exception
            CM_terminatedByPF(); // terminaison du CM (appel de destroy)
        }
    }

    final private void cleanException() { // annule une InterruptException recue trop tot
        try { Thread.sleep(0); } // pour recuperer l'exception
        catch (InterruptedException ie) { /* l'exception est annulee */ }
    }

    /**
     * Normal terminaison of a BC: calls destroy then definitly stops the BC.
     */
    final public void CM_terminatedByPF() {
        try { destroy(); } // terminaison du CM (appel de destroy)
        catch (StopBCException sbcid) {  }
        catch (InterruptedException iid) {  }
        terminate_CM(); // debloquer la PF si elle est en attente dans join
    }

    final private synchronized void terminate_CM() {
        BCRunning = false;
        notifyAll(); // debloquer la PF si elle est dans join
    }

    /**
     * Depose a listener to be run by the listener Manager.<br>
     * The listener manager is a thread that execute the <b>performSample</B> of a listener.
     * It manages a list of listeners to run and runs them.
     * 
     * @param toRead Osagaia Input Unit on which the listener will read a sample
     */
    final public void deposeListener(InputUnit toRead) {
        listenerManager.deposeListener(toRead); // demander son lancement
    }

    /**
     * Starts the BC (called by the Osagaia's UC when started).<br>
     * This method starts the listener manager thread ans the BC thread.
     */
    public void start() { // appelee par l'UC pour lancer le CM
        if (!BCRunning) {
            listenerManager = new InputListenersManager(this);
            listenerManager.setPriority(Thread.NORM_PRIORITY-1);
            listenerManagerRunning = false; // le listener manager ne sera lance que si on enregistre un listener
            current = new Thread(this);
            current.setPriority(Thread.NORM_PRIORITY-1);
            current.start();
        }
    }
    /**
     * Method called by the platform to stop the BC<br>
     * This method throws an InterrutedException to the BC
     * in order to stop it if it is waiting (sleep or wait methods).<br>
     * The listener manager thread is also stopped.
     */
    final public void stop() { // appelee par l'UC pour arereter le CM
        BCAllowedToRun = false;
        listenerManager.stopThread();
        listenerManager.interrupt();
        current.interrupt(); // debloquer le CM s'il est bloque par sleep ou wait
    }
    
    /**
     * Waits for the BC and the listener manager to terminate.
     * A maximum wait time is defined if <b>Parameters</b>.<br>
     * After this delai, if the BC is not yet terminated, the platform set the BC's prioriry
     * to the minimal priority and stops waiting.
     */
    final synchronized public void join() { // Attente de terminaison du CM
        if ((BCRunning) || (listenerManager.isRunning())) { // le CM est encore en marche
            Timer delai = new Timer(); // timer pour eviter d'attendre trop longtemps
            delai.schedule(new WaitForJoin(), Parameters.MAXIMAL_WAIT_FOR_CM);
            while ((BCRunning) || (listenerManager.isRunning())) {// attendre la fin du CM
                try { wait(); } // semaphore debloque par l'arret du CM
                                // ou la fin du delai d'attente de cet arret
                catch (InterruptedException ie) {
                    BCRunning = false;
                }
            }
            delai.cancel(); // arreter le timer
            current = null;
            listenerManager = null;
        }
    }
    
    /**
     * Associate Inuput and Ouptus Units of the container to the BC
     * @param inputUnit array of the Input Units of the container 
     * @param outputUnit Output Unit of the container 
     */
    final public void setInputOutputUnits(InputUnit[] inputUnit, OutputUnit outputUnit) {
        // association du CM e son UE et son US
        this.iu = (InputUnit[])inputUnit;
        listeListeners = new InputListener[iu.length];
        if (!serialized) listeFiltres = new String[iu.length];
        for (int i=0; i<iu.length; i++) {
            listeListeners[i] = null;
            if (!serialized) listeFiltres[i] = "";
        }
        this.ou = (OutputUnit)outputUnit;
    }
    
    /**
     * Associate the Control Unit of the container to the BC
     * @param cu Control Unit of the container
     */
    final public void setControlUnit(ControlUnit cu) {
        uc = cu;
    }
    
    /**
     * Sets the symbolic name of the BC<br>
     * @param name symbolic name of the BC
     */
    final public void setName(String name) {
        monNom = name;
    }
    /**
     * Returns the QoS of the BC<br>
     * If the BC is not running returns -1
     *
     * @return QoS level of the BC (0 to 1)
     */
    final public float readQoS() {
        if (BCRunning) return levelStateQoS();
        else return -1f;
    }

     /**
      * Reassociate all serialized input class filters to the input streams of the BC
      */
     public final void associateFilters() {
         for (int i=0; i<iu.length; i++) {
             if (listeFiltres[i].length() != 0) setInputClassFilter(i, listeFiltres[i]);
         }
     }

     // Classe lancee par un timer pour permettre e la methode join qui attend
     // la terminaison du CM de l'arreter brutalement s'il ne se termine
     // pas dans un delai suffsamment court (defini dans Utils)
     private class WaitForJoin extends TimerTask {
         public void run() {
             terminate_CM();
             if (current != null) current.setPriority(Thread.MIN_PRIORITY);
             if (listenerManager != null) listenerManager.setPriority(Thread.MIN_PRIORITY);
             try {
                ContextManager pf = (ContextManager)(ServicesRegisterManager.lookForService(Parameters.CONTEXT_MANAGER));
                pf.signalEvent(new ContextInformation("Osagaia container "+monNom+": Can't stop BC"));
             }
             catch (ServiceClosedException sce) {
                System.err.println("Osagaia Container "+monNom+" can't get acces to the platform service");
             }
         }
     }

     /**
      * This method is used to indicate that this BC has been serialized
      */
     public void setSerialized() {
         serialized = true;
     }

     /**
      * Associates the container to the BC
      * @param cont the container of the BC
      */
     public void setContainer(BCContainer cont) {
         container = cont;
     }

}
