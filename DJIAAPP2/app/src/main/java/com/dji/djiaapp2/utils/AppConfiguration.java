package com.dji.djiaapp2.utils;

/*
 *  Global app configurations
 */
public class AppConfiguration {
    // Drone mode
    public static int DRONE_MODE_FREE = 0;
    public static int DRONE_MODE_SEARCH = 1;
    public static int DRONE_MODE_CHASE = 2;
    // Controller App settings
    public static String CONTROLLER_IP_ADDRESS = "10.255.252.25";
    public static String SCREEN_MIRROR_RTSP_SERVER_ADDR = "rtsp://127.0.0.1:8554/test";
    public static String RTMPUrl = "rtmp://127.0.0.1:1936/test";
    // Movement settings
    public static int maxSpeed = 5;

    public static void setControllerIpAddress(String ip) {
        if (!ip.isEmpty()) {
            CONTROLLER_IP_ADDRESS = ip.trim();
        }
    }

    public static void setScreenMirrorServerAddr(String ip) {
        if (!ip.isEmpty()) {
            SCREEN_MIRROR_RTSP_SERVER_ADDR = ip.trim();
        }
    }

    public static void setRTMPUrl(String url) {
        if (!url.isEmpty()) {
            RTMPUrl = url.trim();
        }
    }

    public static void setMaxSpeed(int max) { if (max <= 15 && max >= 0) { maxSpeed = max; }}

}
