/*
 *
 * Created on 27 oct. 2009 16:22:42;
 */

package liuppa.t2i.alcool.kalimuchofixe;

import platform.Platform;

/**
 * The main programm of Kalimucho.<br>
 * It only runs the platform.
 *
 * @author Dalmau
 */

public class Kalimucho {

    private Platform plateforme;

    /**
     * Runs the PF and the containers for network management
     */
    public void run() {
        // Lancer la plate-forme
        plateforme = new Platform();
    }

    /**
     * Stops the Kalimucho platform
     */
    public void stop() {
        // arreter les services lances
        plateforme.stop();
    }

    /**
     * Main programm for Kalimucho
     * @param args no arguments
     */
    public static void main(String[] args) {
        Kalimucho app = new Kalimucho();
        app.run();
    }
}
