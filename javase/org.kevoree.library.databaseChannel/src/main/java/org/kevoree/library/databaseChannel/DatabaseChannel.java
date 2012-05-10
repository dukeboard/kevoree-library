package org.kevoree.library.databaseChannel;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 18/04/11
 * Time: 08:58
 */

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactoryEnum;
import net.sf.hajdbc.dialect.DialectFactoryEnum;
import net.sf.hajdbc.sql.DataSource;
import net.sf.hajdbc.sql.DataSourceDatabase;
import net.sf.hajdbc.sql.DataSourceDatabaseClusterConfiguration;
import net.sf.hajdbc.sql.SQLProxy;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import org.apache.derby.drda.NetworkServerControl;
import org.daum.hajdbc.UrlDataSource;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.annotation.ChannelTypeFragment;
import org.kevoree.annotation.*;
import org.kevoree.framework.*;
import org.kevoree.framework.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

@Library(name = "JavaSE", names = {"Android"})
@ChannelTypeFragment
@DictionaryType({
        @DictionaryAttribute(name = "clusterName", defaultValue = "kevoreeCluster", optional = false),
        @DictionaryAttribute(name = "db", defaultValue = "derby", optional = false, vals = {"derby", "hsqldb", "h2", "postgresql", "mysql"}),
        @DictionaryAttribute(name = "login", defaultValue = "daum", optional = false),
        @DictionaryAttribute(name = "password", defaultValue = "daum", optional = false),
        @DictionaryAttribute(name = "port", optional = false,fragmentDependant = true),
        @DictionaryAttribute(name = "dbname", optional = false,fragmentDependant = true)
})

public class DatabaseChannel extends AbstractChannelFragment implements Runnable {

    private Logger logger = LoggerFactory.getLogger(DatabaseChannel.class);
    private Thread handler=null;
    final Semaphore sem = new java.util.concurrent.Semaphore(1);
    private  Boolean alive=true;
    private  HashMap<String,DataSourceDatabase> dataSourceDatabases = new HashMap<String,DataSourceDatabase>();
    private SQLProxy<javax.sql.DataSource, DataSourceDatabase, javax.sql.DataSource, SQLException> proxy;
    private NetworkServerControl serverControl= null;


    @Override
    public Object dispatch(Message message) {
        for (KevoreeChannelFragment cf : getOtherFragments()) {
            if (!message.getPassedNodes().contains(cf.getNodeName())) {
                forward(cf, message);
            }
        }
        return null;
    }

    public String getAddress(String remoteNodeName) {
        String ip = KevoreePlatformHelper.getProperty(this.getModelService().getLastModel(), remoteNodeName,
                org.kevoree.framework.Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP());
        if (ip == null || ip.equals("")) {
            ip = "127.0.0.1";
        }
        return ip;
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

    @Start
    public void startChannel() {
        alive = true;
        handler = new Thread(this);
        handler.start();
    }

    @Stop
    public void stopChannel() {
        alive = false;
        handler.interrupt();
        try {
            serverControl.shutdown();
        } catch (Exception e) {
            //ignore
        }
    }

    @Update
    public void updateChannel() {


    }


    @Override
    public ChannelFragmentSender createSender(final String remoteNodeName, String remoteChannelName) {
        return new ChannelFragmentSender() {
            @Override
            public Object sendMessageToRemote(Message msg) {
                try {
                    sem.acquire();
                } catch (InterruptedException e) {
                    // ignore
                }

                try
                {




                } catch (Exception e) {

                    //ignore
                }
                finally
                {
                    msg = null;
                    sem.release();
                }

                return null;
            }
        };
    }


    private  boolean isServerStarted(NetworkServerControl server)
    {
        try {
            server.ping();
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }


    @Override
    public void run() {
        try
        {
            int current_node_derbyport =   KevoreeFragmentPropertyHelper.getIntPropertyFromFragmentChannel(getModelService().getLastModel(), getName(), "port", getNodeName());
            logger.debug("Starting SGBD "+current_node_derbyport);

            //accepts connections from other hosts on an IPv6 system
            // serverControl =      new NetworkServerControl(InetAddress.getByName("::"),1527);

            //accepts connections from other hosts on an IPv4 system
            serverControl =   new NetworkServerControl(InetAddress.getByName("0.0.0.0"),current_node_derbyport);
            serverControl.start(new PrintWriter(System.out, true));

            // Wait for server to come up
            for (int j = 0; j < 60; j++)
            {
                Thread.sleep(1000);
                if (isServerStarted(serverControl))
                    break;
            }
        } catch (Exception e)
        {
            logger.error("SGBD Error :",e);
        }

        List<String> listNodes =   getAllNodes();
        logger.debug("Number of node "+listNodes.size());


        String login =   this.getDictionary().get("login").toString();
        String password = this.getDictionary().get("password").toString();

        try
        {
            for(String _node : listNodes)
            {
                String hostname = getAddressModel(_node);
                int derbyport =getport(_node, "port");
                DataSourceDatabase current = new DataSourceDatabase();
                current.setId(_node);
                current.setName(UrlDataSource.class.getName());

                String url =    "jdbc:derby://"+hostname+":"+derbyport+"/"+_node+";create=true";
                current.setProperty("url",url );
                current.setUser(login);
                current.setPassword(password);
                logger.debug("Create database  "+_node+" on "+_node+" "+derbyport);
                dataSourceDatabases.put(_node, current);
            }


        } catch (IOException e) {
            logger.error("The cluster can't be configure "+e);
        }
        Message message = new Message();
        message.setContent(dataSourceDatabases);

        for (org.kevoree.framework.KevoreePort p : getBindedPorts()  )
        {
            forward(p, message);
        }
    }
}


