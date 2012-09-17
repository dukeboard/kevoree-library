package model.interfaces.inputoutput;

import util.streams.samples.Sample;
import model.StopBCException;

/**
 * Interface of listeners for samples (inputs) used by BC components in order to perform received samples automatically
 * 
 * @author Dalmau
 */
public interface InputListener {

    /**
     * Performs the treatment associated to a received sample
     * @param ech received sample
     * @throws StopBCException  rose to stop the Bc when removed or migrated
     * @throws InterruptedException  rose to stop the Bc when removed or migrated
     */
    public void performSample(Sample ech) throws StopBCException, InterruptedException;
}
