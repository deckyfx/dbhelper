package com.github.deckyfx.dbhelper;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.github.deckyfx.greendao.database.Database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// changes

public class DbUtils extends com.github.deckyfx.greendao.DbUtils {

    public static boolean checkDataBase(Context context, String databasename) {
        boolean checkDB = false;
        try {
            File file   = new File(context.getDatabasePath(databasename).getPath());
            checkDB     = file.exists();
        } catch(SQLiteException e) {
            e.printStackTrace();
        }
        return checkDB;
    }

    /*
        Import data from JSON text,
        Warning, this process should be run in Different threat than main to avoid screenlag
        All data will be overwritten
        The JSON should has structure
        {
            EntityName1: [ { Entity1Record1 }, { Entity1Record2 }, ... ],
            ...
        }
     */
    public static boolean importDbFromJsonString(String jsonText,
                                        DBHelper DbHelper,
                                        ArrayList<String> ignoreKeys,
                                        JSONImportConverter converter) {
        try {
            JSONObject json						= new JSONObject(jsonText);
            Iterator<?> keys = json.keys();
            if (ignoreKeys == null) {
                ignoreKeys                      = new ArrayList<>();
            }
            while( keys.hasNext() ) {
                String jsonkey_TableName        = (String) keys.next();
                if (ignoreKeys.contains(jsonkey_TableName)) {
                    continue;
                } else {
                    Entity<Object, Long> entity = DbHelper.getEntity(jsonkey_TableName);
                    if (entity == null) {
                        continue;
                    } else {
                        entity.deleteAll();
                        Object value            = json.get(jsonkey_TableName);
                        JSONObject obj;
                        Database db             = entity.entity.getDatabase();
                        db.beginTransaction();
                        try {
                            // do all your inserts and so on here.
                            if ( value instanceof JSONObject ) {
                                obj = (JSONObject) value;
                                if (obj.has(jsonkey_TableName)) {
                                    Object entityData = converter.entityFor(jsonkey_TableName, obj.get(jsonkey_TableName));
                                    if (entityData != null) {
                                        entity.insertOrReplace(entityData);
                                    }
                                }
                            } else if (value instanceof JSONArray) {
                                JSONArray array = (JSONArray) value;
                                for (int i = 0; i < array.length(); i++) {
                                    Object inner_value = array.get(i);
                                    if (inner_value instanceof JSONObject) {
                                        obj = (JSONObject) inner_value;
                                        if (obj.has(jsonkey_TableName)) {
                                            inner_value     = obj.get(jsonkey_TableName);
                                        }
                                        Object entityData = converter.entityFor(jsonkey_TableName, inner_value);
                                        if (entityData != null) {
                                            entity.insertOrReplace(entityData);
                                        }
                                    }
                                    array.put(i, inner_value);
                                }
                                value = array;
                            }
                            db.setTransactionSuccessful();
                            db.endTransaction();
                        } catch (Exception ex) {
                            SQLiteException se = new SQLiteException("Error while insert data to table " + jsonkey_TableName);
                            se.setStackTrace(ex.getStackTrace());
                            se.printStackTrace();
                            db.endTransaction();
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (JSONException e) {
            SQLiteException se = new SQLiteException("Unable to parse JSON text");
            se.setStackTrace(e.getStackTrace());
            se.printStackTrace();
            return false;
        }
    }

    public static void importDbFromAssets(Context context, String assetsFile, String databasename) throws SQLiteException {
        String path     = assetsFile;
        String dbpath   = context.getDatabasePath(databasename).getParent();
        String dest     = dbpath + "/" + databasename;
        InputStream is;
        boolean isZip = false;

        try {
            // try uncompressed
            is          = context.getAssets().open(path);
        } catch (IOException e) {
            // try zip
            try {
                is = context.getAssets().open(path + ".zip");
                isZip = true;
            } catch (IOException e2) {
                // try gzip
                try {
                    is = context.getAssets().open(path + ".gz");
                } catch (IOException e3) {
                    SQLiteException se = new SQLiteException("Missing " + path + " file (or .zip, .gz archive) in assets, or target folder not writable");
                    se.setStackTrace(e3.getStackTrace());
                    throw se;
                }
            }
        }

        try {
            File f = new File(dbpath + "/");
            if (!f.exists()) { f.mkdir(); }
            if (isZip) {
                ZipInputStream zis = getFileFromZip(is);
                if (zis == null) {
                    throw new SQLiteException("Archive is missing a SQLite database file");
                }
                writeExtractedFileToDisk(zis, new FileOutputStream(dest));
            } else {
                writeExtractedFileToDisk(is, new FileOutputStream(dest));
            }
        } catch (IOException e) {
            SQLiteException se = new SQLiteException("Unable to write " + dest + " to data directory");
            se.setStackTrace(e.getStackTrace());
            throw se;
        }
    }

    public static void writeExtractedFileToDisk(InputStream in, OutputStream outs) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer))>0){
            outs.write(buffer, 0, length);
        }
        outs.flush();
        outs.close();
        in.close();
    }

    public static ZipInputStream getFileFromZip(InputStream zipFileStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipFileStream);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            return zis;
        }
        return null;
    }

    public interface JSONImportConverter {
        public Object entityFor(String key, Object value);
    }
}
