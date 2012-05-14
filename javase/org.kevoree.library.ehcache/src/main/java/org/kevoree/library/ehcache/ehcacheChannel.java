package org.kevoree.library.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractChannelFragment;
import org.kevoree.framework.ChannelFragmentSender;
import org.kevoree.framework.KevoreeFragmentPropertyHelper;
import org.kevoree.framework.KevoreePlatformHelper;
import org.kevoree.framework.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 11/05/12
 * Time: 14:46
 */

@Library(name = "JavaSE", names = {"Android"})
@ChannelTypeFragment
@DictionaryType({
        @DictionaryAttribute(name = "cacheName", defaultValue = "kevoreeCluster", optional = false),
        @DictionaryAttribute(name = "port", optional = false,fragmentDependant = true)
})
public class ehcacheChannel extends AbstractChannelFragment implements Runnable {

    private Thread handler=null;
    private  boolean alive=true;
    private Logger logger = LoggerFactory.getLogger(ehcacheChannel.class);
    @Start
    public void startChannel() {
        alive = true;
        handler = new Thread(this);
        handler.start();
    }

    @Stop
    public void stopChannel() {
        try
        {
            alive = false;
            handler.interrupt();
        } catch (Exception e) {
            //ignore
        }



    }

    @Update
    public void updateChannel() {


    }

    @Override
    public Object dispatch(Message message) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ChannelFragmentSender createSender(String s, String s1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void run() {

        String cacheName =   "jed";
        StringBuilder factoryconfprop = new StringBuilder();

        try
        {
            List<String> listNodes =   getAllNodes();
            logger.debug("Number of node "+listNodes.size());

            for(String _node : listNodes)
            {
                if(!_node.equals(getNodeName())){
                    String hostname = getAddressModel(_node);
                    int port =getport(_node, "port");
                    String rmi = "//"+hostname+":"+port+"/"+cacheName+"|";
                    factoryconfprop.append(rmi);
                }
            }

        } catch (Exception e) {
            logger.error("The cluster can't be configure "+e);
        }


        // remote peer
        FactoryConfiguration factoryConfig = new FactoryConfiguration();
        factoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory");
        factoryConfig.setProperties("peerDiscovery=manual, rmiUrls="+factoryconfprop.toString());
        factoryConfig.setPropertySeparator(",");



        logger.debug("Remote peer properties ="+factoryConfig.getProperties());



        String rmilocal="";
        int port = 0;
        try {
            String hostname = getAddressModel(getNodeName());
            port = getport(getNodeName(), "port");
            rmilocal ="hostName="+hostname+", port="+port+", socketTimeoutMillis=120000";
        } catch (IOException e) {
            logger.error("",e);
        }
        CacheManager   cacheManager=null;
        try{
            // local node
            FactoryConfiguration listenerFactoryConfig = new FactoryConfiguration();
            listenerFactoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory");
            listenerFactoryConfig.setProperties(rmilocal);
            listenerFactoryConfig.setPropertySeparator(",");

            logger.debug("Local peer properties ="+listenerFactoryConfig.getProperties());

            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setMaxEntriesLocalHeap(100);
            /*
cacheConfiguration.setEternal(true);
cacheConfiguration.overflowToDisk(true);
cacheConfiguration.setTimeToLiveSeconds(300);
cacheConfiguration.setTimeToIdleSeconds(3000);
            */
            CacheConfiguration.CacheEventListenerFactoryConfiguration  cachEvent =  new CacheConfiguration.CacheEventListenerFactoryConfiguration();
            cachEvent.setClass("net.sf.ehcache.distribution.RMICacheReplicatorFactory");
            cachEvent.setProperties("replicateAsynchronously=true, replicatePuts=true,  replicatePutsViaCopy=false, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true, asynchronousReplicationIntervalMillis=50");
            cachEvent.setPropertySeparator(",");


            cacheConfiguration.addCacheEventListenerFactory(cachEvent);

            CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactoryConfiguration = new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration();
            bootstrapCacheLoaderFactoryConfiguration.setClass("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory");
            bootstrapCacheLoaderFactoryConfiguration.setProperties("bootstrapAsynchronously=true, maximumChunkSizeBytes=5000000");
            bootstrapCacheLoaderFactoryConfiguration.setPropertySeparator(",");

            cacheConfiguration.addBootstrapCacheLoaderFactory(bootstrapCacheLoaderFactoryConfiguration);


            Configuration configuration = new Configuration();
            configuration.setDefaultCacheConfiguration(cacheConfiguration);
            configuration.addCacheManagerPeerProviderFactory(factoryConfig);
            configuration.addCacheManagerPeerListenerFactory(listenerFactoryConfig);

            cacheManager = CacheManager.create(configuration);
            Cache cache = new Cache(cacheConfiguration);
            cache.setName(cacheName);
            cacheManager.addCache(cache);

        } catch (Exception e) {
            logger.error("",e);
        }


        if(cacheManager !=null)
        {
            Message message = new Message();
            message.setContent(cacheManager);

            for (org.kevoree.framework.KevoreePort p : getBindedPorts()  )
            {
                forward(p, message);
            }
        } else {
            logger.error("CacheManager is NULL");
        }



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
}
