package org.kevoree.library.databaseChannel;

import org.kevoree.annotation.*;
import org.kevoree.common.dbProxy.api.dbProxyService;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Random;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 09/05/12
 * Time: 11:43
 */

@Library(name = "JavaSE")
@ComponentType



@Requires({
        @RequiredPort(name = "dbProxyService", type = PortType.SERVICE, className = dbProxyService.class, optional = true)
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

        Random rand =    new Random();
        while (alive)
        {

            Connection c =  this.getPortByName("dbProxyService", dbProxyService.class).getConnection();
            try
            {
                if( c != null)
                {
                      int a = rand.nextInt(200);
                    int b = rand.nextInt(200);
                    c.setAutoCommit(false);
                    PreparedStatement ps = c.prepareStatement("INSERT INTO test (id, name) VALUES (?, ?)");
                    ps.setInt(1,a);
                    ps.setString(2, "1");
                    ps.addBatch();
                    ps.setInt(1, b);
                    ps.setString(2, "2");
                    ps.addBatch();
                    ps.executeBatch();
                    ps.close();
                    c.commit();
                    logger.debug(" commit "+a+" "+b);

                } else {
                    logger.debug("Connection is null");
                }

                c.commit();
                 c.close();
                


            } catch (Exception e) {
                logger.error("",e);
            }


            try {
                Thread.sleep(9500);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

}
