package com.stu.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by LinSiXin on 2016/10/1.
 */

public class ChargeAreaDBHelper extends SQLiteOpenHelper {

    public static final String TABLENAME = "chargeArea";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    private static final String path = "/data/data/com.dji.GSDemo.GaodeMap/databases/chargearea.db";

    final String createTable = "CREATE TABLE IF NOT EXISTS "+TABLENAME+"(" +
            COL_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            COL_NAME+" TEXT NOT NULL," +
            COL_LAT+" DOUBLE NOT NULL," +
            COL_LNG+" DOUBLE NOT NULL)";


    public ChargeAreaDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("demo","database upgrade");
    }
}
