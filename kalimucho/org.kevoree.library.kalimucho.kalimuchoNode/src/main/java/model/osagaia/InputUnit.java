package model.osagaia;

import model.StopBCException;
import util.streams.samples.Sample;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceClosedException;
import util.Commands;
import model.interfaces.inputoutput.InputListener;
import network.connectors.EncapsulatedSample;

/**
 * Input Unit of a BC container<br>
 * Offers methods to:<br>
 * <ul>
 * <li>	Connect to the input connector.
 * That means waiting until this connector is available.
 * <li>	Disconnect from the input connector
 * <li>	Stop => raises an exception to the BC at the next try of reading
 * in input stream (that will stop the BC)
 * <li>	Read a sample (class Sample or inherited)
 * on the input stream (suspends the BC on a  semaphore until either:
 * <ul>
 * <li>	Sample is available
 * <li>	Input connector has been removed
 * <li>	BCes input has been disconnected
 * <li>	Input  Unit has been stopped
 * </ul></ul>
 * When BCes input is disconnected or when the input connector is removed,
 * the input unit waits for a reconnection.
 *
 * @author Dalmau
 */

// Classe de l'UE Osagaia
public class InputUnit {

    private String nomconnecteurEntree; // nom symbolique du connecteur
    private boolean arret; // indique si l'UE est arretee
    private boolean connecte; // indique si l'UE est connectee
    private model.korrontea.OutputUnit serveur; // Us du connecteur connecte en entree
    private boolean lectureEnCours; // indique si le CM est en cours de lecture
    private Class<?> classeEntree; // classe des echantillons d'entree
    private ControlUnit uc;
    private int rang;
    private InputListener ecouteur;
    
    /**
     * Constuction of an Input Unit for BC container
     * @param number number of the IU
     */
    public InputUnit(int number) {
        rang = number;
        arret=false; // l'UE est prete e fonctionner
        connecte = false; // l'UE n'est pas connectee
        serveur = null;
        lectureEnCours = false;
        ecouteur = null;
        try { classeEntree = Class.forName(Sample.class.getName()); }
        catch (ClassNotFoundException cnfe) { classeEntree = null; }
    }

    /**
     * Associate the Control Unit of the Osagaia Container to this Input Unit.
     * @param cu Control Unit of the Osagaia Container
     */
    public void setControlUnit(ControlUnit cu) {
        uc=cu;
    }

