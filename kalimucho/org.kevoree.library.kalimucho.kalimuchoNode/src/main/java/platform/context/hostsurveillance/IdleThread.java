package platform.context.hostsurveillance;

/**
 * Thread of lowest priority which measure time between two activations.<br>
 * When it runs it store the actual system time then makes a yield in order to release the CPU.<br>
 * When it is reactivated it stops and measure the time between its last activation.<br>
 * This time is an aproximative  measure of the CPU load.
 * There is no way to know the real CPU load with java (execpted on sunspots or android).
 *
 * @author Dalmau
 */
public class IdleThread extends Thread {
    
    private long temps; // temps entre 2 activations
    private long debut; // date de la 1ere activation
    private boolean running; // indique si le thread est toujours en attente d'activation
    
    /**
     * Thread used to measure CPU loading
     */
    public IdleThread() {
        running = true;
        setPriority(Thread.MIN_PRIORITY+1); // priorite faible
        start();
    }

    /**
     * This thread measures the time between two successive activations
     */
    @Override
    public void run() {
//        debut = System.nanoTime();
        debut = System.currentTimeMillis(); // date actuelle
        yield(); // passer un tour
//        temps = System.nanoTime()-debut;
        temps = System.currentTimeMillis()-debut; // temps pour 2eme activation
        running = false; // le thread a reussi a s'executer
    }

    /**
     * Indicates if the thread is still running
     * @return true is the thread is still running
     */
    public boolean isRunning() { return running; }

    /**
     * Returns the mesured time between two succesive activations
     * @return time between two succesive activations
     */
    public long getTime() { return temps; }

}
