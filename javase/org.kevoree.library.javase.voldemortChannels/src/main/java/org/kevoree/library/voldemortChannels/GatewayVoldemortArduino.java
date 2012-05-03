package org.kevoree.library.voldemortChannels;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.KevoreeFragmentPropertyHelper;
import org.kevoree.framework.KevoreePlatformHelper;
import org.kevoree.framework.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voldemort.client.StoreClient;
import voldemort.cluster.Node;
import voldemort.versioning.Versioned;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 19/04/12
 * Time: 17:33
 */

@Library(name="JavaSE")
@Provides({
        @ProvidedPort(name = "msg", type = PortType.MESSAGE)
})

@ComponentType
public class GatewayVoldemortArduino extends AbstractComponentType implements Runnable  {

    private Logger logger = LoggerFactory.getLogger(GatewayVoldemortArduino.class);
    private List<Node> nodes = new ArrayList<Node>();
    private  Thread thread;
    private Node currentNode=null;
    @Start
    public void start() {
        thread = new Thread(this);

        thread.start();
    }

    @Stop
    public void stop() {
        //NOP
    }

    @Update
    public void update() {
        nodes.clear();
        thread.start();
    }


    @Port(name = "msg")
    public void appendIncoming(Object msg) {


        if(msg instanceof  Message) {

            Message currentmsg = (Message) msg;

            String remoteNodeName = currentmsg.getDestNodeName();

            KClient t = new KClient(currentNode,nodes);
            StoreClient store = t.getStore("kevoree");

            Versioned data =  store.get(remoteNodeName);
            Versioned<Message> version=null;

            if(data !=null)
            {
                // get the value
                version = store.get(remoteNodeName);
                // modify the value
                version.setObject(currentmsg);

                // update the value
                store.put(remoteNodeName,version);
            }else
            {
                store.put(remoteNodeName,msg);
            }

        }


    }
    public String getAddressModel(String remoteNodeName) {
        String ip = KevoreePlatformHelper.getProperty(this.getModelService().getLastModel(), remoteNodeName,
                org.kevoree.framework.Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP());
        if (ip == null || ip.equals("")) {
            ip = "127.0.0.1";
        }
        return ip;
    }


    public List<String> getAllNodes () {
        ContainerRoot model = this.getModelService().getLastModel();
        for (Object o : model.getGroupsForJ()) {
            Group g = (Group) o;
            List<String> peers = new ArrayList<String>(g.getSubNodes().size());
            for (ContainerNode node : g.getSubNodesForJ()) {
                peers.add(node.getName());
            }
            return peers;
        }
        return new ArrayList<String>();
    }


    public int getport(String nodeName,String port) throws IOException {
        try {
            //logger.debug("look for port on " + nodeName);
            return KevoreeFragmentPropertyHelper.getIntPropertyFromFragmentChannel(getModelService().getLastModel(), getName(), port, nodeName);
        } catch (NumberFormatException e)
        {
            throw new IOException(e.getMessage());
        }
    }


    @Override
    public void run() {
        try
        {
            List<String> listNodes =   getAllNodes();
            logger.error("Number of node "+listNodes.size());

            for(String _node : listNodes)
            {
                String hostname = getAddressModel(_node);
                int id =getport(_node, "id");
                int httpPort = getport(_node, "httpPort");
                int socketPort  = getport(_node, "socketPort");
                int adminPort  = getport(_node, "adminPort");
                String partition =   KevoreeFragmentPropertyHelper.getPropertyFromFragmentChannel(getModelService().getLastModel(), getName(), "partitions", _node);
                List<Integer> partitions =  new ArrayList<Integer>();
                StringTokenizer st = new StringTokenizer(partition, ";");
                while (st.hasMoreTokens())
                {
                    partitions.add(Integer.parseInt(st.nextToken()));
                }
                logger.error("Node "+id+" httpPort="+httpPort+" "+" socketPort="+socketPort+" adminPort="+adminPort+" partitions="+partitions);
                Node node = new Node(id,hostname,httpPort,socketPort,adminPort,partitions);
                nodes.add(node);

                if(_node.equals(getNodeName()))
                                {
                                    currentNode  = node;
                                }
            }


        } catch (IOException e) {
            logger.error("The cluster can't be configure "+e);
        }
    }
}
