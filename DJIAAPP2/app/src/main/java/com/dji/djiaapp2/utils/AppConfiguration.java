package com.dji.djiaapp2.utils;

/*
 *  Global app configurations
 */
public class AppConfiguration {
    public static int APP_TCP_PORT = -1;
    public static int APP_RTSP_VIDEO_PORT = 8554;
    public static String APP_SERVER_IP = "";
    public static int APP_SERVER_UDP_PORT = -1;
    public static int APP_SERVER_TCP_PORT = -1;
    // If phantom 3 --> true , else --> false
    public static boolean APP_RTSP_VIDEO_ENABLE_TRANSCODING = true;
    public static int APP_RTSP_VIDEO_BIT_RATE_BITS_PER_SEC= 1500000;
    public static int APP_RTSP_VIDEO_WIDTH= 1280;
    public static int APP_RTSP_VIDEO_HEIGHT= 720;
    public static int APP_RTSP_VIDEO_IFRAME_INTERVAL_SEC = 1;
    // Drone mode
    public static int DRONE_MODE_FREE = 0;
    public static int DRONE_MODE_SEARCH = 1;
    public static int DRONE_MODE_CHASE = 2;
    // Controller App settings
    public static String CONTROLLER_IP_ADDRESS = "127.0.0.1";
    public static String SCREEN_MIRROR_RTSP_SERVER_ADDR = "rtsp://127.0.0.1:8554/test";
    public static String RTMPUrl = "";
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
