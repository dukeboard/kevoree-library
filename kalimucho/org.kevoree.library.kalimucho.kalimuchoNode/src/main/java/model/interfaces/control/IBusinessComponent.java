package model.interfaces.control;

/**
 * 
 * The life cycle of the business component(BC) is controled through this interface<br>
 * That alows driving it inside the Container.<br>
 * Indirectly the platform can control its execution via the Control Unit (CU).<br>
 *
 * @author ccassag1 modifyed by Dalmau
 * 
 */

public interface IBusinessComponent {
    // interface de tout composant metier (Osagaia et Korrontea)

    /**
     * Runs the BC
     */
    public void start(); // lancement du CM
    
    /**
     * Waits for BC terminaison
     */
    public void join(); // attente de terminaison du CM
    
    /**
     * Returns the QoS level of the BC (0 to 1)
     *
     * @return the QoS level of the BC (0 to 1)
     */
    
    public float levelStateQoS(); // consultation de la QdS du CM
}
