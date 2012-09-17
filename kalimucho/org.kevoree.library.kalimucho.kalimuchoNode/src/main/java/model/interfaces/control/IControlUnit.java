package model.interfaces.control;


/**
 * Interface of Control Units used in Containers (BC container and connector container).<br>
 * 
 * @author Dalmau
 * 
 */

public interface IControlUnit {
    // Interface de toute UC (Osagaia et Korrontea)
    
    /**
     * Sends to the platform the QoS level returned by the the BC (0 to 1)
     *
     * @return the QoS level returned by the the BC (0 to 1)
     */
    public float sendBCQoSLevel(); // consultation de la QdS

}
