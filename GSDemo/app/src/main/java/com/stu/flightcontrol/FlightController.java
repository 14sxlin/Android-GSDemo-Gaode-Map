package com.stu.flightcontrol;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.stu.database.ChargeAreaManager;
import com.stu.database.HomePoint;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.Battery.DJIBattery;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

/**
 *
 * TODO  功能可能需要更改
 * Created by LinSixin on 2016/8/26.
 */
public class FlightController {

    private static final double MAXLENGTH = 9999999999.999999;
    DJIBaseProduct product;

    public boolean useable =  false;//信息完整 可以使用函数

    private DJIFlightController controller;
    private ChargeAreaManager areaManager;
    private int goHomeThreshold;
    private int currentBatteryPrecent;
    private DJIBattery battery;

    public FlightController(DJIBaseProduct product, Context context){
        this.product = product;
        areaManager = new ChargeAreaManager(context);
        if(product!=null && product.isConnected()) {
            battery = product.getBattery();
            controller = ((DJIAircraft)product).getFlightController();
        }else{
            showToast("产品未连接 无法获取电池信息");
            useable = false;
            return;
        }
        battery.setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
            @Override
            public void onResult(DJIBattery.DJIBatteryState djiBatteryState) {
                currentBatteryPrecent = djiBatteryState.getBatteryEnergyRemainingPercent();
                if(shouldGoHome())
                {
                    showToast("测试: 电量达到返航要求了哦");
                    Log.d("demo","电量达到返航要求");
                }
            }
        });

        useable = true;


    }


    /**
     * 读取数据库中已经保存的充电点,然后比较距离,返回最近的点
     * @return
     */
    private HomePoint findShortestHomePoint(){
        // TODO: 2016/8/26  to test
        if(!useable) return null;

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
     * 检查电量是否应该返回
     * @return true 应该返回 false 不应该返回
     *
     */
    public boolean shouldGoHome(){
        // TODO: 2016/8/26 to  test
        assert useable == true;
        final HomePoint minPoint = findShortestHomePoint();
        controller.setHomeLocation(new DJIFlightControllerDataType.DJILocationCoordinate2D(minPoint.getLat(),minPoint.getLng()), new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError!=null){
                    showToast("设置Home点失败 : "+djiError.getDescription());
                }
            }
        });


        controller.getGoHomeBatteryThreshold(new DJIBaseComponent.DJICompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                goHomeThreshold = integer;
                showToast("Go Home Threshold : " +integer+"%");
            }

            @Override
            public void onFailure(DJIError djiError) {
                showToast("获取返程阀值失败: "+djiError.getDescription());
            }
        });

        if(goHomeThreshold>=currentBatteryPrecent)
            return true;
        else return false;//不应该返航

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
