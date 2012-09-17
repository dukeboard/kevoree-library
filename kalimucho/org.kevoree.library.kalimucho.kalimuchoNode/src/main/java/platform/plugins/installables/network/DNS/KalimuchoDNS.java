package platform.plugins.installables.network.DNS;

import network.NetworkEmitterContainer;
import network.NetworkReceptorContainer;
import network.platform.PlatformMessagesReceptor;
import network.platform.NetworkPlatformMessage;
import platform.servicesregister.ServicesRegisterManager;
import platform.servicesregister.ServiceInUseException;
import platform.servicesregister.ServiceClosedException;
import model.interfaces.platform.IPlatformPlugin;
import util.Parameters;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Vector;
import util.NetworkAddress;
import network.AddressesChecker;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This service is a kind of DNS for the Kalimucho platform.<br>
 * Each host from which something is received by network id added to the DNS.<br>
 * Because hosts can disappear, each host has a time to tive at the end of which it is removed from the DNS.<br>
 * At regular rate hosts send tgheis DNS content to neighboors (host that can be accessed directly by network).<br>
 * When a DNS content is received, the local one id updated from information taken into the received one.<br>
 * For each host in the DNS information is:<br>
 * - The unique identifier of this host.<br>
 * - The clock shift of the clock of this host relatively to the local one (if available).<br>
 * - The clock shift accuracy (if available).<br>
 * - The known addresses of this host. For each address extra information is :<br>
 *   - This adress is directly reachable or not.<br>
 *   - This addres has been taken from a received DNS or collected locally
 *
 * @author Dalmau
 */
public class KalimuchoDNS extends Thread implements IPlatformPlugin {

    private HashMap<String, RemoteHostDescriptor> registeredHosts;
    private String myID;
    private Timer raffraichissement;
    private Timer diffusion;
    private PlatformMessagesReceptor boiteDeReponses;
    private NetworkReceptorContainer rec;
    private NetworkEmitterContainer nec;
    static private boolean actif = false;

