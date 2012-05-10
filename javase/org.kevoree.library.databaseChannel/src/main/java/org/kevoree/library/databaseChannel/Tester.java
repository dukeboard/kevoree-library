package org.kevoree.library.databaseChannel;

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactoryEnum;
import net.sf.hajdbc.dialect.DialectFactoryEnum;
import net.sf.hajdbc.sql.DataSource;
import net.sf.hajdbc.sql.DataSourceDatabase;
import net.sf.hajdbc.sql.DataSourceDatabaseClusterConfiguration;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import net.sf.hajdbc.sync.FullSynchronizationStrategy;
import org.daum.hajdbc.UrlDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 09/05/12
 * Time: 16:30
 */
public class Tester {


    public static void  main(String test[]) {



        DataSourceDatabase db1 = new DataSourceDatabase();
        db1.setId("node0");
        db1.setName(UrlDataSource.class.getName());
        db1.setProperty("url", "jdbc:derby://127.0.0.1:1527/node0");

        db1.setUser("daum");
        db1.setPassword("daum");

        DataSourceDatabase db2 = new DataSourceDatabase();
        db2.setId("node1");
        db2.setName(UrlDataSource.class.getName());
        db2.setProperty("url", "jdbc:derby://127.0.0.1:1528/node1");
        db2.setUser("daum");
        db2.setPassword("daum");

        List<DataSourceDatabase> dataSourceDatabases = new ArrayList<DataSourceDatabase>();


        dataSourceDatabases.add(db1);
        dataSourceDatabases.add(db2);

        DataSourceDatabaseClusterConfiguration config = new DataSourceDatabaseClusterConfiguration();
        config.setDatabases(dataSourceDatabases);

        config.setDialectFactory(DialectFactoryEnum.DERBY);
        config.setDatabaseMetaDataCacheFactory(DatabaseMetaDataCacheFactoryEnum.SHARED_EAGER);

     /*
        DefaultChannelProvider channel = new DefaultChannelProvider();


        config.setDispatcherFactory(channel);

                */
        HashMap<String,SynchronizationStrategy> t = new HashMap<String, SynchronizationStrategy>();


        SynchronizationStrategy t2 = new FullSynchronizationStrategy();
        t.put("jed",t2);


        config.setSynchronizationStrategyMap(t);
        config.setDefaultSynchronizationStrategy("jed");


        SimpleStateManagerFactory state = new SimpleStateManagerFactory();


        config.setStateManagerFactory(state);

        final DataSource ds = new DataSource();

        ds.setCluster("kevoreeCluster");
        ds.setConfigurationFactory(new SimpleDatabaseClusterConfigurationFactory<javax.sql.DataSource, DataSourceDatabase>(config));


        /*
        try {
             String createSQL1 = "DROP TABLE test";

             Connection c1 = ds.getConnection("daum", "daum");
             Statement s1 = c1.createStatement();
             s1.execute(createSQL1);
             s1.close();
         } catch (Exception e)   {

         }
         */
        /*
        String createSQL = "CREATE TABLE test (id INTEGER NOT NULL, name VARCHAR(10) NOT NULL, PRIMARY KEY (id))";
        try {

            Connection c = ds.getConnection("daum","daum");
            Statement s1 = c.createStatement();
            s1.execute(createSQL);
            s1.close();
        } catch (Exception e) {
            System.out.println("node 0");
        }
        */






        Connection db=null;
        PreparedStatement ps=null;

        try {
            Statement stmt = null;
                             /*
       db = ds.getConnection("daum", "daum");

       stmt = db.createStatement();

       stmt.execute("DELETE FROM test");
       stmt.close();     */

            Random rand = new Random();
            int i =0;


            while(i <50){

                db = ds.getConnection("daum", "daum");


                db.setAutoCommit(false);
                ps = db.prepareStatement("INSERT INTO test (id, name) VALUES (?, ?)");
                ps.setInt(1,rand.nextInt(9000000));
                ps.setString(2, "1");
                ps.addBatch();
                ps.setInt(1, rand.nextInt(9000000));
                ps.setString(2, "2");
                ps.addBatch();
                ps.executeBatch();
                ps.close();
                db.commit();

                i++;
            }


            String selectSQL = "SELECT id, name FROM test";
            try {

                db = ds.getConnection("daum", "daum");
                stmt = db.createStatement();
                i=0;
                ResultSet rs1 = stmt.executeQuery(selectSQL);
                while(rs1.next())
                {
                    //   System.out.println("node 0 ->"+rs1.getInt(1)+" "+rs1.getString(2));
                    i++;
                }

                System.out.println("coutn = "+i);

            } catch (Exception e) {
                System.out.print(e.getCause());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        finally {
            try {
                if(db != null)
                    db.close();
            } catch (SQLException e) {

            }
            try {
                if(ps !=null)
                    ps.close();
            } catch (SQLException e) {

            }

        }


    }
}
