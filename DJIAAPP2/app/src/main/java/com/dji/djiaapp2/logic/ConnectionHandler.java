package com.dji.djiaapp2.logic;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dji.djiaapp2.MApplication;
import com.dji.djiaapp2.models.Drone;

import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Handles the connection logic when the application is launched
 * Establishes registration with DJI Mobile SDK then connection with the drone
 */
public class ConnectionHandler {
    private static final String TAG = "Connection Handler";

    private AtomicBoolean isRegistrationInProgress;
    private boolean isSDKRegistered;

    public ConnectionHandler() {
        isRegistrationInProgress = new AtomicBoolean(false);
        isSDKRegistered = false;
    }

    public void startSDKRegistration(Callback callback, Context context) {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                Log.i(TAG, "registering, pls wait...");
                DJISDKManager.getInstance().registerApp(context, new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            Log.i(TAG, "Register Success");
                            //DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP("0.0.0.0"); // For DJI Bridging App Testing
                            DJISDKManager.getInstance().startConnectionToProduct();
                            isSDKRegistered = true;
                        } else {
                            Log.i(TAG, "Register sdk fails, please check the bundle id and network connection!");
                            isSDKRegistered = false;
                        }
                        Log.v(TAG, djiError.getDescription());
                        callback.onComplete();
                    }

                    @Override
                    public void onProductDisconnect() {
                        Log.d(TAG, "onProductDisconnect");
                        updateProductConnect(false);
                        callback.onComplete();
                    }
                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                        updateProductConnect(true);
                        callback.onComplete();
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {
                    }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                  BaseComponent newComponent) {

                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> {
                                Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                callback.onComplete();
                            });
                        }
                        Log.d(TAG,
                                String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                        componentKey,
                                        oldComponent,
                                        newComponent));

                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
                    }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) {
                    }
                });
            });
        }
    }

    public boolean isSDKRegistered() {
        return isSDKRegistered;
    }

    private void updateProductConnect(boolean isSuccess) {
        if (isSuccess) {
            Drone.getInstance().setConnected(true);
            BaseProduct mProduct = MApplication.getProductInstance();
            Drone.getInstance().setModel(mProduct.getModel().getDisplayName());
        } else {
            Drone.getInstance().setConnected(false);
            Drone.getInstance().setModel("Not Detected");
        }
    }
}
