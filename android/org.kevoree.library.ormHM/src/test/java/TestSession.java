import model.sitac.Intervention;
import model.sitac.Moyen;
import model.sitac.MoyenType;
import org.junit.Test;
import org.kevoree.library.ormHM.persistence.PersistenceConfiguration;
import org.kevoree.library.ormHM.persistence.PersistenceSession;
import org.kevoree.library.ormHM.persistence.PersistenceSessionFactory;
import org.kevoree.library.ormHM.persistence.connection.EhcacheHandler;

import static org.junit.Assert.assertEquals;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 10:45
 */
public class TestSession {

    @Test
    public void testSave(){
        PersistenceConfiguration configuration=null;
        PersistenceSessionFactory factory=null;

        try
        {
            configuration = new PersistenceConfiguration();
            configuration.addPersistentObject(Moyen.class);
            configuration.addPersistentObject(Intervention.class);
            configuration.addPersistentObject(Intervention.class);


            EhcacheTest ehcacheTeste = new EhcacheTest();

            EhcacheHandler ehcacheHandler = new EhcacheHandler(ehcacheTeste.getCacheManager());


            configuration.setConnectionConfiguration(ehcacheHandler);


            factory = configuration.getPersistenceSessionFactory();
            PersistenceSession s = factory.openSession();

            Moyen m1 = new Moyen(new MoyenType(1), "FPT", 1);
            Moyen m2 = new Moyen(new MoyenType(2), "VSAV", 2);
            Moyen m3 = new Moyen(new MoyenType(1), "FPT", 3);
            Moyen m4 = new Moyen(new MoyenType(1), "FPT", 4);
            Moyen m5 = new Moyen(new MoyenType(2), "VSAV", 5);

            s.save(m1);
            s.save(m2);
            s.save(m3);
            s.save(m4);
            s.save(m5);

            s.close();



            s = factory.openSession();

            Moyen _m1 = (Moyen)  s.get(Moyen.class, m1.getNumber());
            assertEquals(m1.getNumber(),_m1.getNumber());
            assertEquals(m1.getType(),_m1.getType());
            assertEquals(m1.getName(),_m1.getName());

            Moyen _m2 = (Moyen)  s.get(Moyen.class, m2.getNumber());

            assertEquals(m2.getNumber(),_m2.getNumber());
            assertEquals(m2.getType(),_m2.getType());
            assertEquals(m2.getName(),_m2.getName());


            s.close();

            ehcacheTeste.shudown();

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
