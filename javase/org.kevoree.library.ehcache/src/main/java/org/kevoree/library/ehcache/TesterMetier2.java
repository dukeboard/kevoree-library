package org.kevoree.library.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
public class TesterMetier2 extends AbstractComponentType implements  Runnable{
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
    public void update()
    {
        try
        {
            current.start();
        } catch (Exception e) {
            // ignore
        }
    }


    @Override
    public void run()
    {
        while (alive)
        {
            try {
                CacheManager c =  this.getPortByName("ehCacheService", IehcacheService.class).getCacheManger();

                Cache myCache =c.getCache("jed");
                myCache.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        System.out.print(evt.getPropertyName()+" old="+evt.getOldValue()+" new="+evt.getNewValue());
                    }
                });


            }  catch (Exception e)
            {
                logger.error("error");
            }


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
