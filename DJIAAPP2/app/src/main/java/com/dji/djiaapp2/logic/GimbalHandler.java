package com.dji.djiaapp2.logic;

import android.util.Log;

import com.dji.djiaapp2.MApplication;

import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.DJIParamMinMaxCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;

public class GimbalHandler {

    private Gimbal gimbal;

    public GimbalHandler() {
        BaseProduct product = MApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                gimbal = ((Aircraft) product).getGimbal();
                gimbal.setMode(GimbalMode.FREE, djiError -> {
                    lookDown();
                    if (djiError != null) {
                        Log.e("GimbalHandler", djiError.getDescription());
                    }
                });
            }
        }
    }

    public void lookDown() {
        Number angle = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(CapabilityKey.ADJUST_PITCH))).getMin();

        Rotation rotation = new Rotation.Builder()
                .mode(RotationMode.ABSOLUTE_ANGLE)
                .time(0.5)
                .pitch(angle.floatValue())
                .build();

        gimbal.rotate(rotation, djiError -> {
            if (djiError != null) {
                Log.e("GimbalHandler", djiError.getDescription());
            }
        });
    }
}