    /**
     * Find the Output Unit of the actually connected connector.
     * 
     * @return the Output Unit of the actually connected connector.
     * @throws ServiceClosedException if there is actually no connector connected to this
     * Osagaia Input Unit
     */
    public model.korrontea.OutputUnit findConnectorOU() throws ServiceClosedException {
        model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree); 
        return accesUS.getOU();
    }

    /**
     * Connects the Input Unit to a connector
     *
     * @param nom name of the connector to connect with
     */
    public synchronized void connection(String nom) {
        nomconnecteurEntree = nom;
        if (!nomconnecteurEntree.equals(Commands.ES_NOT_USED)) {
            // ouverture du service de l'US du connecteur en entree
            model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.platformWaitForService(nomconnecteurEntree);
            serveur = accesUS.getOU();
            serveur.consumerConnection(uc, rang); // signaler la connexion au connecteur
            if (ecouteur != null) serveur.listenerConnection(uc, rang); // s'il y a un ecouteur lui faire vider le buffer
            connecte=true; // l'UE est connecte
            notifyAll(); // deblocage du CM s'il etait en lecture sur l'UE ou en attente de reconnexion
        }
    }
    
    /**
     * Disconnects the Input Unit from the connector
     */
    public void disconnection() {
        if (!nomconnecteurEntree.equals(Commands.ES_NOT_USED)) {
           try { // essayer de signaler e l'US du connecteur qu'on se deconnecte
                model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree); 
                serveur = accesUS.getOU();
                serveur.consumerDisconnection(); // debloque le CM s'il est attente d'entree
           }
           catch (ServiceClosedException ace) {} // on n'est deje pas connecte au connecteur
           connecte = false; // l'UE n'est plus connectee
           while(lectureEnCours) { // attendre que le CM ait termine ou suspendu la lecture
                Thread.yield();
           }
        }
    }

    /**
     * Used to stop the IU.
     * When stopped the IU raises a StopBCException when BC tries to get an input from this IU.
     */
    public synchronized void stop() {
         /* utilise pour que l'UE cesse de fonctionner
          * quand ce booleen est a true tout appel e une lecture dans l'UE
          * provoque une exception de classe StopCMException qui sera utilisee
          * par le CM pour s'arreter
          */
        arret=true;
        notifyAll(); // deblocage du CM s'il etait en lecture sur l'UE ou en attente de reconnexion
    }
    
    /**
     * Define the name of the class of samples to be read by the BC. 
     * When <b>readInInputUnit</b> is called without parameter, it is assumed that 
     * the BC reads samples assignment-compatible with the class defined by this method.
     * If the sample actually present in the connector
     * is not assignment-compatible with this class it is discarded and
     * the Input Unit waits for another one.
     * 
     * @param className name of the class of sample to be read.
     * @throws ClassNotFoundException If the class name in parameter could not be found
     */
    public void setInputClassFilter(String className) throws ClassNotFoundException {
         classeEntree = Class.forName(className);
    }

    /**
     * Removes the class ilter associated to this IU
     */
    public void removeInputClassFilter() {
        try { classeEntree = Class.forName(Sample.class.getName()); }
        catch (ClassNotFoundException cnfe) { classeEntree = null; }
    }

    /**
     * Indicates if an accepted input (see filter) is available on this IU
     * @return true if an accepted input is available
     * @throws StopBCException
     */
    public boolean isInputAvailable() throws StopBCException {
       if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
       if (!connecte) return false;
       else { // l'UE n'est pas arr?t?e et pas d?connect?e
           return isSampleOfClassAvailable(classeEntree);
       }
    }

    /**
     * Read a sample on this IU
     * @return the read sample
     * @throws StopBCException when the BC has to be stopped
     */
    public Sample readInInputUnit() throws StopBCException {
        return readInInputUnit(classeEntree);
    }

    /**
     * Waits for a sample of a given class in the input stream. 
     * The requester is locked on a semaphore until a sample is available.<br>
     * If the sample actually present in the connector is not assignment-compatible 
     * with the class given in parameter it is discarded and the Input Unit waits for another one.
     * 
     * @param className name of the class of sample to be read. If the sample actually present 
     * in the connector is not assignment-compatible with this class it is discarded and 
     * the Input Unit waits for another one.
     * 
     * @return the sample read in the input stream
     * @throws StopBCException Used to stop the BC by the platform
     * @throws ClassNotFoundException If the class name in parameter could not be found
     */
    public Sample readInInputUnit(String className) throws StopBCException, ClassNotFoundException {
        return readInInputUnit(Class.forName(className));
    }

    /**
     * Waits for a sample of a given class in the input stream.
     * The requester is locked on a semaphore until a sample is available.<br>
     * If the sample actually present in the connector is not assignment-compatible
     * with the class given in parameter it is discarded and the Input Unit waits for another one.
     *
     * @param classeEchantillon class of sample to be read. If the sample actually present
     * in the connector is not assignment-compatible with this class it is discarded and
     * the Input Unit waits for another one.
     * @return the sample read in the input stream
     * @throws StopBCException Used to stop the BC by the platform
     */
    private Sample readInInputUnit(Class<?> classeEchantillon) throws StopBCException {
        synchronized(this) { // semaphore debloque par une reconnexion ou un arret de l'UE
           while (!connecte && !arret){ // attendre d'etre connecte ou arrete
               try { wait(); } catch(InterruptedException ie) {}
           }
       }
       // si l'UE est arretee lever une exception pour arreter le CM
       if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
       else { // l'UE n'est pas arretee
           EncapsulatedSample ech=null;
           Sample echLu=null;
           boolean nonRecu=true;
           while (nonRecu && connecte && (!arret)) { // tenter une recuperation d'echantillon dans le connecteur
                            // si pendant cette tentative le connecteur d'entree a ete supprime
                            // ou si l'UE a ete deconnectee il faut attendre une reconnexion ou un arret
               lectureEnCours = true; // la lecture est en cours
               try { // essayer de recuperer un echantillon dans l'US du connecteur
                   model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree); 
                   serveur = accesUS.getOU();
                   ech = serveur.getSample(); // appel bloquant dont on sort avec l'?chantillon ou une exception
                   echLu = ech.getSample(uc.getCM().getClass().getClassLoader());
                   echLu.setInputNumber(rang);
                   // on arrive ici parce qu'on a reeu un echantillon => le retourner
                   if (connecte && (!arret)) {
                       if (classeEchantillon.isInstance(echLu)) nonRecu=false; // on a eu un echantillon de la bonne classe => on le renvoie
                       else nonRecu=true; // on a eu un echantillon pas de la bonne classe => on l'ignore
                   }
               }
               catch (ServiceClosedException ace) { // le connecteur n'existe plus
                   // on arrive ici soit
                   //   parce qu'on ete deconnecte => attendre d'etre reconnecte ou arrete si on n'est pas arrete on retente la lecture
                   //   parce qu'on a ete arrete => terminer la lecture et arreter le CM
                   connecte=false; // l'UE n'est plus connectee => attendre une reconnexion ou un arret
                   synchronized(this) { // semaphore debloque par une reconnexion ou un arret de l'UE
                       lectureEnCours = false; // la lecture est suspendue
                       while (!connecte && !arret){ // attendre d'etre reconnecte ou arrete
                           try { wait(); } catch(InterruptedException ie) {}
                       }
                   }
                   if (arret) nonRecu=false; // si on a ete arrete pandant l'attente se terminer
               }
           }
       // si on a ete arrete pendant une tentative de recuperation d'echantillon il faut arreter le CM
       lectureEnCours = false; // la lecture est terminee
       if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE"));
       else return echLu; // renvoyer l'echantillon
       }
    }

    /**
     * Reads a sample when an event indicating that a new sample is available occurs
     * @return the sample read in the input stream
     * @throws StopBCException Used to stop the BC by the platform
     */
    public Sample readOnEvent() throws StopBCException {
       EncapsulatedSample ech=null;
       Sample echLu=null;
       if (connecte && (!arret)) { // tenter une recuperation d'echantillon dans le connecteur
                        // si pendant cette tentative le connecteur d'entree a ete supprime
                        // ou si l'UE a ete deconnectee il faut attendre une reconnexion ou un arret
           lectureEnCours = true; // la lecture est en cours
           try { // essayer de recuperer un echantillon dans l'US du connecteur
               model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree);
               serveur = accesUS.getOU();
               ech = serveur.getSample(); // appel bloquant dont on sort avec l'?chantillon ou une exception
               echLu = ech.getSample(uc.getCM().getClass().getClassLoader());
               // on arrive ici parce qu'on a reeu un echantillon => le retourner
               if (connecte && (!arret)) {
                   if (!classeEntree.isInstance(echLu)) echLu=null; // on a eu un echantillon pas de la bonne classe => on l'ignore
               }
           }
           catch (ServiceClosedException ace) { // le connecteur n'existe plus
               // on arrive ici soit
               //   parce qu'on ete deconnecte => attendre d'etre reconnecte ou arrete si on n'est pas arrete on retente la lecture
               //   parce qu'on a ete arrete => terminer la lecture et arreter le CM
               connecte=false; // l'UE n'est plus connectee => attendre une reconnexion ou un arret
               lectureEnCours = false; // la lecture est suspendue
              }
              if (arret) echLu=null; // si on a ete arrete pandant l'attente se terminer
           }
       lectureEnCours = false; // la lecture est terminee
       if ((!arret)&& connecte && (echLu != null)) {
           echLu.setInputNumber(rang); // c'est OK on a l'echantillon
       }
       else {
           if (arret) throw(new StopBCException("CM arrete lors d'une lecture dans l'UE")); // le composant est arrete
           else echLu = null; // l'Ue est deconnectee
       }
       return echLu;
   }

    /**
     * Adds an input listener to this IU
     * @param listener input listener to add
     */
    public void addInputListener(InputListener listener) {
        ecouteur = listener;
        try { // s'il y a un connecteur sur cette entree
            model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree);
            serveur = accesUS.getOU();
            serveur.listenerConnection(uc, rang); // faire vider le buffer par l'ecouteur d'entree
        }
        catch (ServiceClosedException sce) {}
    }

    /**
     * Removes the input listener associated to this IU
     */
    public void removeInputListener() {
        ecouteur = null;
    }

    /**
     * Returns the actually associated input listener
     * @return input listener associated to this IU
     */
    public InputListener getInputListener() {
        return ecouteur;
    }

    private boolean isSampleOfClassAvailable(Class<?> sampleClass) {
        try {
            model.korrontea.ControlUnit accesUS = (model.korrontea.ControlUnit)ServicesRegisterManager.lookForService(nomconnecteurEntree);
            return accesUS.getOU().getBuffer().isSampleOfClassAvailable(sampleClass, uc.getCM().getClass().getClassLoader());
        }
        catch(ServiceClosedException sce) {
            return false;
        }
    }

}
