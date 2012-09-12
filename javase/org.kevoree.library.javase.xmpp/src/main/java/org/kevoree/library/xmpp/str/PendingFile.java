package org.kevoree.library.xmpp.str;

import org.jivesoftware.smack.MessageListener;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 14:33
 * To change this template use File | Settings | File Templates.
 */
public class PendingFile extends Pending {

    private File file;
    private String description;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
