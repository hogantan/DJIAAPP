package com.dji.djiaapp2.logic;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;

import android.util.Log;

import com.dji.djiaapp2.MApplication;
import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.AppConfiguration;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * For handling virtual manual control of drone (virtual joystick logic)
 */

public class VirtualControllerHandler {

    private static final String TAG = "Virtual Controller";

    private FlightController flightController;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private final float verticalJoyControlMaxSpeed = 2;
    private final float yawJoyControlMaxSpeed = 30;

    public VirtualControllerHandler() {
        BaseProduct product = MApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                this.flightController = ((Aircraft) product).getFlightController();
                this.flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                this.flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                this.flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                this.flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                this.flightController.setVirtualStickAdvancedModeEnabled(true);
                this.flightController.setVirtualStickModeEnabled(true, djiError -> {
                    if (djiError != null){
                        Log.e(TAG, djiError.getDescription());
                    }
                });
            }
        }
    }

    public void moveRightStick(float pX, float pY) {
        if (Math.abs(pX) < 0.2 && Math.abs(pY) < 0.2) {
            Drone.getInstance().setmPitch(0);
            Drone.getInstance().setmRoll(0);
            return;
        }

        Drone.getInstance().setmPitch(pX * AppConfiguration.MAX_SPEED);
        Drone.getInstance().setmRoll(pY * AppConfiguration.MAX_SPEED);

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
        Drone.getInstance().setmPitch(x * AppConfiguration.MAX_SPEED);
        Drone.getInstance().setmRoll(y * AppConfiguration.MAX_SPEED);

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
            flightController.startGoHome(djiError -> {
                if (djiError != null) {
                    Log.e(TAG, djiError.getDescription());
                }
            });
        }
    }

    public void startTakeoff() {
        this.flightController.startTakeoff(djiError2 -> {
            if (djiError2 != null) {
                Log.e(TAG, djiError2.getDescription());
            }
        });
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

    public void onUpdate(FlightControllerState.Callback callback) {
        flightController.setStateCallback(callback);
    }
}
