package com.dji.djiaapp2.logic;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.dji.djiaapp2.MApplication;
import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.AppConfiguration;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * For handling virtual manual control of drone (virtual joystick logic)
 */

public class VirtualControllerHandler {

    private FlightController flightController;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    float verticalJoyControlMaxSpeed = 2;
    float yawJoyControlMaxSpeed = 30;

    public VirtualControllerHandler(boolean isOnMission) {
        BaseProduct product = MApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                this.flightController = ((Aircraft) product).getFlightController();

                this.flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                this.flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                this.flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                this.flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                this.flightController.setVirtualStickAdvancedModeEnabled(true);
                this.enable(true, isOnMission);
            }
        }
    }

    public void enable(boolean isEnabled, boolean onMission) {
        this.flightController.setVirtualStickModeEnabled(isEnabled, djiError -> {
            if (djiError != null){
                Log.e("VirtualController", djiError.getDescription());
            } else {
                if (!onMission) {
                    this.flightController.startTakeoff(djiError2 -> {
                        if (djiError2 != null) {
                            Log.e("VirtualController", djiError2.getDescription());
                        }
                    });
                }
            }
        });
    }

    public void moveRightStick(float pX, float pY) {
        if (Math.abs(pX) < 0.2 && Math.abs(pY) < 0.2) {
            Drone.getInstance().setmPitch(0);
            Drone.getInstance().setmRoll(0);
            return;
        }

        Drone.getInstance().setmPitch(pX * AppConfiguration.maxSpeed);
        Drone.getInstance().setmRoll(pY * AppConfiguration.maxSpeed);

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    public void moveLeftStick(float pX, float pY) {
        if (Math.abs(pX) < 0.2 && Math.abs(pY) < 0.2) {
            Drone.getInstance().setmYaw(0);
            Drone.getInstance().setmThrottle(0);
            return;
        }

        Drone.getInstance().setmYaw(pX * yawJoyControlMaxSpeed);
        Drone.getInstance().setmThrottle(pY * verticalJoyControlMaxSpeed);

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    public void move(float x, float y) {
        Drone.getInstance().setmPitch(x * AppConfiguration.maxSpeed);
        Drone.getInstance().setmRoll(y * AppConfiguration.maxSpeed);

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    public void stopMoving() {
        Drone.getInstance().setAxes(0, 0 ,0 ,0);
        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    public void startLand() {
        if (Drone.getInstance().getMode() == DRONE_MODE_FREE) {
            flightController.startLanding(djiError -> {

            });
        }
    }

    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                float p = Drone.getInstance().getmPitch();
                float r = Drone.getInstance().getmRoll();
                float w = Drone.getInstance().getmYaw();
                float t = Drone.getInstance().getmThrottle();
                flightController.sendVirtualStickFlightControlData(new FlightControlData(p, r, w, t), djiError -> { });
            }
        }
    }

    public void getAltitude(MutableLiveData<Float> currentAlt)
    {
        flightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                currentAlt.postValue(flightControllerState.getAircraftLocation().getAltitude());
            }
        });
    }
}
