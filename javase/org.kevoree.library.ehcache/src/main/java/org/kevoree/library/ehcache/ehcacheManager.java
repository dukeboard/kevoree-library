package org.kevoree.library.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.KevoreePlatformHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 11/05/12
 * Time: 17:09
 */

@Library(name = "JavaSE")
@ComponentType

@DictionaryType({
        @DictionaryAttribute(name = "PeerListenerPort", optional = false),
        @DictionaryAttribute(name = "multicastGroupAddress", defaultValue = "230.0.0.1",optional = false),
        @DictionaryAttribute(name = "multicastGroupPort", defaultValue = "4446",optional = false),
        @DictionaryAttribute(name = "multicastTimeToLive", defaultValue = "32",optional = false)
})
@Provides({
        @ProvidedPort(name = "ehcacheChannel", type = PortType.MESSAGE),
        @ProvidedPort(name = "ehCacheService", type = PortType.SERVICE, className = IehcacheService.class)

})
public class ehcacheManager extends AbstractComponentType implements Runnable,  IehcacheService{
    private Logger logger = LoggerFactory.getLogger(ehcacheManager.class);
    private Thread handler=null;
    private  CacheManager cacheManager=null;
    private Semaphore verrou = new Semaphore(0);
    private  boolean alive=true;
    @Start
    public void start() {
        handler = new Thread(this);
        handler.start();
    }

    @Stop
    public void stop()
    {
        try
        {
            alive = false;
            handler.interrupt();
        } catch (Exception e) {
            //ignore
        }
    }

    @Port(name = "ehcacheChannel")
    public void confCluster(Object obj) {

    }

    @Update
    public void update()
    {

    }

    @Override
    @Port(name = "ehCacheService", method = "getCacheManger")
    public CacheManager getCacheManger() {
        if(cacheManager == null)
        {
            try {
                verrou.tryAcquire(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.error("The service is not available ",e);
                return  null;
            }
        }
        return cacheManager;
    }



    public String getAddressModel(String remoteNodeName) {
        String ip = KevoreePlatformHelper.getProperty(this.getModelService().getLastModel(), remoteNodeName,
                org.kevoree.framework.Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP());
        if (ip == null || ip.equals("")) {
            ip = "127.0.0.1";
        }
        return ip;
    }


    @Override
    public void run() {

        try {

            String hostname = getAddressModel(getNodeName());
            int PeerListenerPort =   Integer.parseInt(this.getDictionary().get("PeerListenerPort").toString());

            String multicastGroupAddress =  this.getDictionary().get("multicastGroupAddress").toString();
            String multicastGroupPort =  this.getDictionary().get("multicastGroupPort").toString();
            String multicastTimeToLive =  this.getDictionary().get("multicastTimeToLive").toString();

            logger.debug("Starting RMICacheManagerPeerProviderFactory with multicastGroupAddress="+multicastGroupAddress+" multicastGroupPort="+multicastGroupPort+" multicastTimeToLive="+multicastTimeToLive);

            // remote peer
            FactoryConfiguration factoryConfig = new FactoryConfiguration();
            factoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory");
            // factoryConfig.setProperties("peerDiscovery=manual, rmiUrls=//127.0.0.1:40001/jed");
            factoryConfig.setProperties("peerDiscovery=automatic, multicastGroupAddress="+multicastGroupAddress+", multicastGroupPort="+multicastGroupPort+", timeToLive="+multicastTimeToLive);
            factoryConfig.setPropertySeparator(",");


            logger.debug("Starting RMICacheManagerPeerListenerFactory with hostname="+hostname+" PeerListenerPort="+PeerListenerPort);

            // local node
            FactoryConfiguration listenerFactoryConfig = new FactoryConfiguration();
            listenerFactoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory");
            listenerFactoryConfig.setProperties("hostName="+hostname+", port="+PeerListenerPort+", socketTimeoutMillis=120000");



            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setMaxEntriesLocalHeap(100);

            /*
            cacheConfiguration.setEternal(true);
            cacheConfiguration.overflowToDisk(true);
            cacheConfiguration.setTimeToLiveSeconds(300);
            cacheConfiguration.setTimeToIdleSeconds(3000);  */


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
            cache.setName("jed");
            cacheManager.addCache(cache);

            verrou.release();

        }catch (Exception e){
            logger.error("", e);
            cacheManager= null;
        }

    }
}
