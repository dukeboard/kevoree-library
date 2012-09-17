package platform.context.hostsurveillance;

import platform.context.ContextManager;
import platform.context.ContextInformation;
import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import java.util.TimerTask;
import util.Parameters;

/**
 * Class run by a timer in order to measure the host's resources.
 * At regular intervals (defined in Parameters) this clas measure the free memory and CPU load
 * @author Dalmau
 */

// Objet lance par un timer pour surveiller la charge de l'hete
// a intervalles reguliers (delai valeur donne dans Parameters).
// Surveille : memoire libre et charge CPU
public class HostLoadWatcher extends TimerTask {

    private ContextManager gestionnaireDeContexte; // superviseur pour lever les alarmes
//    private long ancienTempsIdle; // pour le calcul du temps CPU libre
//    private int memLibre; // pourcentage de memoire libre
    private int cumulMemLibre; // cumul de mesures de pourcentage de memoire libre pour moyenne
    private int moyenneMemLibre; // cumul de mesures pour moyenne
    private boolean alarmeMemoireEnvoyee; // indicateur d'alarme memoire envoyeee
    private long dateMesure; // derniere date a laquelle ce timer a ete active
    private int cumulChargeCPU; // cumul de pourcentage de charge CPU
    private int moyenneChargeCPU; // pourcentage moyen de charge CPU
    private boolean alarmeCPUEnvoyee; // indicateur d'alarme CPU envoyeee
    private int compteMesuresFaites; // la mesure se fait sur une moyenne de mesures periodiques
    //private int threadsActifs;
    private IdleThread mesureCPU;
    private int emisPFReseau, emisConnReseau;
    private int recPFReseau, recConnReseau;
    private NetworkEmitterContainer networkEmitterContainer;
    private NetworkReceptorContainer networkReceptorContainer;

    /**
     * Create the thread to measure host's state
     * @param cm context manager to which events are sent
     * @param nec the network emitter container of the platform
     * @param rec the network receptor container of the platform
     */
    public HostLoadWatcher(ContextManager cm, NetworkEmitterContainer nec, NetworkReceptorContainer rec) {
        gestionnaireDeContexte = cm;
        networkEmitterContainer = nec;
        emisPFReseau = 0; emisConnReseau = 0;
        networkReceptorContainer = rec;
        recPFReseau =0; recConnReseau = 0;
        cumulMemLibre = 0;
        moyenneMemLibre = (int)(Runtime.getRuntime().freeMemory()*100/Runtime.getRuntime().totalMemory());
        moyenneChargeCPU = 0;
        cumulChargeCPU = 0;
        alarmeCPUEnvoyee = false; // alarme non encore envoyee
//        dateMesure = System.nanoTime();
        dateMesure = System.currentTimeMillis();
        mesureCPU = new IdleThread();
    }

