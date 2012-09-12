package org.kevoree.library.xmpp;

import org.jivesoftware.smack.packet.Presence;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

/**
 *
 * @author ffouquet, gnain
 */
public class NewMain {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here

        System.out.println("Beginning XMPP test");
       // XmppComponent compo = new XmppComponent();

        HashMap<String,Object> p = new HashMap<String,Object>();
        p.put("login","entimid@gmail.com");
        p.put("password","entimidpass");

      //  compo.setDictionary(p);

      //  compo.start();

        Properties msg = new Properties();
        msg.put("to","gregory.nain@gmail.com");
        msg.put("content", "Yeepee");

       // compo.sendMessage(msg);

        Thread.sleep(2 * 40 * 1000);

     //   compo.stop();
    }


}
