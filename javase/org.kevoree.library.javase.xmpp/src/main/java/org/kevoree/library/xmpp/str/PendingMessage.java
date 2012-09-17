package org.kevoree.library.xmpp.str;

import org.jivesoftware.smack.MessageListener;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 14:33
 * To change this template use File | Settings | File Templates.
 */
public class PendingMessage extends Pending {

    private String message;
    private MessageListener listener;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageListener getListener() {
        return listener;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }
    public String toString() {
        return "PendingMessage at " + getCreationStamp() + " for " + getTo() + ": '"+getMessage()+"'";
    }
}
