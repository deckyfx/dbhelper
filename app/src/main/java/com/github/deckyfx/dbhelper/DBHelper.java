package com.github.deckyfx.dbhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.deckyfx.greendao.AbstractDao;
import com.github.deckyfx.greendao.AbstractDaoMaster;
import com.github.deckyfx.greendao.AbstractDaoSession;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
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

    private HashMap<String, Entity> mDAOs;

    public static class INVOKE {
        public static final String QUERY_BUILDER        = "queryBuilder";
        public static final String LOAD                 = "load";
        public static final String LOAD_ALL             = "loadAll";
        public static final String INSERT_OR_REPLACE    = "insertOrReplace";
        public static final String INSERT               = "insert";
        public static final String DELETE_ALL           = "deleteAll";
        public static final String UPDATE               = "update";
    }

    public DBHelper(Context context, Class<? extends AbstractDaoMaster> daoMasterClass, String dbName){
        this.mDAOs = new HashMap<String, Entity>();

        if (daoMasterClass != null) {
            Method method = null;
            try {
                Class<?> devOpenHelperClass = Class.forName(daoMasterClass.getName() + "$" + "DevOpenHelper");
                Constructor<?> devOpenHelperCtor = devOpenHelperClass.getDeclaredConstructor(Context.class, String.class, SQLiteDatabase.CursorFactory.class);
                Constructor<?> daoMasterCtor = daoMasterClass.getConstructor(SQLiteDatabase.class);
                this.OpenHelper = (SQLiteOpenHelper) devOpenHelperCtor.newInstance(context, dbName, null);
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
                    AbstractDao<?, Long> daoInstance = (AbstractDao<?, Long>) classMethod.invoke(daoSession);
                    entityFullName = daoInstance.getClass().getCanonicalName();
                    if (daoInstance != null) {
                        Entity entity = new Entity(daoInstance, entityFullName);
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

    public Entity getEntity(Class<?> klass) {
        return this.getEntity(klass.getSimpleName());
    }

    public Entity getEntity(String tableName) {
        Entity entityWrapper = this.mDAOs.get(tableName);
        return entityWrapper;
    }

    public void FlushAll() {
        Iterator it = this.mDAOs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            this.getEntity((String) pair.getKey()).deleteAll();
        }
    }

    public static class InvokeError extends Error{
        public InvokeError(Throwable e) {
            super(e);
        }
    }
}
