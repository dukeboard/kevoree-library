package model.korrontea;

import model.interfaces.IContainer;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;

// Classe du conteneur de flux (connecteur)
/**
 * Class of connectors' containers, created with its name, the type of the connector input
 * (einternale or address and port number)
 * and the type of the connector output (einternale or address and port number).<br>
 * Creates an instance of a transfer BC which ensures data circulation inside the connector.<br>
 * Creates an Input Unit (IU), an Output Unit (OU), a Control Unit (CU) and runs the BC.<br>
 * Registers the service provided by the CU.
 *
 * @author Dalmau
 */
public class ConnectorContainer implements IContainer {

    private String monNom; // nom symbolique du connecteur
    private InputUnit ue;
    private OutputUnit us;
    private ControlUnit uc;
    private ConnectorBCModel cm;

    /**
     * Container construction
     *
     * @param name The symbolic name of this container used to appoint it by the platform
     */
    public ConnectorContainer(String name) {
        monNom = name;
        // Creation UE US UC et CM
        cm = new ConnectorSimpleBC();
        us = new OutputUnit(monNom);
        ue = new InputUnit(monNom); // clonage des echantillons
        cm.setInputOutputUnits(ue, us); // associer le composant e son UE et son US
        uc=new ControlUnit(monNom,ue,cm,us,this); // creation de l'UC
        uc.startUEandUS(); // lancement de l'UE (attente de connexion du composant osagaia)
        uc.start_BC(); // lancement du CM de transfert de flux
        try { // enregistrement des services UC, UE et US
            ServicesRegisterManager.registerService(monNom, uc);
        }
        catch(ServiceInUseException siue) {
            System.err.println("Connector "+monNom+": created twice");
        }
    }

}
