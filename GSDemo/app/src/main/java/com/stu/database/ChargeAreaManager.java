package com.stu.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by acer on 2016/8/11.
 */
public class ChargeAreaManager {


    private ChargeAreaDBHelper dbHelper;

    private static final String path = "/data/data/com.dji.GSDemo.GaodeMap/databases/chargearea.db";


    private SQLiteDatabase chargeAreaDB = null;

    public ChargeAreaManager(Context context){

        dbHelper = new ChargeAreaDBHelper(context,path,null,1);
    }




//    /**
//     * 打开或者创建数据表
//     * @return true 则创建或者打开数据库表成功
//     */
//    private boolean createOrOpenTable(){
//        String createTable = "CREATE TABLE IF NOT EXISTS "+TABLENAME+"(" +
//                COL_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
//                COL_NAME+" TEXT NOT NULL," +
//                COL_LAT+" DOUBLE NOT NULL," +
//                COL_LNG+" DOUBLE NOT NULL)";
//        if(chargeAreaDB==null)
//            return false;
//        chargeAreaDB.execSQL(createTable);
//        return true;
//    }

    /**
     * 插入数据
     * @param latitude 纬度
     * @param longitude 经度
     */
    public void insert(String name,double latitude,double longitude){
        chargeAreaDB = dbHelper.getWritableDatabase();
        String insertSql = "INSERT INTO "+ChargeAreaDBHelper.TABLENAME+"" +
                "(name,latitude,longitude)" +
                "VALUES('"+name+"',"+latitude+","+longitude+")";
        if(chargeAreaDB==null)
            return;
        chargeAreaDB.execSQL(insertSql);
        chargeAreaDB.close();
    }

    /**
     * 查询数据
     * @return 返回Cursor指针
     */
    public Cursor getCursor(){
        chargeAreaDB = dbHelper.getReadableDatabase();
        if(chargeAreaDB!=null)
            return chargeAreaDB.query(
                    ChargeAreaDBHelper.TABLENAME,
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
        chargeAreaDB = dbHelper.getReadableDatabase();
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
        chargeAreaDB.close();
        return list;
    }

    /**
     * 删除数据表中的数据
     * @param id 要删除的数目的id
     * @return
     */
    public boolean delete(int id){
        chargeAreaDB = dbHelper.getWritableDatabase();
        String deleteSQL = "DELETE FROM "+ChargeAreaDBHelper.TABLENAME+" WHERE id = "+id;
        if(chargeAreaDB==null) return false;
        chargeAreaDB.execSQL(deleteSQL);
        chargeAreaDB.close();
        return true;
    }

    /**
     * 清空数据库表数据
     */
    public void truncate(){
        chargeAreaDB = dbHelper.getWritableDatabase();
        String truncateSQL = "DROP TABLE "+ChargeAreaDBHelper.TABLENAME;
        if(chargeAreaDB!=null)
        {
            chargeAreaDB.execSQL(truncateSQL);
        }
        chargeAreaDB.close();

    }

    /**
     * 关闭数据库
     */
    public void closeDatabase(){
        if(chargeAreaDB!=null)
            chargeAreaDB.close();

    }

}
