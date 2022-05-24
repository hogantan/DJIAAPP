package com.dji.djiaapp2.logic;

import static com.dji.djiaapp2.utils.AppConfiguration.RTSP_URL;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dji.djiaapp2.utils.AppConfiguration;
import com.pedro.encoder.video.FormatVideoEncoder;
import com.pedro.encoder.video.GetVideoData;
import com.pedro.encoder.video.VideoEncoder;
import com.pedro.rtplibrary.view.GlInterface;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtsp.rtsp.RtspClient;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.nio.ByteBuffer;

import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

/**
 * This is for using DJI inbuilt RTMP live streaming
 * Requires internet connection on Android device as DJI Livestream SDK
 * requires internet
 */
public class LiveStreamHandler {
    private static final String TAG = "LiveStreamHandler";
    private final LiveStreamManager liveStreamManager;

    private VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private RtspClient rtspClient;
    private VideoEncoder videoEncoder;
    private GlInterface glInterface;
    private DJICodecManager codecManager;

    public LiveStreamHandler() {
        liveStreamManager = DJISDKManager.getInstance().getLiveStreamManager();

        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (codecManager != null) {
                    codecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    public void init(Context context, OpenGlView openGlView) {
        initRTSP(context, openGlView);
        initRTMP();
    }

    private void initRTSP(Context context, OpenGlView openGlView) {
        rtspClient = new RtspClient(new ConnectCheckerRtsp() {
            @Override
            public void onConnectionStartedRtsp(@NonNull String s) {
                Log.e(TAG, "Connection success");
            }

            @Override
            public void onConnectionSuccessRtsp() {
                Log.e(TAG, "Connection success");
            }

            @Override
            public void onConnectionFailedRtsp(String reason) {
                Log.e(TAG, "Connection Failed");
            }

            @Override
            public void onNewBitrateRtsp(long bitrate) {
                Log.i("(GCS)RTSP Bitrate", String.valueOf(bitrate / 100) + " kbps");
            }

            @Override
            public void onDisconnectRtsp() {

            }

            @Override
            public void onAuthErrorRtsp() {

            }

            @Override
            public void onAuthSuccessRtsp() {

            }
        });

        glInterface = openGlView;
        glInterface.init();
        glInterface.setEncoderSize(AppConfiguration.width, AppConfiguration.height);
        glInterface.start();
        codecManager = new DJICodecManager(context, glInterface.getSurfaceTexture(),
                AppConfiguration.width, AppConfiguration.height);
    }

    private void initRTMP() {
        VideoFeeder.VideoFeed feed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        feed.addVideoDataListener(mReceivedVideoDataListener);
        liveStreamManager.setAudioStreamingEnabled(false);
        liveStreamManager.setVideoEncodingEnabled(false);
        liveStreamManager.setLiveVideoResolution(AppConfiguration.resolution);
        liveStreamManager.setLiveVideoBitRateMode(AppConfiguration.mode);
        liveStreamManager.setVideoSource(LiveStreamManager.LiveStreamVideoSource.Primary);
    }

    public void startRTMP() {
        if (isStreamingRTMP()) {
            stopRTMP();
        } else {
            if (AppConfiguration.RTMP_URL != "" && !liveStreamManager.isStreaming()) {
                liveStreamManager.setLiveUrl(AppConfiguration.RTMP_URL);
                liveStreamManager.startStream();
            }
        }
    }

    public void stopRTMP() {
        if (liveStreamManager.isStreaming()) {
            liveStreamManager.stopStream();
        }
    }

    public void startRTSP() {
        if (!rtspClient.isStreaming()) {
            videoEncoder = new VideoEncoder(new GetVideoData() {
                @Override
                public void onSpsPpsVps(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
                    if (!rtspClient.isStreaming()) {
                        rtspClient.setVideoInfo(sps, pps, vps);
                        rtspClient.setOnlyVideo(true);
                        rtspClient.setLogs(true);
                        rtspClient.connect(RTSP_URL);
                    }
                }

                @Override
                public void getVideoData(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
                    rtspClient.sendVideo(h264Buffer, info);
                }

                @Override
                public void onVideoFormat(MediaFormat mediaFormat) {

                }
            });

            videoEncoder.prepareVideoEncoder(AppConfiguration.width, AppConfiguration.height, AppConfiguration.fps,
                    AppConfiguration.bitrate, 0, AppConfiguration.iFrameInterval, FormatVideoEncoder.SURFACE);
            videoEncoder.start();
            glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
        } else {
            stopRTSP();
        }
    }

    public void stopRTSP() {
        if (glInterface != null) {
            glInterface.removeMediaCodecSurface();
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder = null;
            rtspClient.disconnect();
        }
    }

    public void cleanUp() {
        stopRTSP();
        stopRTMP();

        if (glInterface != null) {
            glInterface.stop();
            glInterface = null;
        }

        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
    }

    public boolean isStreamingRTMP() {
        return liveStreamManager.isStreaming();
    }

    public boolean isStreamingRTSP() {
        if (rtspClient != null) {
            return rtspClient.isStreaming();
        } else {
            return false;
        }
    }

    public int getRTMPBitrate() {
        return liveStreamManager.getLiveVideoBitRate();
    }

}
