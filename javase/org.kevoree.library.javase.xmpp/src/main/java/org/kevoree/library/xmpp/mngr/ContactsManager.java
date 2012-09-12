package org.kevoree.library.xmpp.mngr;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.kevoree.library.xmpp.core.XmppKernel;

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

    public void printContactsStats() {
        for(RosterEntry entry : getContactList()) {
            Presence p = kernel.getXmppConnection().getRoster().getPresence(entry.getUser());

            System.out.print("\tPresence: "+entry.getUser()+"[" + p.getType() + "," + p.getMode() + "," + p.getStatus()+"]\n");
            System.out.print("\tEntry: "+entry.getUser()+"[" + entry.getName() + "," + entry.getUser() + "," + entry.getStatus() + "," + entry.getType() + "]\n");

            for(String propName : p.getPropertyNames()) {
                System.out.print("\t\tP:"+ propName + "=" + p.getProperty(propName) + "\n");
            }
            for(PacketExtension ext : p.getExtensions()) {
                System.out.print("\t\tE:"+ ext.toXML() + "\n");
            }
        }
    }

    @Override
    public void entriesAdded(Collection<String> addedContacts) {
        System.out.print("Xmpp::ContactsManager::Contacts added:\n");
        for(String contact : addedContacts) {
            System.out.print("\t"+contact+"\n");
        }
        System.out.println();
    }

    @Override
    public void entriesUpdated(Collection<String> updatedContacts) {
        System.out.print("Xmpp::ContactsManager::Contacts updated:\n");
        for(String contact : updatedContacts) {
            RosterEntry entry = kernel.getXmppConnection().getRoster().getEntry(contact);
            Presence p = kernel.getXmppConnection().getRoster().getPresence(contact);

            System.out.print("\tPresence: "+contact+"[" + p.getType() + "," + p.getMode() + "," + p.getStatus()+"]\n");
            System.out.print("\tEntry: "+contact+"[" + entry.getName() + "," + entry.getUser() + "," + entry.getStatus() + "," + entry.getType() + "]\n");


            for(String propName : p.getPropertyNames()) {
                System.out.print("\t\tP:"+ propName + "=" + p.getProperty(propName) + "\n");
            }
            for(PacketExtension ext : p.getExtensions()) {
                System.out.print("\t\tE:"+ ext.toXML() + "\n");
            }

        }
        System.out.println();
    }

    @Override
    public void entriesDeleted(Collection<String> removedContacts) {
        System.out.print("Xmpp::ContactsManager::Contacts DELETED:\n");
        for(String contact : removedContacts) {
            System.out.print("\t"+contact+"\n");
        }
        System.out.println();
    }

    @Override
    public void presenceChanged(Presence presence) {
        System.out.println("Xmpp::ContactsManager::" + presence.getFrom() + " is now " + presence.getType());
        if(presence.getType() == Presence.Type.available) {
            System.out.println(presence.getFrom().substring(presence.getFrom().indexOf("/")) + " just Connected ! Triggering pending messages treatment");
            kernel.getChatsManager().processPendings(presence.getFrom().substring(0,presence.getFrom().indexOf("/")));
        }
    }

}
