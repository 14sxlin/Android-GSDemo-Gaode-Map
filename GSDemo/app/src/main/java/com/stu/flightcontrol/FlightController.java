package com.stu.flightcontrol;

import android.content.Context;
import android.widget.Toast;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.stu.database.ChargeAreaManager;
import com.stu.database.HomePoint;

import java.util.ArrayList;

import dji.midware.data.model.P3.DataFlycGetPushSmartBattery;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;

/**
 * Created by LinSixin on 2016/8/26.
 */
public class FlightController {

    private static final double MAXLENGTH = 9999999999.999999;
    private DJIFlightController controller;
    private ChargeAreaManager areaManager;


    public FlightController(DJIFlightController controller){
        this.controller = controller;
        areaManager = new ChargeAreaManager();
    }


    /**
     * 读取数据库中已经保存的充电点,然后比较距离,返回最近的点
     * @return
     */
    private HomePoint findShortestHome(){
        // TODO: 2016/8/26  to test
        double currentLat = getCurrentLocation().getLatitude();
        double currentLng = getCurrentLocation().getLongitude();
        LatLng currentLocation = new LatLng(currentLat,currentLng);
        HomePoint minHomePoint = null;
        double minLength = MAXLENGTH;
        ArrayList<HomePoint> list = areaManager.getHomePointList();
        for(HomePoint hp: list)
        {
            double length = AMapUtils.calculateLineDistance(currentLocation,new LatLng(hp.getLat(),hp.getLng()));
            if(length < minLength)
            {
                minHomePoint = hp;
            }
        }
        return minHomePoint;
    }

    /**
     * 检查电量是否充足,如果不足的话,就直接回家
     */
    public boolean checkAndGoHome(){
        // TODO: 2016/8/26 to  test 
        final HomePoint minPoint = findShortestHome();
        controller.setHomeLocation(new DJIFlightControllerDataType.DJILocationCoordinate2D(minPoint.getLat(),minPoint.getLng()), new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError!=null){
                    showToast("设置Home点失败 : "+djiError.getDescription());
                }
            }
        });

        DJIFlightControllerDataType.DJIFlightControllerSmartGoHomeStatus status = controller.getCurrentState().getSmartGoHomeStatus();
        if(status.isAircraftShouldGoHome())
        {
            controller.goHome(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("开始返回 "+minPoint.getName());
                    if(djiError!=null) {
                        showToast("返回Home点失败: "+djiError.getDescription());
                    }
                }
            });
            return true;
        }

        return false;
    }

    private DJIFlightControllerDataType.DJILocationCoordinate3D getCurrentLocation(){
        return controller.getCurrentState().getAircraftLocation();
    }

    /**
     * 设置电量警戒线
     * @param firstLevel
     * @param secondLevel
     */
    public void setBatteryAlert(int firstLevel,int secondLevel){
        // TODO: 2016/8/26
        boolean set1 ,set2;
        controller.setGoHomeBatteryThreshold(firstLevel, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null)
                    showToast(djiError.getDescription());
            }
        });
        controller.setLandImmediatelyBatteryThreshold(secondLevel, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null)
                    showToast(djiError.getDescription());
            }
        });
    }

    private void showToast(final String message){
        Toast.makeText(null,message,Toast.LENGTH_LONG).show();
    }


}
