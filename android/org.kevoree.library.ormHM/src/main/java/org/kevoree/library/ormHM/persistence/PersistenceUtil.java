package org.kevoree.library.ormHM.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:32
 */
public class PersistenceUtil {
  private static PersistenceConfiguration configuration;
  private static PersistenceSessionFactory factory;
  private static Logger log = LoggerFactory.getLogger(PersistenceUtil.class);


  static {
    try {
      configuration = new PersistenceConfiguration();
      factory = configuration.getPersistenceSessionFactory();
    } catch (Throwable ex) {
      log.error("Building PersistenceSessionFactory failed.", ex);
      throw new ExceptionInInitializerError(ex);
    }
  }


  public static PersistenceConfiguration getConfiguration() {
    return configuration;
  }


  public static PersistenceSessionFactory getFactory() {
    return factory;
  }

}
