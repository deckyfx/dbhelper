package com.github.deckyfx.dbhelper;

/**
 * Created by decky on 12/28/16.
 */


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoMaster;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by decky on 2/20/15.
 * after generate Dao, call this in your App
 * App.DB = this.db = new DbHelper(App.MAIN_CONTEXT, DaoMaster.class);
 *
 * */
public class DBHelper {
    private SQLiteDatabase Sqlitedb;

    private AbstractDaoMaster DAOMaster;
    private AbstractDaoSession DAOSession;
    private SQLiteOpenHelper OpenHelper;

    private HashMap<String, EntityMapWrapper> mDAOs;

    private Context mContext;

    public static class INVOKE {
        public static final String QUERY_BUILDER        = "queryBuilder";
        public static final String LOAD                 = "load";
        public static final String LOAD_ALL             = "loadAll";
        public static final String INSERT_OR_REPLACE    = "insertOrReplace";
        public static final String INSERT               = "insert";
        public static final String DELETE_ALL           = "deleteAll";
    }

    public DBHelper(Context context, Class<? extends AbstractDaoMaster> daoMasterClass, String dbName){
        this.mContext = context;
        this.mDAOs = new HashMap<String, EntityMapWrapper>();

        if (daoMasterClass != null) {
            Method method = null;
            try {
                Class<?> devOpenHelperClass = Class.forName(daoMasterClass.getName() + "$" + "DevOpenHelper");
                Constructor<?> devOpenHelperCtor = devOpenHelperClass.getDeclaredConstructor(Context.class, String.class, SQLiteDatabase.CursorFactory.class);
                Constructor<?> daoMasterCtor = daoMasterClass.getConstructor(SQLiteDatabase.class);
                this.OpenHelper = (SQLiteOpenHelper) devOpenHelperCtor.newInstance(this.mContext, dbName, null);
                this.Sqlitedb = this.OpenHelper.getWritableDatabase();
                this.DAOMaster = (AbstractDaoMaster) daoMasterCtor.newInstance(this.Sqlitedb);
                this.DAOSession = this.DAOMaster.newSession();
                this.populateDaoEntity(this.DAOSession);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public class EntityMapWrapper {
        public AbstractDao<?, Long> entity;
        public String hashMapKey;
        public String className;
        public String fullClassName;
        public Class<?> properties;
        public HashMap<String, Field> fields;
        public HashMap<String, Method> methods;

        public EntityMapWrapper(AbstractDao<?, Long> entity, String fullClassName){
            this.entity = entity;
            this.fullClassName = fullClassName;
            String[] bits = fullClassName.split("\\.");
            this.className = bits[bits.length-1];
            this.hashMapKey = this.className.substring(0, this.className.length() - 3); // Remove "Dao" from part className as key
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
                    throw new InvokeError(e);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new InvokeError(e);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new InvokeError(e);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new InvokeError(e);
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

        public QueryBuilder<?> queryBuilder(){
            QueryBuilder<?> b = (QueryBuilder<?>) this.invoke(INVOKE.QUERY_BUILDER);
            return b;
        }

        public List<?> loadAll() {
            return (List) this.invoke(INVOKE.LOAD_ALL);
        }

        public Object load(long id) {
            return this.invoke(INVOKE.LOAD, id);
        }

        public void insert(Object[] datas) {
            for (Object data : datas) {
                this.invoke(INVOKE.INSERT, data);
            }
        }

        public void insert(Object data) {
            this.invoke(INVOKE.INSERT, data);
        }

        public void insertOrReplace(Object[] datas) {
            for (Object data : datas) {
                this.invoke(INVOKE.INSERT_OR_REPLACE, data);
            }
        }

        public void insertOrReplace(Object data) {
            this.invoke(INVOKE.INSERT_OR_REPLACE, data);
        }

        public void deleteAll() {
            this.invoke(INVOKE.DELETE_ALL);
        }
    }

    private void populateDaoEntity(AbstractDaoSession daoSession){
        // populate all DAO
        Class defaultDAOSessionClass = daoSession.getClass();
        Method[] classMethods = defaultDAOSessionClass.getDeclaredMethods();
        for (int i = 0; i < classMethods.length; i++) {
            String methodName = classMethods[i].getName();
            String entityFullName = "";
            Method classMethod;
            if (methodName.startsWith("get") && methodName.endsWith("Dao")) {
                try {
                    classMethod = defaultDAOSessionClass.getMethod(methodName);
                    Object daoInstance = classMethod.invoke(daoSession);
                    entityFullName = daoInstance.getClass().getCanonicalName();
                    if (daoInstance != null) {
                        EntityMapWrapper entity = new EntityMapWrapper((AbstractDao<?, Long>) daoInstance, entityFullName);
                        this.mDAOs.put(entity.hashMapKey, entity);
                    }
                } catch (SecurityException e) { // exception handling omitted for brevity
                    e.printStackTrace();
                } catch (NoSuchMethodException e) { // exception handling omitted for brevity
                    e.printStackTrace();
                } catch (IllegalArgumentException e) { // exception handling omitted for brevity
                    e.printStackTrace();
                } catch (IllegalAccessException e) { // exception handling omitted for brevity
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public EntityMapWrapper getEntity(Class<?> klass) {
        return this.getEntity(klass.getSimpleName());
    }

    public EntityMapWrapper getEntity(String tableName) {
        EntityMapWrapper entityWrapper = this.mDAOs.get(tableName);
        return entityWrapper;
    }

    public void FlushAll() {
        Iterator it = this.mDAOs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            this.getEntity((String) pair.getKey()).deleteAll();
        }
    }

    public class InvokeError extends Error{
        public InvokeError(Throwable e) {
            super(e);
        }
    }
}
