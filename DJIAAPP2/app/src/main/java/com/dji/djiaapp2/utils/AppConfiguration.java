package com.dji.djiaapp2.utils;

import static dji.sdk.sdkmanager.LiveVideoResolution.VIDEO_RESOLUTION_1280_720;

import dji.sdk.sdkmanager.LiveVideoBitRateMode;
import dji.sdk.sdkmanager.LiveVideoResolution;

/*
 *  Global app configurations
 */
public class AppConfiguration {
    // Drone mode
    public static int DRONE_MODE_FREE = 0;
    public static int DRONE_MODE_SEARCH = 1;
    public static int DRONE_MODE_CHASE = 2;

    // Controller App settings
    public static String CONTROLLER_IP_ADDRESS = "127.0.0.1";
    public static int MAX_SPEED = 15;

    // RTSP Settings
    public static String RTSP_URL = "rtsp://127.0.0.1:8554/test";
    public static final int width = 1280;
    public static final int height = 720;
    public static final int fps = 30;
    public static final int bitrate = 5000 * 1000;
    public static final int iFrameInterval = 2;

    // RTMP Settings
    public static String RTMP_URL = "rtmp://127.0.0.1:1935/test";
    public static final LiveVideoResolution resolution = VIDEO_RESOLUTION_1280_720;
    public static final LiveVideoBitRateMode mode = LiveVideoBitRateMode.AUTO;

    // "Geo-Fencing" Setting
    public static int MAX_FLIGHT_RADIUS = 1000;

    // GCS TAG
    public static final String GCS_TAG = "GCS";

    public static void setControllerIpAddress(String ip) {
        if (!ip.isEmpty()) {
            CONTROLLER_IP_ADDRESS = ip.trim();
        }
    }

    public static void setScreenMirrorServerAddress(String ip) {
        if (!ip.isEmpty()) {
            RTSP_URL = ip.trim();
        }
    }

    public static void setRtmpUrl(String url) {
        if (!url.isEmpty()) {
            RTMP_URL = url.trim();
        }
    }

    public static void setMaxSpeed(int max) { if (max <= 15 && max >= 0) { MAX_SPEED = max; }}
}
