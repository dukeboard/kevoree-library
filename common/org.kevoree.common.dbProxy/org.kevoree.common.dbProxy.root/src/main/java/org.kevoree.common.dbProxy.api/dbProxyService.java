package org.kevoree.common.dbProxy.api;

import java.sql.Connection;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 09/05/12
 * Time: 09:39
 */
public interface dbProxyService
{
    public Connection getConnection();
}