    // A intervalles reguliers relever l'etat d'activite de l'hete
    /**
     * Execute a measure a fixed interval
     */
    public void run() {
        long date = System.currentTimeMillis();
//        long date = System.nanoTime();
        emisPFReseau = networkEmitterContainer.getNetworkPlatformTraffic()/Parameters.LOAD_MEASURE_RATE;
        emisConnReseau = networkEmitterContainer.getNetworkConnectorsTraffic()/Parameters.LOAD_MEASURE_RATE;
        recPFReseau = networkReceptorContainer.getNetworkPlatformTraffic()/Parameters.LOAD_MEASURE_RATE;
        recConnReseau = networkReceptorContainer.getNetworkConnectorsTraffic()/Parameters.LOAD_MEASURE_RATE;
        // mesure de la memoire libre
        long libre = Runtime.getRuntime().freeMemory()*100/Runtime.getRuntime().totalMemory();
        cumulMemLibre = cumulMemLibre+(int)libre;

        // Mesure du temps CPU libre => occupation du CPU
        if (mesureCPU.isRunning()) { // le thread de mesure lance au precedent passage n'est toujours pas termine
            cumulChargeCPU = cumulChargeCPU+100; // la charge CPU est donc de 100% pour ce passage
        }
        else { // le thread de mesure lance au precedent passage est termine
            if (date != dateMesure) { // le cas d'egalite se produit parfois, j'ignore pourquoi !!!!!!
                // le temps mesure par ce thread est converti en % de charge CPU et cummule
                cumulChargeCPU = cumulChargeCPU + (int) (mesureCPU.getTime()*100/(date-dateMesure));
                dateMesure = date;
                mesureCPU = new IdleThread(); // lancer un nouveau thread de mesure
            }
        }
        
        // Traitement des alarmes memoire et CPU
        compteMesuresFaites++; // compter le nombre de passages dans ce timer
        if (compteMesuresFaites == Parameters.NUMBER_MEASURES_FOR_AVERAGE) {
            // faire la moyenne des charges CPU mesurees
            moyenneChargeCPU = cumulChargeCPU/Parameters.NUMBER_MEASURES_FOR_AVERAGE;
            // Voir s'il faut lever une alarme de charge CPU
            if ((moyenneChargeCPU >= Parameters.CPU_LOAD_ALARM_LEVEL) && (!alarmeCPUEnvoyee)) {
                gestionnaireDeContexte.signalEvent(new ContextInformation("High CPU load "+String.valueOf(moyenneChargeCPU)+"%"));
                alarmeCPUEnvoyee = true; // lever l'alarme
            }
            if (moyenneChargeCPU < Parameters.END_CPU_LOAD_ALARM_LEVEL)  
                alarmeCPUEnvoyee = false; // rearmer l'alarme

            // faire la moyenne de l'occupation memoire
            moyenneMemLibre = cumulMemLibre/Parameters.NUMBER_MEASURES_FOR_AVERAGE;
            // Voir s'il faut lever une alrme memoire
            if ((moyenneMemLibre <= Parameters.MEMORY_ALARM_LEVEL) && (!alarmeMemoireEnvoyee)) {
                gestionnaireDeContexte.signalEvent(new ContextInformation("Low free Memory "+String.valueOf(moyenneMemLibre)+"%"));
                alarmeMemoireEnvoyee = true; // lever l'alarme
            }
            if (moyenneMemLibre > Parameters.END_MEMORY_ALARM_LEVEL)
                alarmeMemoireEnvoyee = false; // rearmer l'alarme
            
            // reinitialiser pour un nouveau calcul de moyenne
            compteMesuresFaites = 0;
            cumulMemLibre = 0;
            cumulChargeCPU = 0;
        }
    }

    // Methodes permettant de recuperer les dernieres mesures
    /**
     * Get free memory (in %)
     *
     * @return free memory actual average value (in %)
     */
    public int getFreeMemory() { return moyenneMemLibre; }
    
    /**
     * Returns the battery level (in %). For a PC hosts this value is always 100
     * @return the battery level (in %)
     */
    public int getBatteryLevel() { return 100; }

    /**
     * Get CPU load (in %)
     *
     * @return CPU load actual average value (in %)
     */
    public int getCPULoad() { return moyenneChargeCPU; }

    /**
     * returns the actual number of running threads
     * @return the actual number of running threads
     */
    public int getThreadsActifs() { return Thread.activeCount(); }

    /**
     * Returns the actual network output traffic due to the platform
     * @return the actual network output traffic due to the platform
     */
    public int getNetworkPlatformEmissions() { return emisPFReseau; }

    /**
     * Returns  the actual network input traffic due to the platform
     * @return  the actual network input traffic due to the platform
     */
    public int getNetworkPlatformReceptions() { return recPFReseau; }

    /**
     * Returns  the actual network output traffic due to the connectors
     * @return   the actual network output traffic due to the connectors
     */
    public int getNetworkConnectorsEmissions() { return emisConnReseau; }

    /**
     * Returns   the actual network input traffic due to the connectors
     * @return   the actual network input traffic due to the connectors
     */
    public int getNetworkConnectorsReceptions() { return recConnReseau; }

}
