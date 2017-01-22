package com.dji.GSDemo.GaodeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stu.database.ChargeAreaManager;

public class HomeManageActivity extends Activity {

    private ChargeAreaManager chargeAreaManager;
    private Button openBtn,insertBtn,deleteBtn,readBtn,deleteAllBtn;
    private EditText homeName,longitude,latitude,deleteId;
    private TextView msgTv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_manage);
        chargeAreaManager = new ChargeAreaManager(getBaseContext());

        insertBtn = (Button)findViewById(R.id.insertBtn);
        deleteBtn = (Button)findViewById(R.id.deleteBtn);
        readBtn = (Button)findViewById(R.id.readBtn);
        deleteAllBtn = (Button)findViewById(R.id.deleteAllBtn);
        msgTv = (TextView)findViewById(R.id.msgTv) ;
        deleteId = (EditText)findViewById(R.id.deleteId);

        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInsertHomePointDialog();
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id_str = deleteId.getText().toString();
                int id;
                showToast("id_str = "+id_str);
                if(id_str.equals(""))
                {
                    showToast("请输入删除的Id");
                }
                id = Integer.parseInt(id_str);
                chargeAreaManager.delete(id);
                showToast("删除id = "+id+ " 成功");
            }
        });

        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = chargeAreaManager.getCursor();
                String msg ="";
                if(cursor==null|| !cursor.moveToFirst())
                {
                    showToast("没有数据");
                    return ;
                }
                do
                {
                    msg+=(cursor.getString(0)+"/"+cursor.getString(1)+"/"+cursor.getString(2)+"/"+cursor.getString(3)+"\n");
                }while(cursor.moveToNext());
                msg+="共 "+cursor.getCount()+" 条";
                msgTv.setText(msg);
                chargeAreaManager.closeDatabase();
            }
        });

        deleteAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(HomeManageActivity.this)
                        .setTitle("清空")
                        .setMessage("是否清空数据库?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                chargeAreaManager.truncate();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();

            }
        });
    }

    private void showInsertHomePointDialog(){
        LinearLayout linearLayout =(LinearLayout) getLayoutInflater().inflate(R.layout.dialog_homesetting,null);
        longitude= (EditText)linearLayout.findViewById(R.id.longitude);
        latitude = (EditText)linearLayout.findViewById(R.id.latitude);
        homeName = (EditText)linearLayout.findViewById(R.id.homeName);
        new AlertDialog.Builder(this)
                .setView(linearLayout)
                .setTitle("添加充电点")
                .setPositiveButton("添加", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        double lng,lat;
                        showToast(" longitude = "+longitude.getText().toString());
                        if(!longitude.getText().toString().equals(""))
                            lng = Double.parseDouble(longitude.getText().toString());
                        else {
                            showToast("请输入数据");
                            return;
                        }
                        if(!latitude.getText().toString().equals(""))
                            lat = Double.parseDouble(latitude.getText().toString());
                        else {
                            showToast("请输入数据");
                            return;
                        }
                        if(!homeName.getText().toString().equals(""))
                         chargeAreaManager.insert(homeName.getText().toString(),lat,lng);
                        else {
                            showToast("请输入数据");
                            return;
                        }

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setContentView(R.layout.activity_home_manage);
                    }
                }).create().show();


    }

    private void showToast(final String message){
        Toast.makeText(getBaseContext(),message,Toast.LENGTH_LONG).show();
    }
}