    /**
     * Create a local DNS. All the addresses of this host are added on the DNS.
     */
    public KalimuchoDNS() {
        nec = (NetworkEmitterContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_EMISSIONS_CONTAINER);
        rec = (NetworkReceptorContainer)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_RECEPTIONS_CONTAINER);
        boiteDeReponses = rec.getPlatformMessagesReceptor();
        try { // enregistrer le service de DNS
            ServicesRegisterManager.registerService(Parameters.KALIMUCHO_DNS_MANAGER, this);
        }
        catch (ServiceInUseException  mbiue) {
            System.err.println("Kalimucho DNS service created twice");
        }
    }

    public void startPlugin() {
        if (actif) return;
        actif = true;
        registeredHosts = new HashMap<String, RemoteHostDescriptor>(); // creer la liste d'enregistrement des hotes dans le DNS
        // Ajouter une entree de DNS pour l'hote local
        AddressesChecker addressesChecker = (AddressesChecker)ServicesRegisterManager.platformWaitForService(Parameters.NETWORK_ADDRESSES);
        myID = addressesChecker.getHostIdentifier();
        Vector<NetworkAddress> mesAdresses = addressesChecker.getAllAddresses();
        RemoteHostDescriptor hote = new RemoteHostDescriptor();
        hote.setClockAccuracy(0);
        for (int i=0; i<mesAdresses.size(); i++) {
            hote.addAddress(mesAdresses.elementAt(i), true, false);
        }
        registeredHosts.put(new String(myID), hote);
        // Timer pour gerer les durees de vie des enregistrements du DNS
        raffraichissement = new Timer();
        raffraichissement.scheduleAtFixedRate(new RefreshDnsTask(), Parameters.DNS_UPDATE_RATE*1000, Parameters.DNS_UPDATE_RATE*1000);
        diffusion = new Timer();
        diffusion.scheduleAtFixedRate(new DiffusionDnsTask(), Parameters.DNS_DIFFUSION_RATE*1000, Parameters.DNS_DIFFUSION_RATE*1000);
        boiteDeReponses.inscription(Parameters.KALIMUCHO_DNS_MANAGER);
        start();
    }

    public synchronized void stopPlugin() {
        if (!actif) return;
        actif = false;
        raffraichissement.cancel(); // arret du timer de gestion des durees de vie
        diffusion.cancel();
        boiteDeReponses.stop();
        boiteDeReponses.desinscription(Parameters.KALIMUCHO_DNS_MANAGER);
        try { // desenregistrer le service de DNS
            ServicesRegisterManager.removeService(Parameters.KALIMUCHO_DNS_MANAGER);
        }
        catch (ServiceClosedException sce) {
            System.err.println("DNS service already closed");
        }
    }

    /**
     * Receive sent DNS and update the local one.
     */
    @Override
    public void run() {
        while (actif) {
            NetworkPlatformMessage recu = boiteDeReponses.retirerMessage(Parameters.KALIMUCHO_DNS_MANAGER);
            if (recu != null) {
                updateWithReferenceList(recu.getSenderID(), recu.getSerializedObject());
            }
        }
    }

    // Methodes pour completer les infos du DNS a partir d'infos recuperees localement
    /**
     * Add a new information on the DNS.
     * @param id host unique identifier
     * @param addr host address
     * @param dir true if this address allows a direct acces to this host
     */
    public synchronized void addReference(String id, NetworkAddress addr, boolean dir) {
        // Ajout d'un hote ou d'une adresse pour un hote
        // Le dernier parametre permet de distinguer le cas d'un ajout a partir d'une information locale
        // de celui d'un ajout a partir d'informations transmises par un autre DNS
        RemoteHostDescriptor hote = registeredHosts.get(id);
        if (hote == null) { // on n'avait pas encore cet hote dans le DNS
            hote = new RemoteHostDescriptor(); // on le creee
            registeredHosts.put(new String(id), hote); // on l'ajoute au DNS
            System.out.println(id+" added to DNS");
        }
        // Que l'hote soit deja connu ou pas on met a jour l'adresse recue pour cet hote
        hote.addAddress(addr, dir, false);
    }
    /**
     * Add a new information on the DNS.
     * @param id host unique identifier
     * @param addr host address
     * @param dir true if this address allows a direct acces to this host
     */
    public void addReference(String id, String addr, boolean dir) {
        // Ajout d'un hote ou d'une adresse pour un hote a partir d'une information locale
        addReference(id, new NetworkAddress(addr), dir);
    }

    /**
     * REmoves an address from the DNS
     * @param addr the address to remove
     */
    public synchronized void removeReference(NetworkAddress addr) {
        String id = getHostID(addr);
        if (id != null) {
            registeredHosts.get(id).removeAddress(addr);
        }
    }

    // Methodes pour completer les infos du DNS a partir d'infos d'autres DNS distants
    private synchronized void updateWithReferenceList(String senderID, HashMap<String, RemoteHostDescriptor> autre) {
        // Cette methode est employee pour completer le DNS a partir d'un DNS distant
        // Les hotes inconnus sont ajoutes
        // Les adresses inconnues des hotes connus sont ajoutes
        // Les adresses connues des hotes connus ne sont pas modifiees
        RemoteHostDescriptor hoteExpediteur = registeredHosts.get(senderID);
        if (autre.get(myID).isClockShiftAvailable()) { // si on a recu le decalage d'horloge avec l'expediteur
            // s'il est de meilleure precision que ce que l'on a ou si on n'a pas l'info
            if ((autre.get(myID).getClockAccuracy() < hoteExpediteur.getClockAccuracy()) || (!hoteExpediteur.isClockShiftAvailable())) {
                hoteExpediteur.setClockShift(-autre.get(myID).getClockShift()); // mettre a jour le decalage avec l'expediteur
                hoteExpediteur.setClockAccuracy(autre.get(myID).getClockAccuracy());
            }
        }
        for (String cle : autre.keySet()) { // parcourrir les hotes enregistres dans le DNS recu
            RemoteHostDescriptor hoteEnregistreParAutre = autre.get(cle); // hote recu
            RemoteHostDescriptor hoteEnregistreLocalement = registeredHosts.get(cle); // cet hote enregistre localement ou null s'il n'est pas connu
            if (hoteEnregistreLocalement != null) { // on avait deja cet hote dans notre DNS
                if (hoteEnregistreLocalement.getTimeToLive() < hoteEnregistreParAutre.getTimeToLive()) {
                    // si sa duree de vie etait inferieure a celle recue mettre a jour sa duree de vie
                    hoteEnregistreLocalement.setTimeToLive(hoteEnregistreParAutre.getTimeToLive());
                }
                // ajouter toutes les adresses de l'hote recu
                for (String adr : hoteEnregistreParAutre.getRemoteHostAddresseDescriptors().keySet()) {
                    if (hoteEnregistreParAutre.getRemoteHostAddresseDescriptors().get(adr).isDirect())
                        hoteEnregistreLocalement.addAddress(new NetworkAddress(adr), false, false);
                    else hoteEnregistreLocalement.addAddress(new NetworkAddress(adr), false, true);
                }
                // traitement de la mise a jour des infos d'horloge pour cet hote
                if (hoteExpediteur.isClockShiftAvailable()) { // on connait le decalage d'horloge avec l'hote expediteur
                    // on calcule le decalage et l'erreur d'horloge pour l'hote recu
                    long erreur = hoteExpediteur.getClockAccuracy()+hoteEnregistreParAutre.getClockAccuracy();
                    if ((erreur < hoteEnregistreLocalement.getClockAccuracy()) || (!hoteEnregistreLocalement.isClockShiftAvailable())) {
                        // la precision calculee est meilleure que celle que l'on a on on n'a pas l'info
                        hoteEnregistreLocalement.setClockShift(hoteExpediteur.getClockShift()+hoteEnregistreParAutre.getClockShift());
                        hoteEnregistreLocalement.setClockAccuracy(erreur); // garder ces nouvelles valeurs
                    }
                }
            }
            else { // l'hote recu n'est pas connu => on va l'ajouter
                hoteEnregistreLocalement = new RemoteHostDescriptor();
                if (hoteExpediteur.isClockShiftAvailable()) { // on connait le decalage d'horloge avec l'hote expediteur
                    // on calcule le decalage et l'erreur d'horloge pour l'hote recu ajoute
                    hoteEnregistreLocalement.setClockShift(hoteExpediteur.getClockShift()+hoteEnregistreParAutre.getClockShift());
                    hoteEnregistreLocalement.setClockAccuracy(hoteExpediteur.getClockAccuracy()+hoteEnregistreParAutre.getClockAccuracy());
                }
                else { // on ne connait pas le decalage d'horloge avec l'hote expediteur
                    hoteEnregistreLocalement.setClockShiftNotAvailable(); // pas d'infos d'horloge pour l'hote recu ajoute
                }
                for (String adr : hoteEnregistreParAutre.getRemoteHostAddresseDescriptors().keySet()) {
                    if (hoteEnregistreParAutre.getRemoteHostAddresseDescriptors().get(adr).isDirect())
                        hoteEnregistreLocalement.addAddress(new NetworkAddress(adr), false, false);
                    else hoteEnregistreLocalement.addAddress(new NetworkAddress(adr), false, true);
                }
                registeredHosts.put(new String(cle), hoteEnregistreLocalement); // ajouter l'hote recu
            }
        }
    }
    private void updateWithReferenceList(String senderID, byte[] content) {
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        HashMap<String, RemoteHostDescriptor> autre;
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            autre = (HashMap<String, RemoteHostDescriptor>)ois.readObject();
            updateWithReferenceList(senderID, autre);
        }
        catch (IOException ioe) {
            autre = null;
            System.err.println("Error converting a received DNS list: IOError");
        }
        catch (ClassNotFoundException ioe) {
            autre = null;
            System.err.println("Error converting a received DNS list: class not found");
        }
    }

    // Methodes de recuperation de la liste complete des hotes connus
    /**
     * Serialize the content of the DNS into a byte array (used to send it on network)
     * @return the content of the DNS serialized into a byte array
     */
    public synchronized byte[] getHostsAsByteArray() {
        byte[] retour;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(registeredHosts);
            oos.flush();
            retour = bos.toByteArray();
        }
        catch (IOException ioe) {
            System.err.println("Error converting a DNS list to byte array");
            retour =  null;
        }
        return retour;
    }

    // Methodes pour recuperer les infos relatives a un hote
    /**
     * Returns the host descriptor of the specified host
     * @param id unique identifier of the host to get information on
     * @return the host descriptor of the specified host
     */
    public synchronized RemoteHostDescriptor getRemoteHostDescriptor(String id) { return registeredHosts.get(id); }

    // Methode pour recuperer les adresses connues pour un hote
    /**
     * Returns all the known addresses of the specified host
     * @param id unique identifier of the host to get information on
     * @return all the known addresses of the specified host
     */
    public synchronized Vector<String> getRemoteHostAddresses(String id) {
        Vector<String> retour = new Vector<String>();
        HashMap<String, RemoteHostAddressDescriptor> descrs = registeredHosts.get(id).getRemoteHostAddresseDescriptors();
        for (String cle : descrs.keySet()) {
            retour.addElement(cle);
        }
        return retour;
    }
    /**
     * Returns all the known addresses of the specified host according to a given network type
     * @param id unique identifier of the host to get information on
     * @param type network type
     * @return all the known addresses of the specified host according to a given network type
     */
    public synchronized Vector<String> getRemoteHostAddresses(String id, int type) {
        Vector<String> retour = new Vector<String>();
        HashMap<String, RemoteHostAddressDescriptor> descrs = registeredHosts.get(id).getRemoteHostAddresseDescriptors();
        for (String cle : descrs.keySet()) {
            NetworkAddress na = new NetworkAddress(cle);
            if (na.getType() == type) retour.addElement(cle);
        }
        return retour;
    }

    // Methode pour recuperer les adresses permettant un acces direct a un hote
    /**
     *  Returns all the known addresses of the specified host that are directly accessible
     * @param id unique identifier of host to get information on
     * @return all the known addresses of the specified host that are directly accessible
     */
    public synchronized Vector<String> getRemoteHostDirectAddresses(String id) {
        Vector<String> retour = new Vector<String>();
        HashMap<String, RemoteHostAddressDescriptor> descrs = registeredHosts.get(id).getRemoteHostAddresseDescriptors();
        for (String cle : descrs.keySet()) {
            if (descrs.get(cle).isDirect()) retour.addElement(cle);
        }
        return retour;
    }
    /**
     * Returns all the known addresses of the specified host that are directly accessible on the specified network type
     * @param id unique identifier of the host to get information on
     * @param type network type
     * @return all the known addresses of the specified host that are directly accessible on the specified network type
     */
    public synchronized Vector<String> getRemoteHostDirectAddresses(String id, int type) {
        Vector<String> retour = new Vector<String>();
        HashMap<String, RemoteHostAddressDescriptor> descrs = registeredHosts.get(id).getRemoteHostAddresseDescriptors();
        for (String cle : descrs.keySet()) {
            NetworkAddress na = new NetworkAddress(cle);
            if ((na.getType() == type) && (descrs.get(cle).isDirect())) retour.addElement(cle);
        }
        return retour;
    }

    // Methodes permettant de recuperer l'ID d'un hote a partir de l'une de ses adresses
    /**
     * Returns the unique identifier of the host tha had the specified address
     * @param adr address of the host to find
     * @return the unique identifier of the host tha had the specified address
     */
    public String getHostID(String adr) {
        return getHostID(new NetworkAddress(adr));
    }
    /**
     * Returns the unique identifier of the host tha had the specified address
     * @param adr address of the host to find
     * @return the unique identifier of the host tha had the specified address
     */
    public synchronized String getHostID(NetworkAddress adr) {
        String retour = null;
        for (String cle : registeredHosts.keySet()) {
            RemoteHostDescriptor courant = registeredHosts.get(cle);
            if (courant.isPresentAddress(adr)) {
                retour = cle;
                break;
            }
        }
        return retour;
    }

    // Methodes de modification et recuperation du decalage d'horloge avec un hote
    /**
     * Indicate if the clock shift of a host is available
     * @param id unique identifier of the host to get information on
     * @return true if the clock shift of a host is available
     */
    public synchronized boolean isRemoteHostClockShiftAvailable(String id) { return registeredHosts.get(id).isClockShiftAvailable(); }
    /**
     * Returns the clock shift of a host is available (if not the returned value is undetermined)
     * @param id unique identifier of the host to get information on
     * @return the clock shift of a host is available (if not the returned value is undetermined)
     */
    public synchronized long getRemoteHostClockShift(String id) { return registeredHosts.get(id).getClockShift(); }
    /**
     * Returns the clock shit accuracy  of a host is available (if not the returned value is -1).
     * The accuracy means that the real clock shift can be + or - this value
     * @param id unique identifier of the host to get information on
     * @return the clock shit accuracy  of a host is available (if not the returned value is -1).
     */
    public synchronized long getRemoteHostClockAccuracy(String id) { return registeredHosts.get(id).getClockAccuracy(); }
    /**
     * Ajust the clock shift of a host. The store one is modified only if its accuracy is worst than the given one.
     * @param id unique identifier of the host to ajust
     * @param cs new clock shift
     * @param acc new clock accuracy
     */
    public synchronized void adjustRemoteHostClockShift(String id, long cs, long acc) {
        if (registeredHosts.get(id) != null) registeredHosts.get(id).adjustClockShift(cs, acc);
    }

    // Methodes de modification et de recuperation du tempos de vie d'un hote
    /**
     * Gets the remaning time to live of a host
     * @param id unique identifier of the host to get the time to live
     * @return the remaning time to live of the host
     */
    public synchronized long getTimeToLive(String id) { return registeredHosts.get(id).getTimeToLive(); }

    private class RefreshDnsTask extends TimerTask {
         public void run() {
            // decremente la durre de vie de chaque hote (sauf l'hote local)
            // supprime les hotes dont la duree de vie est terminee
            ajuste();
         }
     }
     private synchronized void ajuste() {
         Vector<String> aSupprimer = new Vector<String>();
         for (String cle : registeredHosts.keySet()) {
            if (!cle.equals(myID)) {
                RemoteHostDescriptor courant = registeredHosts.get(cle);
                if (courant.getTimeToLive() > Parameters.DNS_UPDATE_RATE) {
                    courant.decrementTimeToLive(Parameters.DNS_UPDATE_RATE);
                }
                else {
                    aSupprimer.addElement(cle);
                }
            }
        }
        for (int i=0; i<aSupprimer.size(); i++) {
            registeredHosts.remove(aSupprimer.elementAt(i));
            System.out.println(aSupprimer.elementAt(i)+" removed from DNS");
        }
     }

    private class DiffusionDnsTask extends TimerTask {
        public void run() {
            envoyer();
        }
    }
    private synchronized void envoyer() {
        for (String cle : registeredHosts.keySet()) {
            if ((!cle.equals(myID)) && (!cle.equals("Deployment"))) {
                RemoteHostDescriptor courant = registeredHosts.get(cle);
                Vector<String> destinataires = courant.getAllDirectAddresses();
                for (int i=0; i<destinataires.size(); i++) {
                    NetworkPlatformMessage envoi = new NetworkPlatformMessage();
                    envoi.setFinalAddress("local");
                    envoi.setSerializedObject(getHostsAsByteArray());
                    envoi.setOwner(Parameters.KALIMUCHO_DNS_MANAGER);
                    envoi.setReplyTo(Parameters.KALIMUCHO_DNS_MANAGER);
                    envoi.setExpeditorAdressWhenSending();
                    envoi.setAddress(destinataires.elementAt(i));
                    nec.getPlatformMessagesEmitter().postSlowDirectMessage(envoi);
                }
            }
        }
    }

    // Methode de trace du DNS (a ne pas conserver)
    /**
     * For test purpose only: dumps the content of the DNS on console.
     */
    public synchronized void dump() {
        for (String cle : registeredHosts.keySet()) {
            RemoteHostDescriptor courant = registeredHosts.get(cle);
            if (courant.isClockShiftAvailable()) System.out.println("Host ID: "+cle+" TTL: "+courant.getTimeToLive()+" clock shift: "+courant.getClockShift()+" +/-"+(courant.getClockAccuracy()/2));
            else System.out.println("Host ID: "+cle+" TTL: "+courant.getTimeToLive()+" clock shift: not available");
            HashMap<String, RemoteHostAddressDescriptor> descrs = courant.getRemoteHostAddresseDescriptors();
            for (String adresse : descrs.keySet()) {
                System.out.println("  "+adresse+" ajoute ="+descrs.get(adresse).isAdded()+" direct ="+descrs.get(adresse).isDirect());
            }
        }
    }

}
