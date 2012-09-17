package model.osagaia;

import util.streams.samples.Sample; 	// class to extend in order to define classes for objects 
	// that a BC can read or write
import model.StopBCException;	// exception raised by the platform to stop the BC

/**
 * This class does nothing it is just a skeleton to write custom BC classes.
 * 
 * @author Dalmau
 */
public class BCSkeleton extends BCModel {

    /**
     * TO DO: BC initialisationsdone only when the BC is created (not after a migration)<br>
     * Possibly a call to setInputClassFilter to indicate the class of samples accepted in the input streams:<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;setInputClassFilter(MysampleClass.class.getName());
     * @throws StopBCException when the BC is stopped by the platform
     * @throws InterruptedException when the BC is stopped by the platform
     */
    @Override
    public void init() throws StopBCException, InterruptedException {
    // TO DO: BC initialisations done only when the BC is created (not after a migration)
    }

    /**
     * TO DO: BC data treatment<br>
     * public void run_CM() throws StopCMException, InterruptedException {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;  // Initialisations that need to be done after a migration<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;  while (isRunning()) {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// treatment that uses:<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// Sample readSample()  to read in the input stream<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// and writeSample(Sample)  to write to the output stream<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// these methods can raise a StopCMException exception<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
     * }<br>
     * @throws StopBCException when the BC is stopped by the platform
     * @throws InterruptedException when the BC is stopped by the platform
     */
    public void run_BC() throws StopBCException, InterruptedException {
        // Initialisations that need to be done after a migration
        while (isRunning()) {
            // treatment that uses:
            // Sample readSample()  to read in the input stream
            // and writeSample(Sample)  to write to the output stream
            // these methods can raise a StopCMException exception
        }
    }

    /**
     * TO DO: what the BC does before stopping<br>
     * This method is called when the BC is stopped definitely or before a migration
     * @throws StopBCException when the BC is stopped by the platform
     * @throws InterruptedException when the BC is stopped by the platform
     */
    @Override
    public void destroy() throws StopBCException, InterruptedException {
        // TO DO: what the BC does before stopping
    }

    /**
     * TO DO: return the actual QoS level of the BC (0 to 1)
     * 
     * @return the actual QoS level of the BC (0 to 1)
     */
    public float levelStateQoS() {
        // TO DO: return the actual QoS level of the BC (0 to 1)
        return 1.0f;
    }

}
