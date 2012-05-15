import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 15:03
 */
public class EhcacheTest {


    CacheManager cacheManager = null;
    public EhcacheTest() {


        // Manuel peer
        FactoryConfiguration factoryConfig = new FactoryConfiguration();
        factoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory");
        //     factoryConfig.setProperties("peerDiscovery=manual, rmiUrls=//127.0.0.1:40000/jed");
        factoryConfig.setProperties("peerDiscovery=automatic, multicastGroupAddress=230.0.0.1, multicastGroupPort=4446, timeToLive=32");
        factoryConfig.setPropertySeparator(",");


        // local
        FactoryConfiguration listenerFactoryConfig = new FactoryConfiguration();
        listenerFactoryConfig.setClass("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory");
        listenerFactoryConfig.setProperties("hostName=127.0.0.1, port=40015, socketTimeoutMillis=120000");


        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setMaxEntriesLocalHeap(100);


        CacheConfiguration.CacheEventListenerFactoryConfiguration  cachEvent =  new CacheConfiguration.CacheEventListenerFactoryConfiguration();
        cachEvent.setClass("net.sf.ehcache.distribution.RMICacheReplicatorFactory");
        cachEvent.setProperties("replicateAsynchronously=true, replicatePuts=true,  replicatePutsViaCopy=false, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true, asynchronousReplicationIntervalMillis=100");
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

    }

    public void shudown(){
        try {
            cacheManager.shutdown();
        }   catch (Exception e){
            //ignore
        }

    }

    public CacheManager getCacheManager(){

        return cacheManager;
    }
}
