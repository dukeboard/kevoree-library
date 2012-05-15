package org.kevoree.library.ormHM.persistence;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:30
 */
public class PersistentClass
{
    private String name;
    private	Class clazz;
    private PersistentProperty property;
    private String tablename;
    private List<PersistentProperty> persistantProperties =  new ArrayList<PersistentProperty>();
    private PersistentProperty id;

    public PersistentClass(){

    }

    public PersistentClass(String name){
        this.name = name;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public PersistentProperty getProperty() {
        return property;
    }

    public void setProperty(PersistentProperty property) {
        this.property = property;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {

        this.tablename = tablename;
    }

    public List<PersistentProperty> getPersistantProperties() {
        return persistantProperties;
    }

    public void setPersistantProperties(
            List<PersistentProperty> persistantProperties) {
        this.persistantProperties = persistantProperties;
    }

    public PersistentProperty getId() {
        return id;
    }

    public void setId(PersistentProperty id) {
        this.id = id;
    }

    public void parse() throws PersistenceException {
        try
        {
            clazz = Class.forName(getName());
        }
        catch(ClassNotFoundException e)
        {
            throw new PersistenceException("Mapped class not found : "+getName());
        }

        setTablename(clazz.getSimpleName().toLowerCase());
        Method[] methods = clazz.getMethods();
        for(int i=0;i < methods.length;i++)
        {
            Method method = methods[i];
            String methodName = method.getName();
            Class[] types = method.getParameterTypes();
            if(methodName.startsWith("get") && types.length == 0 && method.getDeclaringClass().equals(clazz))
            {
                property = new PersistentProperty(this, method);
                String propertyName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
                if(property.getType() != null)
                {
                    persistantProperties.add(property);
                }
            }
        }
    }

    public Object createInstance() throws PersistenceException, InstantiationException, IllegalAccessException
    {
        Object created = null;
        created = clazz.newInstance();
        return created;
    }

    public void setPersistentProperties(
            List<PersistentProperty> persistentProperties) {
        this.persistantProperties = persistentProperties;
    }

    public List<PersistentProperty> getPersistentProperties() {
        return persistantProperties;
    }

    public PersistentProperty getPersistentProperty(String string)
    {
        for(PersistentProperty p : getPersistentProperties())
        {
            if(p.getName().equals(string))
                return p;
        }
        return null;
    }


}