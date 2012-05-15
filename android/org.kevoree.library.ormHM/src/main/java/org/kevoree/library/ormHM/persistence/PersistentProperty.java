package org.kevoree.library.ormHM.persistence;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.kevoree.library.ormHM.annotations.Id;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 19:00
 */
public class PersistentProperty {

    private PersistentClass pers;

    private String name;
    private Class clazz;
    private String columnName;
    private String javaType;
    private boolean id;
    private Method method;
    private String attachTO = "";

    public PersistentProperty(PersistentClass persistentClass, Method method) {
        this.pers = persistentClass;
        this.method = method;
        parse();
    }

    public void parse()
    {
        Id id = null;
        String methodName = method.getName();
        String propertyName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        setName(propertyName);
        Class rclazz = method.getReturnType();
        String javaType = rclazz.getSimpleName();
        setClazz(rclazz);

        setType(javaType);

        id = method.getAnnotation(Id.class);
        if (id != null)
        {
            setId(true);
            pers.setId(this);
            setType(javaType);
            setAttachTO(id.attachTOCache());
        }

    }

    public String getAttachTO() {
        return attachTO;
    }

    public void setAttachTO(String attachTO) {
        this.attachTO = attachTO;
    }

    public PersistentClass getPers() {
        return pers;
    }
    public void setPers(PersistentClass pers) {
        this.pers = pers;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Class getClazz() {
        return clazz;
    }
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }
    public String getColumnName() {
        return columnName;
    }
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public String getType() {
        return javaType;
    }
    public void setType(String sqlType) {
        this.javaType = sqlType;
    }
    public boolean isId() {
        return id;
    }
    public void setId(boolean id) {
        this.id = id;
    }

    public Object getValue(Object bean) throws PersistenceException {
        try {
            return PropertyUtils.getSimpleProperty(bean, getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setValue(Object bean, Object value) throws PersistenceException {
        Object valueConverted = ConvertUtils.convert(value, getClazz());
        try {
            PropertyUtils.setSimpleProperty(bean, getName(), valueConverted);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}