package org.kevoree.library.xmpp.mngr;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.kevoree.library.xmpp.core.XmppKernel;
import org.kevoree.library.xmpp.lst.DefaultMessageListener;
import org.kevoree.library.xmpp.str.Pending;
import org.kevoree.library.xmpp.str.PendingFile;
import org.kevoree.library.xmpp.str.PendingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 14:46
 * To change this template use File | Settings | File Templates.
 */
public class ChatManager {

    private XmppKernel kernel;

    private Map<String, Chat> activeChats = new HashMap<String, Chat>();
    private Map<String, List<Pending>> pendings = new HashMap<String, List<Pending>>();
    private static final Logger logger = LoggerFactory.getLogger(ChatManager.class);

    //private DefaultMessageListener defaultListener = new DefaultMessageListener();

    public ChatManager(XmppKernel kernel) {
        this.kernel = kernel;
        kernel.setChatsManager(this);
    }

    public void sendMessage(String message, String to, MessageListener listener) {
        if(!kernel.getContactsManager().isRecipientInContactsList(to) ||
                !kernel.getContactsManager().isAvailable(to)) {
            PendingMessage pen = new PendingMessage();
            pen.setListener(listener);
            pen.setMessage(message);
            pen.setTo(to);

            if(!pendings.containsKey(to)) {
                pendings.put(to,new ArrayList<Pending>());
            }

            pendings.get(to).add(pen);

            if(!kernel.getContactsManager().isRecipientInContactsList(to)) {
                kernel.getContactsManager().addRecipient(to);
            }

        } else {

            try {

                if (!activeChats.containsKey(to)) {
                    activeChats.put(to, kernel.getXmppConnection().getChatManager().createChat(to, listener));
                }
                activeChats.get(to).sendMessage(message);

            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
    }


    public void processPendings(String to) {
        if(pendings.containsKey(to)) {
            List<Pending> awaitingMessages = pendings.get(to);
            for(Pending p : awaitingMessages) {
                if(p instanceof PendingMessage) {
                    PendingMessage msg = (PendingMessage)p;
                    sendMessage(msg.getMessage(), msg.getTo(), msg.getListener());
                } else if(p instanceof PendingFile) {
                    PendingFile msg = (PendingFile)p;
                    sendFile(msg.getFile(), msg.getDescription(), msg.getTo());
                } else {
                    logger.warn("Pending message type UNKNOWN: " + p.getClass().getName());
                }
            }
        } else {
           logger.debug("no pending messages for: " + to);
        }
    }


    public void sendFile(File f, String description, String to) {
        if(!kernel.getContactsManager().isRecipientInContactsList(to) ||
                !kernel.getContactsManager().isAvailable(to)) {
            PendingFile pen = new PendingFile();
            pen.setTo(to);
            pen.setFile(f);
            pen.setDescription(description);

            if(!pendings.containsKey(to)) {
                pendings.put(to,new ArrayList<Pending>());
            }

            pendings.get(to).add(pen);

            if(!kernel.getContactsManager().isRecipientInContactsList(to)) {
                kernel.getContactsManager().addRecipient(to);
            }

        } else {

            try {

                ServiceDiscoveryManager sdm = new ServiceDiscoveryManager(kernel.getXmppConnection());
                Iterator<String> it = sdm.getFeatures();
                while(it.hasNext()) {
                    String feature = it.next();
                    System.out.println("Feature:" + feature);
                }

                FileTransferNegotiator.setServiceEnabled(kernel.getXmppConnection(), true);

                // Create the file transfer manager
                FileTransferManager manager = new FileTransferManager(kernel.getXmppConnection());

                // Create the outgoing file transfer
                final OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(to);

                // Send the file
                transfer.sendFile(f,description);
                //final  OutputStream os = transfer.sendFile(f.getName(), f.getTotalSpace(), description);
                //final FileInputStream fis = new FileInputStream(f);

                new Thread(new Runnable(){
                    public void run() {
                        try {

                            while(true) {
                                System.out.println("testing transfer status");
                                if(transfer.getStatus() == FileTransfer.Status.error) {
                                    System.out.println("ERROR!!! " + transfer.getError());
                                } else {
                                    System.out.println("Getting transfer status");
                                    System.out.println(transfer.getStatus());
                                    System.out.println(transfer.getProgress());
                                    /*
                                    byte[] buffer = new byte[1024];
                                    fis.read(buffer);
                                    os.write(buffer);
                                    */
                                }

                                System.out.println("preparing for sleeping");
                                Thread.sleep(1000);
                                System.out.println("WakeUp");
                                System.out.println("Status:" + transfer.getStatus());
                                System.out.println("Progress:" + transfer.getProgress() + "("+transfer.getBytesSent()+")");
                                System.out.println("Exp:" + transfer.getException());
                                System.out.println("Error:" + transfer.getError());

                                System.out.println("Transfer done ?:" + transfer.isDone());
                            }
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted");
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }).start();

            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
    }


}
