package org.kevoree.library.ormHM.persistence;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:32
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PersistenceSessionFactory
{

  private static Logger log = LoggerFactory.getLogger(PersistenceSessionFactory.class);
  private PersistenceConfiguration persistenceConfiguration;
  protected static ThreadLocal<PersistenceSession> currentSession = new ThreadLocal<PersistenceSession>();

  public PersistenceSessionFactory(PersistenceConfiguration persistenceConfiguration)
  {
    this.persistenceConfiguration = persistenceConfiguration;
  }

  public PersistenceSession openSession() throws PersistenceException {
    return new PersistenceSession(this);
  }


  public PersistenceSession getSession() throws PersistenceException
  {
    log.debug("Getting current Persistence Session ");
    PersistenceSession session = currentSession.get();
    if (session == null)
    {
      session = openSession();
      currentSession.set(session);
    }
    return session;
  }

  public void close(PersistenceSession session)
  {
    PersistenceSession current = currentSession.get();
    if (current != null && current == session) {
      currentSession.set(null);
    }
    log.debug("Closing Persistence Session ");
  }

  public PersistenceConfiguration getPersistenceConfiguration() {
    return persistenceConfiguration;
  }

}
