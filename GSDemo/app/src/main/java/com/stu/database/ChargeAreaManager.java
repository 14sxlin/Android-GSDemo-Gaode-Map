package com.stu.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by acer on 2016/8/11.
 */
public class ChargeAreaManager {

    public static final String TABLENAME = "chargeArea";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    private static final String path = "/data/data/com.dji.GSDemo.GaodeMap/databases/chargearea.db";
    private SQLiteDatabase chargeAreaDB = null;

    public ChargeAreaManager(){
        createOrOpenDatabase();
    }
    /**
     * 创建或者打开数据库
     * @return true 则打开或创建数据库成功
     */
    public boolean createOrOpenDatabase(){
        chargeAreaDB = SQLiteDatabase.openOrCreateDatabase(path,null);
        createOrOpenTable();
        return chargeAreaDB!=null;
    }

    /**
     * 打开或者创建数据表
     * @return true 则创建或者打开数据库表成功
     */
    private boolean createOrOpenTable(){
        String createTable = "CREATE TABLE IF NOT EXISTS "+TABLENAME+"(" +
                COL_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_NAME+" TEXT NOT NULL," +
                COL_LAT+" DOUBLE NOT NULL," +
                COL_LNG+" DOUBLE NOT NULL)";
        if(chargeAreaDB==null)
            return false;
        chargeAreaDB.execSQL(createTable);
        return true;
    }

    /**
     * 插入数据
     * @param latitude 纬度
     * @param longitude 经度
     */
    public void insert(String name,double latitude,double longitude){
        String insertSql = "INSERT INTO "+TABLENAME+"" +
                "(name,latitude,longitude)" +
                "VALUES('"+name+"',"+latitude+","+longitude+")";
        if(chargeAreaDB==null)
            return;
        chargeAreaDB.execSQL(insertSql);
    }

    /**
     * 查询数据
     * @return 返回Cursor指针
     */
    public Cursor getCursor(){
        if(chargeAreaDB!=null)
            return chargeAreaDB.query(
                    TABLENAME,
                    new String[]{"id","name","latitude","longitude"},
                    null,//where
                    null,//whereArgs
                    null,//groupBy
                    null,//having
                    null//orderby
            );
        return null;
    }

    public ArrayList<HomePoint> getHomePointList(){
        // TODO: 2016/8/26  test
        ArrayList<HomePoint> list = new ArrayList<HomePoint>();
        Cursor cursor = getCursor();
        if(cursor==null) return  null;
        if(!cursor.moveToFirst()) return null;
        do{
            HomePoint hp = new HomePoint();
            hp.setId(cursor.getLong(0));
            hp.setName(cursor.getString(1));
            hp.setLat(cursor.getDouble(2));
            hp.setLng(cursor.getDouble(3));
            list.add(hp);
        }while(cursor.moveToNext());
        return list;
    }

    /**
     * 删除数据表中的数据
     * @param id 要删除的数目的id
     * @return
     */
    public boolean delete(int id){
        String deleteSQL = "DELETE FROM "+TABLENAME+" WHERE id = "+id;
        if(chargeAreaDB==null) return false;
        chargeAreaDB.execSQL(deleteSQL);
        return true;
    }

    /**
     * 清空数据库表数据
     */
    public void truncate(){
        String truncateSQL = "DROP TABLE "+TABLENAME;
        if(chargeAreaDB!=null)
        {
            chargeAreaDB.execSQL(truncateSQL);
            createOrOpenTable();
        }

    }

    /**
     * 关闭数据库
     */
    public void closeDatabase(){
        if(chargeAreaDB!=null)
            chargeAreaDB.close();

    }

}
