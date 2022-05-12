package com.dji.djiaapp2.models;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_CHASE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_SEARCH;

/**
 * Singleton as only one drone per application run
 * Represents the drone that the application is connected to
 * Contains info such as whether the drone is connected to the application,
 * the model of the drone, etc.
 */
public class Drone {
    private static Drone instance = null;
    private boolean isConnected = false;
    private boolean isListeningToCommands = false;

    private int mode;
    private String model;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    private Drone(){}

    public static synchronized Drone getInstance(){
        if (instance == null)
            instance = new Drone();
        return instance;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getModel() {
        return model;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isOnMission() {
        return mode == DRONE_MODE_SEARCH;
    }

    public boolean isChasing() {
        return mode == DRONE_MODE_CHASE;
    }

    public boolean isFree() {
        return mode == DRONE_MODE_FREE;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public float getmPitch() {
        return mPitch;
    }

    public void setmPitch(float mPitch) {
        this.mPitch = mPitch;
    }

    public float getmRoll() {
        return mRoll;
    }

    public void setmRoll(float mRoll) {
        this.mRoll = mRoll;
    }

    public float getmYaw() {
        return mYaw;
    }

    public void setmYaw(float mYaw) {
        this.mYaw = mYaw;
    }

    public float getmThrottle() {
        return mThrottle;
    }

    public void setmThrottle(float mThrottle) {
        this.mThrottle = mThrottle;
    }

    public void setAxes(float pitch, float roll , float yaw, float throttle) {
        setmPitch(pitch);
        setmRoll(roll);
        setmYaw(yaw);
        setmThrottle(throttle);
    }

    public boolean isListeningToCommands() {
        return isListeningToCommands;
    }

    public void setListeningToCommands(boolean listeningToCommands) {
        isListeningToCommands = listeningToCommands;
    }
}
