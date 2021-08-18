package com.hzontal.tella_vault.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hzontal.tella_vault.IVaultDatabase;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import timber.log.Timber;


public class VaultDataSource implements IVaultDatabase {
    public static final String ROOT_UID = "11223344-5566-4777-8899-aabbccddeeff";

    private static VaultDataSource dataSource;
    private static Gson gson;
    private final SQLiteDatabase database;

    public static synchronized VaultDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new VaultDataSource(context.getApplicationContext(), key);
            gson = new GsonBuilder().create();
        }

        return dataSource;
    }

    public VaultDataSource(Context context, byte[] key) {
        VaultSQLiteOpenHelper sqLiteOpenHelper = new VaultSQLiteOpenHelper(context);
        SQLiteDatabase.loadLibs(context);

        database = sqLiteOpenHelper.getWritableDatabase(key);
    }

    @Override
    public VaultFile getRootVaultFile() {
        return get(ROOT_UID);
    }

    @SuppressLint("TimberArgCount")
    @Override
    public VaultFile create(String parentId, VaultFile vaultFile) {
        if (vaultFile.created == 0) {
            vaultFile.created = System.currentTimeMillis();
        }

        try {
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(D.C_ID, vaultFile.id);
            values.put(D.C_TYPE, vaultFile.type.getValue());
            values.put(D.C_PARENT_ID, parentId);
            values.put(D.C_NAME, vaultFile.name);
            values.put(D.C_CREATED, vaultFile.created);
            values.put(D.C_DURATION, vaultFile.duration);
            values.put(D.C_SIZE, vaultFile.size);
            values.put(D.C_ANONYMOUS, vaultFile.anonymous ? 1 : 0);
            values.put(D.C_HASH, vaultFile.hash);
            values.put(D.C_THUMBNAIL, vaultFile.thumb);
            values.put(D.C_MIME_TYPE, vaultFile.mimeType);
            values.put(D.C_PATH, vaultFile.path);
            values.put(D.C_METADATA, gson.toJson(vaultFile.metadata));

            database.insert(D.T_VAULT_FILE, null, values);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return vaultFile;
    }

    @Override
    public List<VaultFile> list(@Nullable VaultFile parent, @Nullable Filter filter, @Nullable Sort sort, @Nullable Limits limits) {
        List<VaultFile> vaultFiles = new ArrayList<>();

        String direction = Sort.Direction.ASC.name();
        String limit = null;
        String where;
        Cursor cursor = null;

        if (sort != null) {
            direction = sort.direction.name();
        }

        if (limits != null) {
            limit = String.valueOf(limits.limit);
        }

        where = cn(D.T_VAULT_FILE, D.C_PARENT_ID) + " = '" + (parent != null ? parent.id : ROOT_UID) + "'";

        try {
            // todo: add support for filter directly in query
            final String query = SQLiteQueryBuilder.buildQueryString(
                    false,
                    D.T_VAULT_FILE,
                    new String[]{
                            D.C_ID,
                            D.C_TYPE,
                            D.C_PARENT_ID,
                            D.C_NAME,
                            D.C_CREATED,
                            D.C_DURATION,
                            D.C_SIZE,
                            D.C_HASH,
                            D.C_ANONYMOUS,
                            D.C_THUMBNAIL,
                            D.C_MIME_TYPE,
                            D.C_PATH,
                            D.C_METADATA
                    },
                    where,
                    null,
                    null,
                    D.C_CREATED + " " + direction,
                    limit
            );

            cursor = database.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                vaultFiles.add(cursorToVaultFile(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return vaultFiles;
    }

    public VaultFile updateMetadata(VaultFile vaultFile, Metadata metadata) {
        ContentValues values = new ContentValues();

        values.put(D.C_METADATA, gson.toJson(metadata));

        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return get(vaultFile.id);
    }

    public VaultFile completeVaultOutputStream(VaultFile vaultFile) {
        ContentValues values = new ContentValues();

        values.put(D.C_HASH, vaultFile.hash);
        values.put(D.C_SIZE, vaultFile.size);
        values.put(D.C_DURATION, vaultFile.duration);

        database.update(D.T_VAULT_FILE, values, D.C_ID + " = ?",
                new String[]{vaultFile.id});

        return get(vaultFile.id);
    }

    @Override
    public VaultFile get(String id) {
        try (Cursor cursor = database.query(
                D.T_VAULT_FILE,
                new String[]{
                        D.C_ID,
                        D.C_PATH,
                        D.C_NAME,
                        D.C_METADATA,
                        D.C_CREATED,
                        D.C_DURATION,
                        D.C_ANONYMOUS,
                        D.C_SIZE,
                        D.C_HASH,
                        D.C_MIME_TYPE,
                        D.C_TYPE,
                        D.C_THUMBNAIL
                },
                cn(D.T_VAULT_FILE, D.C_ID) + " = ?", new String[]{id},
                null, null, null, null
        )) {
            if (cursor.moveToFirst()) {
                return cursorToVaultFile(cursor);
            }
        } catch (Exception e) {
            Timber.d(e, getClass().getName());
        }

        return null;
    }

    @Override
    public List<VaultFile> get(String[] ids) {
        return null;
    }

    @Override
    public boolean delete(VaultFile vaultFile, IVaultFileDeleter deleter) {
        try {
            database.beginTransaction();

            int count = database.delete(D.T_VAULT_FILE, D.C_ID + " = ?", new String[]{vaultFile.id});

            if (count != 1) {
                return false;
            }

            if (deleter.delete(vaultFile)) {
                database.setTransactionSuccessful();
            }

            return true;
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public void destroy() {
        deleteTable(D.T_VAULT_FILE);
    }

    @SuppressLint("TimberArgCount")
    private VaultFile cursorToVaultFile(Cursor cursor) {
        VaultFile vaultFile = new VaultFile();

        vaultFile.id = cursor.getString(cursor.getColumnIndexOrThrow(D.C_ID));
        vaultFile.type = VaultFile.Type.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(D.C_TYPE)));
        vaultFile.name = cursor.getString(cursor.getColumnIndexOrThrow(D.C_NAME));
        vaultFile.created = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_CREATED));
        vaultFile.duration = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_DURATION));
        vaultFile.size = cursor.getLong(cursor.getColumnIndexOrThrow(D.C_SIZE));
        vaultFile.anonymous = cursor.getInt(cursor.getColumnIndexOrThrow(D.C_ANONYMOUS)) == 1;
        vaultFile.hash = cursor.getString(cursor.getColumnIndexOrThrow(D.C_HASH));
        vaultFile.thumb = cursor.getBlob(cursor.getColumnIndexOrThrow(D.C_THUMBNAIL));
        vaultFile.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(D.C_MIME_TYPE));
        vaultFile.path = cursor.getString(cursor.getColumnIndexOrThrow(D.C_PATH));
        vaultFile.metadata = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(D.C_METADATA)), Metadata.class);

        return vaultFile;
    }

    private String cn(String table, String column) {
        return table + "." + column;
    }

    private String cn(String table, String column, String as) {
        return table + "." + column + " AS " + as;
    }
    private void deleteTable(String table) {
        database.execSQL("DELETE FROM " + table);
    }
}
