package com.dji.GSDemo.GaodeMap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.Camera.DJICameraSettingsDef.CameraMode;
import dji.sdk.Camera.DJICameraSettingsDef.CameraShootPhotoMode;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent.DJICompletionCallback;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;
import dji.sdk.base.DJIError;

public class TakePhotoActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();

    protected CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextView mConnectStatusTextView;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    //照片参数设置
    private Button backBtn;
    private final int PhtotoSetting = Menu.FIRST;
    private final int CaptureFrequence = Menu.FIRST+1;
    private Spinner ISOSpinner;//曝光度
    private Spinner SatSpinner;//饱和度
    private Spinner HueSpinner;//色调
    private RadioGroup sharpRadios;//锐度
    private RadioGroup contrastRadios;//对比度
    private RadioGroup photoQualityRadios;//JEPG照片的质量


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
            onProductChange();
        }

    };

    /**
     * 根据连接的产品更换标题栏的名称
     */
    private void updateTitleBar() {
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                //The product is connected
                mConnectStatusTextView.setText(DJIDemoApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft)product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            // The product or the remote controller are not connected.
            mConnectStatusTextView.setText("Disconnected");
        }
    }

    /**
     * 产品更换的时候,初始化预览窗口
     */
    protected void onProductChange() {
        initPreviewer();
    }

    /*
    以下是创建Activity的覆盖方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_photo);

        initUI();
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if(mCodecManager != null){
                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };

        DJICamera camera = DJIDemoApplication.getCameraInstance();

        if (camera != null) {
            camera.setDJICameraUpdatedSystemStateCallback(new DJICamera.CameraUpdatedSystemStateCallback() {
                @Override
                public void onResult(DJICamera.CameraSystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        TakePhotoActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
        }

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

    }

    @Override
    public void onResume() {
                    Log.e(TAG, "onResume");
                    super.onResume();
                    initPreviewer();
                    updateTitleBar();
                    if(mVideoSurface == null) {
                        Log.e(TAG, "mVideoSurface is null");
                    }
                }

    @Override
    public void onPause() {
                    Log.e(TAG, "onPause");
                    uninitPreviewer();
                    super.onPause();
                }

    @Override
    public void onStop() {
                    Log.e(TAG, "onStop");
                    super.onStop();
                }

    public void onReturn(View view){
                    Log.e(TAG, "onReturn");
                    this.finish();
                }
    @Override
    protected void onDestroy() {
                    Log.e(TAG, "onDestroy");
                    uninitPreviewer();
                    unregisterReceiver(mReceiver);
                    super.onDestroy();
                }

    /*
    以下是初始化窗口
     */

    /**
     * 初始化拍照录像界面
     */
    private void initUI() {
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

//        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    /**
     * 初始化所有spinner
     */
    private void initSpinners(View view) {//// TODO: 2016/8/6  need test
        final DJICamera camera = DJIDemoApplication.getCameraInstance();
        if(camera==null)
        {
            showToast("camera not connect");
//            return;
        }
		ISOSpinner = initSpinner(view,R.id.isoSpinner,R.array.ISO, 0);
		SatSpinner = initSpinner(view,R.id.sat,R.array.from_3to3, 3);
		HueSpinner  = initSpinner(view,R.id.hue,R.array.from_3to3,3);
        ISOSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int id, long position) {
                String selectedItem = (String)adapterView.getSelectedItem();
                showToast(selectedItem);
                if(camera==null) return;//// TODO: 2016/8/6  delete this line
                switch (id)
                {
                    case 0:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.Auto, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 1:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_100, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 2:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_200, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 3:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_400, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 4:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_800, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 5:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_1600, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 6:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_3200, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 7:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_6400, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 8:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_12800, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case 9:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.ISO_25600, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    default:
                    {
                        camera.setISO(DJICameraSettingsDef.CameraISO.Auto, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("Error :"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
	    SatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = (String)adapterView.getSelectedItem();
                showToast(""+(selectedItem)+" "+(i-3));
                if(camera==null) return;//// TODO: 2016/8/6  delete this line
                camera.setSaturation(i - 3, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError!=null)
                            showToast("Error :"+djiError.getDescription());


                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        HueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = (String)adapterView.getSelectedItem();
                showToast(""+(selectedItem)+" "+(i-3));
                if(camera==null) return;//// TODO: 2016/8/6  delete this line
                camera.setHue(i - 3, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError!=null)
                            showToast("Error :"+djiError.getDescription());
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * 初始化单个spinner
     */
    private  Spinner initSpinner(View view,int id,int arrayResource,int selection) {
		Spinner spinner =(Spinner)view.findViewById(id);
		String [] items  =getResources().getStringArray(arrayResource);
		ArrayAdapter<String> aa =
				new ArrayAdapter<String>(
						this,
						android.R.layout.simple_spinner_item, 
						items);
		aa.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(aa);
		spinner.setSelection(selection);
        return spinner;
	}

    /**
     * 初始化照相参数设置的单选按钮
     * 并添加单选按钮组的事件监听
     */
    private void initPhotoSettingRadio(View view){
        final DJICamera camera = DJIDemoApplication.getCameraInstance();
        if(camera==null)
        {
            showToast("camera not connect");
            return;
        }
        sharpRadios = (RadioGroup)view.findViewById(R.id.sharp);
        contrastRadios = (RadioGroup)view.findViewById(R.id.contrast);
        photoQualityRadios = (RadioGroup)view.findViewById(R.id.quality);

        sharpRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.sharp_hard:{
                        camera.setSharpness(DJICameraSettingsDef.CameraSharpness.Hard, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.sharp_standard:{
                        camera.setSharpness(DJICameraSettingsDef.CameraSharpness.Standard, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.sharp_soft:{
                        camera.setSharpness(DJICameraSettingsDef.CameraSharpness.Soft, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }

                }
            }
        });
        contrastRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.contrast_hard: {
                        camera.setContrast(DJICameraSettingsDef.CameraContrast.Hard, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.contrast_standard: {
                        camera.setContrast(DJICameraSettingsDef.CameraContrast.Standard, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.contrast_soft: {
                        camera.setContrast(DJICameraSettingsDef.CameraContrast.Soft, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                }
            }
        });
        photoQualityRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.quality_normal: {
                        camera.setPhotoQuality(DJICameraSettingsDef.CameraPhotoQuality.Normal, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.quality_fine: {
                        camera.setPhotoQuality(DJICameraSettingsDef.CameraPhotoQuality.Fine, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                    case R.id.quality_excellent: {
                        camera.setPhotoQuality(DJICameraSettingsDef.CameraPhotoQuality.Excellent, new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                    showToast("设置出错:"+djiError.getDescription());
                            }
                        });
                        break;
                    }
                }
            }
        });
    }

    /*
    以下是初始化预览窗口
     */
    private void initPreviewer() {

        DJIBaseProduct product = DJIDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = product.getCamera();
                if (camera != null){
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        DJICamera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            DJIDemoApplication.getCameraInstance().setDJICameraReceivedVideoDataCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.e(TAG, "onSurfaceTextureAvailable");
                    if (mCodecManager == null) {
                        mCodecManager = new DJICodecManager(this, surface, width, height);
                    }
                }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.e(TAG, "onSurfaceTextureSizeChanged");
                }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    Log.e(TAG,"onSurfaceTextureDestroyed");
                    if (mCodecManager != null) {
                        mCodecManager.cleanSurface();
                        mCodecManager = null;
                    }

                    return false;
                }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    /*
     以下是创建菜单项
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, PhtotoSetting, Menu.NONE, "相机参数设置");
        menu.add(Menu.NONE, CaptureFrequence, Menu.NONE, "定时采集照片设置");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch(id){
            case PhtotoSetting:{
                final LinearLayout layout = (LinearLayout)getLayoutInflater()
                        .inflate(R.layout.dialog_photoparameter,null);
                setContentView(layout);
                initSpinners(layout);
                initPhotoSettingRadio(layout);
                backBtn = (Button)layout.findViewById(R.id.backBtn);
                backBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                      setContentView(R.layout.activity_photo);
                    }
                });
                break;
            }
            case CaptureFrequence:{
                LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_capture_frequency,null);
                final EditText numEdit =(EditText)layout.findViewById(R.id.photoNum);
                final EditText intervalEdit =(EditText)layout.findViewById(R.id.photoInterval);
                new AlertDialog.Builder(this)
                        .setTitle("计划任务")
                        .setView(layout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String numStr = numEdit.getText().toString();
                                String interval = intervalEdit.getText().toString();
                                if(numStr!=null&&interval!=null&&!numStr.equals("")&&!interval.equals(""))
                                {
                                    DJICamera camera = DJIDemoApplication.getCameraInstance();
                                    if(camera==null)
                                    {
                                        showToast("camera not connect");
                                        return;
                                    }
                                    int intervalnum = Integer.parseInt(interval);
                                    int count = Integer.parseInt(numStr);
                                    if(!(count<=255&&count>=2))
                                    {
                                        showToast("数目应该在2到255之间");
                                        return;
                                    }
                                    if(!(intervalnum>=2))
                                    {
                                        showToast("间隔应该大于等于2秒");
                                    }
                                    DJICameraSettingsDef.CameraPhotoIntervalParam param = new DJICameraSettingsDef.CameraPhotoIntervalParam();
                                    param.captureCount = count;
                                    param.timeIntervalInSeconds = intervalnum;
                                    camera.setPhotoIntervalParam(param, new DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if(djiError==null)
                                            {
                                                showToast("设置计划拍照任务成功");
                                            }else{
                                                showToast("任务设置失败: "+djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    监听按钮事件
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(CameraMode.ShootPhoto);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(CameraMode.RecordVideo);
                break;
            }
            default:
                break;
        }
    }

    /*
    以下是照相录像功能
     */
    private void switchCameraMode(CameraMode cameraMode){

        DJICamera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setCameraMode(cameraMode, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }

    }


    // Method for taking photo
    private void captureAction(){

        CameraMode cameraMode = CameraMode.ShootPhoto;

        final DJICamera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null) {

            CameraShootPhotoMode photoMode = CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
            camera.startShootPhoto(photoMode, new DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("take photo: success");
                    } else {
                        showToast(error.getDescription());
                    }
                }

            }); // Execute the startShootPhoto API
        }
    }

    // Method for starting recording
    private void startRecord(){

        CameraMode cameraMode = CameraMode.RecordVideo;
        final DJICamera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new DJICompletionCallback(){
                @Override
                public void onResult(DJIError error)
                {
                    if (error == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        DJICamera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new DJICompletionCallback(){

                @Override
                public void onResult(DJIError error)
                {
                    if(error == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TakePhotoActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

}
