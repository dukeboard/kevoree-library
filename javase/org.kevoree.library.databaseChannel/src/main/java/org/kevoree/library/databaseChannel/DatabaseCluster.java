package org.kevoree.library.databaseChannel;

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactoryEnum;
import net.sf.hajdbc.dialect.DialectFactoryEnum;
import net.sf.hajdbc.distributed.jgroups.DefaultChannelProvider;
import net.sf.hajdbc.sql.DataSource;
import net.sf.hajdbc.sql.DataSourceDatabase;
import net.sf.hajdbc.sql.DataSourceDatabaseClusterConfiguration;
import net.sf.hajdbc.sql.SQLProxy;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import net.sf.hajdbc.sync.FullSynchronizationStrategy;
import org.kevoree.annotation.*;
import org.kevoree.common.dbProxy.api.dbProxyService;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 09/05/12
 * Time: 14:27
 */

@Library(name = "JavaSE")
@ComponentType
@DictionaryType({
        @DictionaryAttribute(name = "login", defaultValue = "daum", optional = false),
        @DictionaryAttribute(name = "password", defaultValue = "daum", optional = false)
})
@Provides({
        @ProvidedPort(name = "configuration", type = PortType.MESSAGE)   ,
        @ProvidedPort(name = "dbProxyService", type = PortType.SERVICE, className = dbProxyService.class)
})
public class DatabaseCluster extends AbstractComponentType implements  dbProxyService{
    private Logger logger = LoggerFactory.getLogger(DatabaseCluster.class);
    private  javax.sql.DataSource ds=null;
    private  HashMap<String,DataSourceDatabase>  dataSourceDatabases=null;
    private     SQLProxy<javax.sql.DataSource, DataSourceDatabase, javax.sql.DataSource, SQLException> proxy = null;

    DataSource ds_cluster;
    @Start
    public void start() {

    }

    @Stop
    public void stop()
    {

    }

    @Port(name = "configuration")
    public void confCluster(Object obj) {

        if (obj instanceof HashMap)
        {
            try
            {
                String login =   this.getDictionary().get("login").toString();
                String password = this.getDictionary().get("password").toString();

                dataSourceDatabases = (HashMap<String, DataSourceDatabase>) obj;
                DataSourceDatabaseClusterConfiguration config = new DataSourceDatabaseClusterConfiguration();

                config.setDatabases(dataSourceDatabases.values());

                config.setDialectFactory(DialectFactoryEnum.DERBY);
                config.setDatabaseMetaDataCacheFactory(DatabaseMetaDataCacheFactoryEnum.SHARED_EAGER);

                DefaultChannelProvider channel = new DefaultChannelProvider();


                config.setDispatcherFactory(channel);
                HashMap<String,SynchronizationStrategy> t = new HashMap<String, SynchronizationStrategy>();


                SynchronizationStrategy t2 = new FullSynchronizationStrategy();
                t.put("full",t2);

                config.setSynchronizationStrategyMap(t);
                config.setDefaultSynchronizationStrategy("full");
                SimpleStateManagerFactory state = new SimpleStateManagerFactory();

                config.setStateManagerFactory(state);
                ds_cluster = new DataSource();
                ds_cluster.setCluster("kevoreeCluster");
                ds_cluster.setConfigurationFactory(new SimpleDatabaseClusterConfigurationFactory<javax.sql.DataSource, DataSourceDatabase>(config));

            } catch (Exception e)
            {
                logger.error("",e);
                ds = null;
            }
        }

    }

    @Update
    public void update() {

    }

    @Override
    @Port(name = "dbProxyService", method = "getConnection")
    public Connection getConnection() {
        Connection connection =null;

        logger.debug("getConnection Service");
        try
        {
            String login =   this.getDictionary().get("login").toString();
            String password = this.getDictionary().get("password").toString();

            proxy = (SQLProxy<javax.sql.DataSource, DataSourceDatabase, javax.sql.DataSource, SQLException>) Proxy.getInvocationHandler(ds_cluster.getProxy());
            ds =  proxy.getObject(dataSourceDatabases.get(getNodeName()));

            if(ds != null)
            {
                connection = ds.getConnection(login,password);
            }
            logger.debug("getConnection isValid = "+connection.isValid(100));
        }  catch (Exception e)
        {
            logger.error("",e);
            connection = null;
        }
        return   connection;
    }
}
