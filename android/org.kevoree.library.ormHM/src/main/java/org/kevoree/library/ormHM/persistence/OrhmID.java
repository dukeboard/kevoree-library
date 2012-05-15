package org.kevoree.library.ormHM.persistence;

import java.io.Serializable;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 17:58
 */
public class OrhmID implements Serializable{

    private  String attachTo ="";
    private Object id;


    public OrhmID(String attachTO, Object id) {
        this.attachTo = attachTO;
        this.id = id;

    }
    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getAttachTo() {
        return attachTo;
    }

    public void setAttachTo(String attachTo) {
        this.attachTo = attachTo;
    }
}
