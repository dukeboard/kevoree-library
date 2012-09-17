package model.interfaces.platform;

/**
 * Interface for plugins than the platform can start.
 *
 * @author Dalmau
 */
public interface IPlatformPlugin {

    /**
     * Method to start the plugin (the plugin can ba a thread)
     */
    public void startPlugin();
    /**
     * Method to stop the plugin (the plugin can ba a thread os can start servers)
     */
    public void stopPlugin();

}
