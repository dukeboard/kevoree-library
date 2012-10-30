/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kevoree.library.xmpp.cmp;

import org.jivesoftware.smack.RosterEntry;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.annotation.*;
import org.kevoree.framework.MessagePort;
import org.kevoree.library.xmpp.mngr.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 *
 * @author gnain
 */
@Provides({
        @ProvidedPort(name = "send", type = PortType.MESSAGE)
})
@Requires({
        @RequiredPort(name = "messageReceived", type = PortType.MESSAGE, needCheckDependency = true, optional = false)
})
@Library(name = "JavaSE")
@DictionaryType({@DictionaryAttribute(name = "login"),@DictionaryAttribute(name = "password")})
@ComponentType
public class XmppComponent extends AbstractComponentType {
    private static final Logger logger = LoggerFactory.getLogger(XmppComponent.class);

    private ConnectionManager client;
   // private LocalMessageListener defaultListener;

    @Port(name = "send")
    public void sendMessage(Object message) {
        logger.debug("XMPP Send msg =>" + message.toString());
        Properties msg = (Properties)message;
        System.out.println("Sending message to: " + msg.get("to"));
        System.out.println(msg.get("content"));

   //     client.sendMessage((String)msg.get("content"), (String)msg.get("to"), defaultListener);
        System.out.println(msg.get("Sent"));
    }

    public void messageReceived(String message) {
        getPortByName("messageReceived", MessagePort.class).process(message);
    }

    @Start
    public void start() {
        logger.info("Starting");
        logger.debug("Credentials: '" + (String)getDictionary().get("login") + "':'" + (String)getDictionary().get("password") + "'");
        client = new ConnectionManager();
        if(client.login((String)getDictionary().get("login"), (String)getDictionary().get("password"))) {
  //      defaultListener = new LocalMessageListener(this);
   //     client.setDefaultResponseStrategy(defaultListener);
            logger.info("Started");
        } else {
            logger.warn("Error while connecting to the server.");
        }

    }

    @Stop
    public void stop() {
        logger.info("Closing...");
        /*
        System.out.print("Current contact list:\n");
        for(RosterEntry entry : client.getContacts()) {
            System.out.print("\t" + entry.getUser() + " as " + entry.getName() + " is "+entry.getStatus() + "\n");
            //System.out.print("\t"+entry.toString()+"\n");
        }
        System.out.println();
         */
        client.disconnect();
        logger.info("Closed.");
    }

}
