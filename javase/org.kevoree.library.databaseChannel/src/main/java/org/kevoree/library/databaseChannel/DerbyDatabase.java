package org.kevoree.library.databaseChannel;

import org.apache.derby.drda.NetworkServerControl;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.net.InetAddress;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 07/05/12
 * Time: 15:34
 */

@Library(name = "JavaSE")
@ComponentType
@Provides({
        //@ProvidedPort(name = "dbProxyService", type = PortType.SERVICE, className = dbProxyService.class)
})
@DictionaryType({
        @DictionaryAttribute(name = "derbyport", optional = false)
})
public class DerbyDatabase extends AbstractComponentType implements  Runnable {
    private Logger logger = LoggerFactory.getLogger(DerbyDatabase.class);
    private NetworkServerControl serverControl= null;
    private Thread current = new Thread(this);

    @Start
    public void start() {
        current.start();
    }

    @Stop
    public void stop()
    {
        try {
            serverControl.shutdown();
        } catch (Exception e) {
            // ignore
        }
    }



    @Update
    public void update() {
        try {
            serverControl.shutdown();
            current.start();
        } catch (Exception e) {
            // ignore
        }
    }


    @Override
    public void run() {
        try
        {
            int current_node_derbyport = Integer.parseInt(getDictionary().get("derbyport").toString());
            logger.debug("Starting Derby "+current_node_derbyport);

            serverControl = new NetworkServerControl(InetAddress.getByName("localhost"),current_node_derbyport);
            serverControl.start(new PrintWriter(System.out, true));
        } catch (Exception e)
        {
            logger.error("The starting of Derby fail "+e);
        }

    }

    /*
    @Override
    @Port(name = "dbProxyService", method = "getListDataSourceDatabase")
    public List<DataSourceDatabase> getListDataSourceDatabase() {

        return null;
    }

    @Override
    @Port(name = "dbProxyService", method = "getDataSource")
    public DataSource getDataSource() {
        return null;
    }
    */
}
