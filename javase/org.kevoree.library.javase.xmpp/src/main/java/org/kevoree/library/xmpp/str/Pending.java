package org.kevoree.library.xmpp.str;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 14:34
 * To change this template use File | Settings | File Templates.
 */
public abstract class Pending {

    protected String to;
    private long creationStamp = System.currentTimeMillis();

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public long getCreationStamp() {
        return creationStamp;
    }
}
