package org.kevoree.library.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 11/05/12
 * Time: 17:19
 */
@Library(name = "JavaSE")
@ComponentType
@Requires({
        @RequiredPort(name = "ehCacheService", type = PortType.SERVICE, className = IehcacheService.class, optional = true)
})
public class TesterMetier extends AbstractComponentType implements  Runnable{
    private Logger logger = LoggerFactory.getLogger(TesterMetier.class);
    private  boolean  alive=true;
    private Thread current = new Thread(this);

    @Start
    public void start() {
        current.start();
        alive =true;
    }

    @Stop
    public void stop() {
        alive =false;
    }

    @Update
    public void update() {
        try {

            current.start();
        } catch (Exception e) {
            // ignore
        }
    }


    @Override
    public void run() {

        int i=0;
        while (alive)
        {

            try {


                CacheManager c =  this.getPortByName("ehCacheService", IehcacheService.class).getCacheManger();

                Cache myCache =   c.getCache("jed");

                myCache.acquireReadLockOnKey(0);
                System.out.println(myCache.get(0));
                myCache.releaseReadLockOnKey(0);



                System.out.println("put on key 0"+i);
                myCache.acquireWriteLockOnKey(0);
                Element aCacheElement = new Element(0, getNodeName()+" "+i);
                myCache.put(aCacheElement);
                myCache.releaseWriteLockOnKey(0);

                i++;
            }  catch (Exception e){
                logger.error("error ",e);
            }


            try {
                Thread.sleep(9500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
