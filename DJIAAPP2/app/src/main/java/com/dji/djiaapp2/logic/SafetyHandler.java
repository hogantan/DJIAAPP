package com.dji.djiaapp2.logic;

import android.util.Log;

import com.dji.djiaapp2.MApplication;
import com.dji.djiaapp2.utils.AppConfiguration;

import dji.common.flightcontroller.ConnectionFailSafeBehavior;

import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * For safety behavior of drone
 */

public class SafetyHandler {
    private static final String TAG = "Safety Handler";

    private FlightController flightController;

    public SafetyHandler() {
        BaseProduct product = MApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                this.flightController = ((Aircraft) product).getFlightController();

                // "Geo-Fencing" Settings
                this.flightController.setMaxFlightRadiusLimitationEnabled(true, djiError -> {
                    if (djiError == null) {
                        flightController.setMaxFlightRadius(AppConfiguration.MAX_FLIGHT_RADIUS, djiError1 -> {
                            if (djiError1 != null) {
                                Log.e(TAG, djiError1.getDescription());
                            }
                        });
                    } else {
                        Log.e(TAG, djiError.getDescription());
                    }
                });

                // Connection failsafe
                this.flightController.setConnectionFailSafeBehavior(ConnectionFailSafeBehavior.GO_HOME, djiError -> {
                    if (djiError != null) {
                        Log.e(TAG, djiError.getDescription());
                    }
                });
            }
        }
    }
}
