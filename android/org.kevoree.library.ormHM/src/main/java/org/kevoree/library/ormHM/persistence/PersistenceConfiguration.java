package org.kevoree.library.ormHM.persistence;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:30
 */

import org.kevoree.library.ormHM.persistence.connection.StandardHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PersistenceConfiguration {

    private  Logger logger = LoggerFactory.getLogger(this.getClass());
    private StandardHandler connectionConfiguration=null;
    private Map<String, PersistentClass> persistentClasses=null;


    public PersistenceConfiguration() throws PersistenceException
    {
        persistentClasses =  new HashMap<String, PersistentClass>();
        for(PersistentClass pc : getPersistentClasses())
        {
            System.out.println(pc);
            pc.parse();
        }
    }


    public void addPersistentObject(Class pc) throws PersistenceException {
        PersistentClass tmp = new PersistentClass(pc.getName());
        persistentClasses.put(tmp.getName(), tmp);
        tmp.parse();
    }


    public void addPersistentClass(PersistentClass pc)
    {
        persistentClasses.put(pc.getName(), pc);
    }


    public StandardHandler getConnectionConfiguration()
    {
        return connectionConfiguration;
    }

    public void setConnectionConfiguration(StandardHandler connectionConfiguration)
    {
        this.connectionConfiguration = connectionConfiguration;
    }


    public PersistenceSessionFactory getPersistenceSessionFactory(){
        return new PersistenceSessionFactory(this);
    }



    public PersistentClass getPersistentClass(Class clazz)
    {
        return getPersistentClass(clazz.getName());
    }

    public PersistentClass getPersistentClass(Object clazz)
    {
        return getPersistentClass(clazz.getClass());
    }

    public PersistentClass getPersistentClass(String name) {
        return persistentClasses.get(name);
    }

    public Collection<PersistentClass> getPersistentClasses()
    {
        return persistentClasses.values();
    }

}