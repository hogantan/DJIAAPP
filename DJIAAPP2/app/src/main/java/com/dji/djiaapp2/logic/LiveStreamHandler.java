package com.dji.djiaapp2.logic;

import static dji.sdk.sdkmanager.LiveVideoResolution.VIDEO_RESOLUTION_1920_1080;

import com.dji.djiaapp2.utils.AppConfiguration;

import dji.sdk.camera.VideoFeeder;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

/**
 * This is for using DJI inbuilt RTMP live streaming
 */
public class LiveStreamHandler {

    private LiveStreamManager liveStreamManager;

    public LiveStreamHandler(VideoFeeder.VideoDataListener listener) {
        liveStreamManager = DJISDKManager.getInstance().getLiveStreamManager();
        VideoFeeder.VideoFeed feed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        feed.addVideoDataListener(listener);
        liveStreamManager.setAudioStreamingEnabled(false);
        liveStreamManager.setVideoEncodingEnabled(false);
        DJISDKManager.getInstance().getProduct().getCamera().setHDLiveViewEnabled(true, djiError -> { });
        liveStreamManager.setLiveVideoResolution(VIDEO_RESOLUTION_1920_1080);
        liveStreamManager.setVideoSource(LiveStreamManager.LiveStreamVideoSource.Primary);
    }

    public void startStream() {
        if (AppConfiguration.RTMPUrl != "" && !liveStreamManager.isStreaming()) {
            liveStreamManager.setLiveUrl(AppConfiguration.RTMPUrl);
            liveStreamManager.startStream();
        } else {
            liveStreamManager.stopStream();
        }
    }

    public void stopStream() {
        if (liveStreamManager.isStreaming()) {
            liveStreamManager.stopStream();
        }
    }

}
