package org.kevoree.library.ormHM.persistence;

import org.kevoree.library.ormHM.persistence.connection.StandardHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:33
 */

public class PersistenceSession implements  IPersistenceSession{

    private static Logger logger =  LoggerFactory.getLogger(PersistenceSession.class);
    private PersistenceSessionFactory factory;
    private PersistenceConfiguration conf;
    private StandardHandler standardHandler=null;

    public PersistenceSession(PersistenceSessionFactory persistenceSessionFactory) throws PersistenceException
    {
        this.factory = persistenceSessionFactory;
        this.conf = persistenceSessionFactory.getPersistenceConfiguration();
        standardHandler = conf.getConnectionConfiguration();
        if(standardHandler == null)
        {
            throw new PersistenceException("standardHandler is null");
        }
    }

    public void save(Object bean) throws PersistenceException
    {
        PersistentClass pc = null;
        pc=factory.getPersistenceConfiguration().getPersistentClass(bean);

        for (PersistentProperty pp : pc.getPersistentProperties())
        {
            if(pp.isId())
            {
                Object idclass =  pp.getValue(bean);
                if(idclass != null)
                {
                    OrhmID id = new OrhmID(pp.getAttachTO(),idclass);
                    standardHandler.save(id, bean);
                }
                else
                {
                    logger.warn("the id is null");
                }
            }
        }
    }

    public Object get(Class clazz,Object _id) throws PersistenceException
    {
        Object bean = null;
        PersistentClass pc= null;

        try
        {
            pc = factory.getPersistenceConfiguration().getPersistentClass(clazz);
            OrhmID id = new OrhmID(pc.getId().getAttachTO(),_id);
            bean = standardHandler.get(id);
            return bean;

        } catch (Exception e)
        {
            logger.error("",e);
        }

        return  null;
    }

    public Object getAll(Class clazz) throws PersistenceException
    {
        // TODO
        return  null;
    }



    public void close()
    {
        factory.close(this);
    }
}