package org.kevoree.library.ehcache;

import net.sf.ehcache.CacheManager;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 11/05/12
 * Time: 17:09
 */

@Library(name = "JavaSE")
@ComponentType
/*
@DictionaryType({
        @DictionaryAttribute(name = "login", defaultValue = "daum", optional = false),
        @DictionaryAttribute(name = "password", defaultValue = "daum", optional = false)
}) */
@Provides({
        @ProvidedPort(name = "ehcacheChannel", type = PortType.MESSAGE),
        @ProvidedPort(name = "ehCacheService", type = PortType.SERVICE, className = IehcacheService.class)
})
public class ehcacheManager extends AbstractComponentType implements  IehcacheService{
    private Logger logger = LoggerFactory.getLogger(ehcacheManager.class);

    private  CacheManager cacheManager=null;
    @Start
    public void start() {

    }

    @Stop
    public void stop()
    {

    }

    @Port(name = "ehcacheChannel")
    public void confCluster(Object obj) {

        if (obj instanceof CacheManager)
        {
            try
            {
                cacheManager = (CacheManager)obj;
                logger.debug("receivce CacheManager"+cacheManager);


            } catch (Exception e)
            {
                logger.error("",e);

            }
        } else {

            logger.error("is not CacheManager instance");
        }

    }

    @Update
    public void update() {

    }


    @Override
    @Port(name = "ehCacheService", method = "getCacheManger")
    public CacheManager getCacheManger() {
        // todo
        return cacheManager;
    }
}
