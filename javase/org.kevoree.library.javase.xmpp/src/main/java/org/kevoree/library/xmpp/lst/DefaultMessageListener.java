package org.kevoree.library.xmpp.lst;


import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

public class DefaultMessageListener implements MessageListener {

    public DefaultMessageListener() {

    }

    public void processMessage(Chat arg0, Message arg1) {

        System.out.print("Message Received in Chat:"+arg0.getThreadID()+"\n");
        System.out.print("\t" + arg1.getBody());
    }
}
