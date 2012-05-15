package org.kevoree.library.ormHM.persistence.connection;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.kevoree.library.ormHM.persistence.OrhmID;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 14:03
 */
public class EhcacheHandler implements StandardHandler {

    private CacheConfiguration cacheConfiguration=null;
    // todo use global cache configuration

    private CacheManager cacheManager = null;
    public EhcacheHandler(CacheManager cacheManager){
         this.cacheManager = cacheManager;
    }
    public CacheManager getCacheManager()
    {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cache)
    {
        cacheManager = cache;
    }


    @Override
    public void save(OrhmID orhmH, Object bean)
    {
        Cache cache = cacheManager.getCache(orhmH.getAttachTo());

        if(cache == null)
        {
            cacheManager.addCache(orhmH.getAttachTo());
            cache = cacheManager.getCache(orhmH.getAttachTo());
        }

        cache.acquireWriteLockOnKey(orhmH.getId());

        Element aCacheElement = new Element(orhmH.getId(),bean);
        cache.put(aCacheElement);
        cache.releaseWriteLockOnKey(orhmH.getId());
    }

    @Override
    public Object get(OrhmID orhmH)
    {
        Object bean=null;
        Cache cache = cacheManager.getCache(orhmH.getAttachTo());

        cache.acquireReadLockOnKey(orhmH.getId());
         bean = cache.get(orhmH.getId()).getValue();
        cache.releaseReadLockOnKey(orhmH.getId());

        return bean;
    }
}
