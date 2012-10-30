package org.kevoree.library.xmpp.mngr;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.kevoree.library.xmpp.core.XmppKernel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 10:54
 * To change this template use File | Settings | File Templates.
 */
public class ContactsManager implements RosterListener {

    private XmppKernel kernel;
    private static final Logger logger = LoggerFactory.getLogger(ContactsManager.class);

    public ContactsManager(XmppKernel kernel) {
        this.kernel = kernel;
        kernel.setContactsManager(this);
    }

    public Collection<RosterEntry> getContactList() {
        return kernel.getXmppConnection().getRoster().getEntries();
    }

    public boolean isRecipientInContactsList(String recipient) {
        return kernel.getXmppConnection().getRoster().contains(recipient);
    }

    public boolean isAvailable(String to) {
        Presence p = kernel.getXmppConnection().getRoster().getPresence(to);
        return p.getType() == Presence.Type.available;
    }

    public boolean addRecipient(String recipient) {
        try {
            if(!isRecipientInContactsList(recipient)) {
                kernel.getXmppConnection().getRoster().createEntry(recipient, "", new String[0]);
            }

            return true;
        } catch (XMPPException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return false;
    }

    public boolean removeRecipient(String recipientAddress) {
        RosterEntry r = kernel.getXmppConnection().getRoster().getEntry(recipientAddress);
        if(r!= null) {
            try {
                kernel.getXmppConnection().getRoster().removeEntry(r);
            } catch (XMPPException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return false;
            }
        }
        return true;
    }

    public String printContactsStats() {
        String stats = "";
        for(RosterEntry entry : getContactList()) {
            Presence p = kernel.getXmppConnection().getRoster().getPresence(entry.getUser());

            stats += ("\tPresence: "+entry.getUser()+"[" + p.getType() + "," + p.getMode() + "," + p.getStatus()+"]\n");
            stats += ("\tEntry: "+entry.getUser()+"[" + entry.getName() + "," + entry.getUser() + "," + entry.getStatus() + "," + entry.getType() + "]\n");

            for(String propName : p.getPropertyNames()) {
                stats += ("\t\tP:"+ propName + "=" + p.getProperty(propName) + "\n");
            }
            for(PacketExtension ext : p.getExtensions()) {
                stats += ("\t\tE:"+ ext.toXML() + "\n");
            }
        }
        logger.debug(stats);
        return stats;
    }

    @Override
    public void entriesAdded(Collection<String> addedContacts) {
       String temp = ("Xmpp::ContactsManager::Contacts added:\n");
        for(String contact : addedContacts) {
            temp += ("\t"+contact+"\n");
        }
       logger.debug(temp);
    }

    @Override
    public void entriesUpdated(Collection<String> updatedContacts) {
        String temp = "Xmpp::ContactsManager::Contacts updated:\n";
        for(String contact : updatedContacts) {
            RosterEntry entry = kernel.getXmppConnection().getRoster().getEntry(contact);
            Presence p = kernel.getXmppConnection().getRoster().getPresence(contact);

            temp += ("\tPresence: "+contact+"[" + p.getType() + "," + p.getMode() + "," + p.getStatus()+"]\n");
            temp += ("\tEntry: "+contact+"[" + entry.getName() + "," + entry.getUser() + "," + entry.getStatus() + "," + entry.getType() + "]\n");


            for(String propName : p.getPropertyNames()) {
                temp += ("\t\tP:"+ propName + "=" + p.getProperty(propName) + "\n");
            }
            for(PacketExtension ext : p.getExtensions()) {
                temp += ("\t\tE:"+ ext.toXML() + "\n");
            }

        }
        logger.debug(temp);
    }

    @Override
    public void entriesDeleted(Collection<String> removedContacts) {
        String temp = "Xmpp::ContactsManager::Contacts DELETED:\n";
        for(String contact : removedContacts) {
            temp += "\t"+contact+"\n";
        }
        logger.debug(temp);
    }

    @Override
    public void presenceChanged(Presence presence) {
        logger.debug(presence.getFrom() + " is now " + presence.getType());
        if(presence.getType() == Presence.Type.available) {
            logger.debug(presence.getFrom().substring(presence.getFrom().indexOf("/")) + " just Connected ! Triggering pending messages treatment");
            kernel.getChatsManager().processPendings(presence.getFrom().substring(0,presence.getFrom().indexOf("/")));
        }
    }

}
