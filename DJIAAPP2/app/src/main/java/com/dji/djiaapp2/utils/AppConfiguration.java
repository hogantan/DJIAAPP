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
    public static String CONTROLLER_IP_ADDRESS = "127.0.0.1";
    public static String SCREEN_MIRROR_RTSP_SERVER_ADDR = "rtsp://127.0.0.1:8554/test";
    public static String RTMPUrl = "rtmp://127.0.0.1:1936/test";
    //Stream Mode
    public static int STREAM_RAW = 0;
    public static int STREAM_MIRROR = 1;

    public static void setControllerIpAddress(String ip) {
        CONTROLLER_IP_ADDRESS = ip;
    }
    public static void setScreenMirrorServerAddr(String ip) {
        SCREEN_MIRROR_RTSP_SERVER_ADDR = ip;
    }
    public static void setRTMPUrl(String url) { RTMPUrl = url; }

}
