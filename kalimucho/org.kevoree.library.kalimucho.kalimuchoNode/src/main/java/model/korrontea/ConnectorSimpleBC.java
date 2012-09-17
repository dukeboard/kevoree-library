
package model.korrontea;

import model.StopBCException;	// exception levee par la PF pour arreter le CM

/**
 * Implementation of ConnectorBCModel used to do a simple transfer of data without any politic.<br>
 * It is an nfinite loop which reads in the IU and writes in the OU<p>
 * The code of this BC is the following:<p>
 * package model.korrontea<br>
 * import util.StopCMException;	// exception raised by the platform to stop the BC<br>
 * import util.stream.samples; //Sample<br><p>
 * // BC for data transfer in a connector<br>
 * // No politic : only sample transfer<br>
 * public class ConnectorSimpleBC extends ConnectorBCModel {<p>
 * &nbsp;&nbsp;&nbsp;&nbsp;    public void run_CM() throws StopCMException {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    while (true) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;         // read in the IU write in the OU<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;writeSample(readSample());<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     }<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; }<p>
 * &nbsp;&nbsp;&nbsp;&nbsp;   public float levelStateQdS() { // used by platform to get QoS of this BC<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        return 1f ; // QoS level (maximal because no politic is used)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;    }<br>
 * }<br>
 *
 * @author Dalmau
 */

// Composant metier de transfert de flux dans un connecteur
// Ne met en place aucune politique : se contente de transferer les echantillons
public class ConnectorSimpleBC extends ConnectorBCModel {

    /**
     * Infinite loop which reads in the IU and writes in the OU
     *
     */

    public void run_CM() throws StopBCException { // execute lorsque le CM est lance par la PF
        while (true) {
            // lire dans l'UE et ecrire dans l'US
            writeSample(readSample());
        }
    }

    /**
     * Method called by the platform to get the BC QoS level (0 to 1)
     *
     * @return allways 1
     */
    public float levelStateQoS() { // execute quand la PF veut connaetre la QdS de ce CM
        return 1f; // niveau de QdS (toujours au maximum car pas de traitement fait)
    }

}
