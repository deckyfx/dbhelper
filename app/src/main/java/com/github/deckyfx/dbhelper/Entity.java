package com.github.deckyfx.dbhelper;

import com.github.deckyfx.greendao.AbstractDao;
import com.github.deckyfx.greendao.Property;
import com.github.deckyfx.greendao.query.QueryBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by decky on 9/25/17.
 */

public class Entity<T, K> {
    public AbstractDao<T, K> entity;
    public String hashMapKey;
    public String className;
    public String fullClassName;
    public Class<?> properties;
    public HashMap<String, Field> fields;
    public HashMap<String, Method> methods;

    public Entity(AbstractDao<T, K> entity, String fullClassName){
        super();
        this.entity = entity;
        this.fullClassName = fullClassName;
        String[] bits = fullClassName.split("\\.");
        this.className = bits[bits.length-1];
        this.hashMapKey = this.className.substring(0, this.className.length() - 3);
        // Remove "Dao" from part className as key
        Class<?>[] innerClass = this.entity.getClass().getDeclaredClasses();
        for (int i = 0; i < innerClass.length; i++) {
            if (innerClass[i].getSimpleName().equals("Properties")) {
                this.properties = innerClass[i];
            }
        }
        this.fields = new HashMap<String, Field>();
        this.methods = new HashMap<String, Method>();
    }

    public Object invoke(String methodName, Object... arguments) {
        Object result = null;
        Method method = this.getMethod(methodName);
        if (method != null) {
            try {
                method.setAccessible(true);
                result = method.invoke(this.entity, arguments);
                method.setAccessible(false);
            } catch (SecurityException e) {
                e.printStackTrace();
                throw new DBHelper.InvokeError(e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new DBHelper.InvokeError(e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new DBHelper.InvokeError(e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new DBHelper.InvokeError(e);
            }
        }
        return result;
    }

    public Property getProperty(String fieldName) {
        Property property = null;
        Field field = this.getField(fieldName);
        if (field != null) {
            try {
                property = (Property) field.get(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return property;
    }

    private Method getMethod(String methodName){
        Method method = this.methods.get(methodName);
        if (method == null) {
            ArrayList<Method> method_lists = new ArrayList(Arrays.asList(this.entity.getClass().getDeclaredMethods()));
            method_lists.addAll(Arrays.asList(this.entity.getClass().getSuperclass().getDeclaredMethods()));
            for (Method m : method_lists) {
                if (m.getName().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if (method != null) {
                this.methods.put(methodName, method);
            }
        }
        return method;
    }

    private Field getField(String fieldName){
        Field field = this.fields.get(fieldName);
        if (field == null) {
            if (fieldName.length() >= 1) {
                fieldName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
            }
            try {
                field = this.properties.getDeclaredField(fieldName);
            } catch(NoSuchFieldException e){
                e.printStackTrace();
                try {
                    field = this.properties.getSuperclass().getDeclaredField(fieldName);
                } catch(NoSuchFieldException e2){
                    e2.printStackTrace();
                    if (this.properties != null) {
                        try {
                            field = this.properties.getSuperclass().getDeclaredField(fieldName);
                        } catch(NoSuchFieldException e3){
                            e3.printStackTrace();
                        }
                    }
                }
            }
            if (field != null) {
                this.fields.put(fieldName, field);
            }
        }
        return field;
    }

    public QueryBuilder<T> queryBuilder(){
        QueryBuilder<T> b = (QueryBuilder<T>) this.invoke(DBHelper.INVOKE.QUERY_BUILDER);
        return b;
    }

    public List<T> loadAll() {
        return (List<T>) this.invoke(DBHelper.INVOKE.LOAD_ALL);
    }

    public T load(long id) {
        return (T) this.invoke(DBHelper.INVOKE.LOAD, id);
    }

    public void insert(T data) {
        this.invoke(DBHelper.INVOKE.INSERT, data);
    }

    public void insert(T[] datas) {
        for (T data : datas) {
            this.invoke(DBHelper.INVOKE.INSERT, data);
        }
    }

    public void update(T data) {
        this.invoke(DBHelper.INVOKE.UPDATE, data);
    }

    public void update(T[] datas) {
        for (T data : datas) {
            this.invoke(DBHelper.INVOKE.UPDATE, data);
        }
    }

    public void insertOrReplace(T data) {
        this.invoke(DBHelper.INVOKE.INSERT_OR_REPLACE, data);
    }

    public void insertOrReplace(T[] datas) {
        for (T data : datas) {
            this.invoke(DBHelper.INVOKE.INSERT_OR_REPLACE, data);
        }
    }

    public void deleteAll() {
        this.invoke(DBHelper.INVOKE.DELETE_ALL);
    }
}
