package com.stu.flightcontrol;

import android.content.Context;
import android.widget.Toast;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;

/**
 * Created by LinSixin on 2016/8/26.
 */
public class FlightController {

    private DJIFlightController controller;

    public FlightController(DJIFlightController controller){
        this.controller = controller;
    }


    private DJIFlightControllerDataType.DJILocationCoordinate2D findShortestHome(){
        // TODO: 2016/8/26
        return null;
    }

    public void checkAndGoHome(){
        // TODO: 2016/8/26
        //获取当前的坐标
        //AMapUtils.calculateLineDistance();
    }

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
