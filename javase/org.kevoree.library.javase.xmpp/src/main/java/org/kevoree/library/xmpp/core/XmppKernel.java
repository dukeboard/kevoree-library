package org.kevoree.library.xmpp.core;

import org.jivesoftware.smack.XMPPConnection;
import org.kevoree.library.xmpp.mngr.ChatManager;
import org.kevoree.library.xmpp.mngr.ConnectionManager;
import org.kevoree.library.xmpp.mngr.ContactsManager;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 14:45
 * To change this template use File | Settings | File Templates.
 */
public class XmppKernel {

    private ConnectionManager connectionManager;
    private XMPPConnection xmppConnection;
    private ContactsManager contactsManager;
    private ChatManager chatsManager;

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public XMPPConnection getXmppConnection() {
        return xmppConnection;
    }

    public void setXmppConnection(XMPPConnection xmppConnection) {
        this.xmppConnection = xmppConnection;
    }

    public ContactsManager getContactsManager() {
        return contactsManager;
    }

    public void setContactsManager(ContactsManager contactsManager) {
        this.contactsManager = contactsManager;
    }

    public ChatManager getChatsManager() {
        return chatsManager;
    }

    public void setChatsManager(ChatManager chatsManager) {
        this.chatsManager = chatsManager;
    }
}
