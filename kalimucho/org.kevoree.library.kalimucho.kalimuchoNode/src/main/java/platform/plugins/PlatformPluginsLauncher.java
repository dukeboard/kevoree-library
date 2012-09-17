package platform.plugins;

import util.Parameters;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import model.interfaces.platform.IPlatformPlugin;
import java.util.HashMap;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;

/**
 * Launcher of all the requested platform's plugins
 * The plugins are registered in a txt file located as /platform/plugins/plugins.txt
 *
 * @author Dalmau
 */
public class PlatformPluginsLauncher {

    private HashMap<String, IPlatformPlugin> lances;

    /**
     * Create a plugin launcher that runs all the registered plugins
     */
    public PlatformPluginsLauncher() {
        try {
            ServicesRegisterManager.registerService(Parameters.PLUGINS_LAUNCHER, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.out.println("Plugins launcher service created twice");
        }
        lances = new HashMap<String, IPlatformPlugin>();
        // lancer tous les plugins trouves dans le fichier res/plugins.txt
        InputStream lire = getClass().getResourceAsStream("/platform/plugins/plugins.txt");
        if (lire != null) {
            BufferedReader lireNoms = new BufferedReader(new InputStreamReader(lire));
            String plug;
            try {
                while ((plug = lireNoms.readLine()) != null) {
                    if (!plug.startsWith("#")) {
                        try {
                            installPlugin(plug);
                        }
                        catch (ClassNotFoundException cnfe) {
                            System.err.println("Can't start plugin "+plug+" : class unknown");
                        }
                        catch (InstantiationException cnfe) {
                            System.err.println("Can't start plugin "+plug+" : class can't be instantiated");
                        }
                        catch (IllegalAccessException cnfe) {
                            System.err.println("Can't start plugin "+plug+" : Illegal access to the class");
                        }
                    }
                }
            }
            catch (IOException ioe) {
                System.err.println("Can't read plugins file: /platform/plugins/plugins.txt");
            }
        }
    }

    /**
     * Start a plugin
     * @param plug name of the pluggin
     * @throws ClassNotFoundException the class of the pluggin does not exist
     * @throws InstantiationException the class of the pluggin can't be instanciated
     * @throws IllegalAccessException the class of the pluggin can't be accessed
     */
    public synchronized void installPlugin(String plug) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (lances.get(plug) == null) {
            Class<?> tmpClass = Class.forName(plug);
            IPlatformPlugin pp = (IPlatformPlugin)tmpClass.newInstance();
            lances.put(plug, pp);
            pp.startPlugin();
            System.out.println("Adding plugin "+plug);
        }
        else System.out.println(plug+" allready installed");
    }

    /**
     * Uninstalls a plugin
     * @param plug name of the pluggin
     */
    public synchronized void uninstallPlugin(String plug) {
        if (lances.get(plug) != null) {
            lances.get(plug).stopPlugin();
            System.out.println("Removing plugin "+plug);
        }
        else {
            System.out.println("Removing plugin "+plug+" can't find this plugin");
        }
    }

    /**
     * Uninstalls a plugin
     * @param plug name of the pluggin
     */
    public synchronized void startPlugin(String plug) {
        if (lances.get(plug) != null) {
            lances.get(plug).startPlugin();
            System.out.println("Starting plugin "+plug);
        }
        else {
            System.out.println("Can't start plugin "+plug+": plugin unknown");
        }
    }

    /**
     * Stops the plugins
     */
    public void stopAllPlugins() {
        for (String cle : lances.keySet()) {
            lances.get(cle).stopPlugin();
        }
    }

}
