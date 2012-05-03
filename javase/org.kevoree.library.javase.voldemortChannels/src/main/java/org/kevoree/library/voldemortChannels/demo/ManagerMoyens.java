package org.kevoree.library.voldemortChannels.demo;

import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.MessagePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 26/04/12
 * Time: 15:51
 */
@Library(name = "JavaSE", names = {"Android"})
@Requires({
        @RequiredPort(name = "moyens", type = PortType.MESSAGE,optional = true)
})
@ComponentType
public class ManagerMoyens extends AbstractComponentType implements  Runnable{
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean  alive = true;
    private int period = 2000;

    @Start
    public void start()
    {
        new Thread(this). start ();
    }


    @Stop
    public void stop() {
        alive  = false;
    }

    @Update
    public void update() {

    }


    @Override
    public void run() {
        int count = 0;

        while(alive)
        {
            String rowData[][] = { { "BLS", "RENNES"+new Random().nextInt(10), "LIBRE" },
                        { "FPT", "RENNES", "LIBRE" } };

            getPortByName("moyens", MessagePort.class).process(rowData);
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }
}