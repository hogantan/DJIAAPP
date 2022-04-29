package com.dji.djiaapp2.activities;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_SEARCH;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dji.djiaapp2.R;
import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.AppConfiguration;
import com.dji.djiaapp2.utils.OnScreenJoystick;
import com.dji.djiaapp2.viewmodels.VideoViewModel;
import com.dji.djiaapp2.screenmirror.DisplayService;
import com.pedro.rtsp.rtsp.RtspClient;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * For getting live video feed to show on application as well as
 * streaming it via RTSP
 * Provides virtual joystick controls to control drone movement
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoActivity extends AppCompatActivity
        implements ConnectCheckerRtsp, TextureView.SurfaceTextureListener {

    private static final String TAG = VideoActivity.class.getName();
    private final int REQUEST_CODE_STREAM = 179; //random num
    private final int REQUEST_CODE_RECORD = 180; //random num

    protected TextureView mVideoSurface = null;
    private ImageView mode;
    private ToggleButton chaseBtn;
    private Button settingsBtn;
    private Button landBtn;
    private Button mirrorScreenBtn;
    private Button startRTMPBtn;
    private ToggleButton toggleUI;
    private ToggleButton toggleLayoutBtn;
    private OnScreenJoystick joystickRight;
    private OnScreenJoystick joystickLeft;
    private ProgressDialog loadingBar;
    private TextView altitude;

    private VideoViewModel videoViewModel;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private RtspClient rtspClient;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
    private long presentTimeUs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        videoViewModel.init(getApplicationContext());
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_video);

        initUI();
        subscribeToViewModel();
        initRtspClient();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer,
                            size,UsbAccessoryService.VideoStreamSource.Camera.getIndex());
                }
            }
        };
        videoViewModel.initRTMP(mReceivedVideoDataListener);
    }

    private void initUI() {
        Bundle bundle = getIntent().getExtras();
        int currentMode = bundle.getInt("mode");

        mode = findViewById(R.id.mode);
        if (currentMode == (DRONE_MODE_SEARCH)) {
            mode.setImageResource(R.drawable.ic_baseline_location_on_24);
        } else {
            mode.setImageResource(R.drawable.ic_baseline_control_camera_24);
        }
        mVideoSurface = findViewById(R.id.primaryVideoFeed);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        chaseBtn = findViewById(R.id.chaseBtn);
        chaseBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                videoViewModel.startChase();
            } else {
                videoViewModel.stopChase();
            }
        });

        settingsBtn = findViewById(R.id.settingsBtn);
        popupSettings();

        joystickRight = findViewById(R.id.joystickRight);
        joystickLeft = findViewById(R.id.joystickLeft);

        joystickRight.setJoystickListener((joystick, pX, pY) -> {
            videoViewModel.stopMission();
            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            videoViewModel.moveRightStick(pX, pY);
        });

        joystickLeft.setJoystickListener((joystick, pX, pY) -> {
            videoViewModel.stopMission();
            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            videoViewModel.moveLeftStick(pX, pY);
        });

        landBtn = findViewById(R.id.land_btn);
        landBtn.setOnClickListener(view -> videoViewModel.startLand());

        toggleLayoutBtn = findViewById(R.id.toggleLayoutBtn);
        toggleLayout();

        toggleUI = findViewById(R.id.toggleUIBtn);
        toggleUI();

        startRTMPBtn = findViewById(R.id.startrtmp_btn);
        startRTMPBtn.setOnClickListener(view -> videoViewModel.startRTMP());

        loadingBar = new ProgressDialog(VideoActivity.this);

        altitude = findViewById(R.id.altitude);

        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
            initScreenMirror();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG,"onSurfaceTextureDestroyed");
        videoViewModel.cleanSurface();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Drone.getInstance().isFree()) {
                    onBackPressed();
                } else {
                    String msg;
                    String title;
                    if (Drone.getInstance().isOnMission()) {
                        msg = "Do you want to stop current waypoint mission?";
                        title = "Mission Executing";
                    } else {
                        msg = "Do you want to stop chasing?";
                        title = "Chasing Target";
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
                    builder.setMessage(msg).setTitle(title);
                    builder.setCancelable(false)
                            .setPositiveButton("Yes", (dialog, id) -> {
                                videoViewModel.stopMission();
                                videoViewModel.stopChase();

                                loadingBar.setTitle("Loading");
                                loadingBar.setMessage("Please wait..");
                                loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                loadingBar.setIndeterminate(true);
                                loadingBar.show();

                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        loadingBar.dismiss();
                                        onBackPressed();
                                    }
                                }, 2000);
                            })
                            .setNegativeButton("No", (dialog, id) -> dialog.cancel());
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void popupSettings() {
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View settings = LayoutInflater.from(VideoActivity.this).inflate(R.layout.settings, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(VideoActivity.this, R.style.AlertDialogCustom);
                alert.setCancelable(false);
                settings.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context
                                .INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(settings.getWindowToken(), 0);
                        return true;
                    }
                });
                final EditText controllerIP = settings.findViewById(R.id.controllerIP);
                final EditText screenMirrorAddr = settings.findViewById(R.id.screenMirroRtspServer);
                final EditText RTMPAddr = settings.findViewById(R.id.RTMPUrl);

                alert.setTitle("Settings");
                alert.setView(settings);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AppConfiguration.setControllerIpAddress(controllerIP.getText().toString());
                        AppConfiguration.setScreenMirrorServerAddr(screenMirrorAddr.getText().toString());
                        AppConfiguration.setRTMPUrl(RTMPAddr.getText().toString());
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // do nothing, no update values
                    }
                });
                alert.show();
            }
        });
    }

    private void toggleLayout() {
        toggleLayoutBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    landBtn.setVisibility(View.VISIBLE);
                    joystickRight.setVisibility(View.VISIBLE);
                    joystickLeft.setVisibility(View.VISIBLE);
                    chaseBtn.setVisibility(View.INVISIBLE);
                    settingsBtn.setVisibility(View.INVISIBLE);
                    mirrorScreenBtn.setVisibility(View.INVISIBLE);
                    landBtn.setVisibility(View.INVISIBLE);
                    startRTMPBtn.setVisibility(View.INVISIBLE);


                    joystickRight.setAlpha(0);
                    joystickLeft.setAlpha(0);
                    joystickRight.animate().alpha(1.0f).setDuration(1000).start();
                    joystickLeft.animate().alpha(1.0f).setDuration(1000).start();

                } else {
                    landBtn.setVisibility(View.INVISIBLE);
                    joystickRight.setVisibility(View.INVISIBLE);
                    joystickLeft.setVisibility(View.INVISIBLE);
                    chaseBtn.setVisibility(View.VISIBLE);
                    settingsBtn.setVisibility(View.VISIBLE);
                    mirrorScreenBtn.setVisibility(View.VISIBLE);
                    landBtn.setVisibility(View.VISIBLE);
                    startRTMPBtn.setVisibility(View.VISIBLE);

                    settingsBtn.setAlpha(0);
                    settingsBtn.animate().alpha(1.0f).setDuration(1000).start();
                }
            }
        });
    }

    private void toggleUI() {
        toggleUI.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    hideAllUI();
                } else {
                    showAllUI();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("reset", "true");
        setResult(RESULT_OK, intent);
        DisplayService displayService = videoViewModel.getDisplayService();
        if (displayService.isStreaming()) {
            displayService.stopStream();
        }
        videoViewModel.stopRTMP();
    }

    private void subscribeToViewModel() {
        videoViewModel.isOnMission.observe(this, i -> {
            if (i == DRONE_MODE_FREE) {
                mode.setImageResource(R.drawable.ic_baseline_control_camera_24);
                toggleLayoutBtn.setVisibility(View.VISIBLE);
            } else if (i == DRONE_MODE_SEARCH) {
                mode.setImageResource(R.drawable.ic_baseline_location_on_24);
                toggleLayoutBtn.setVisibility(View.INVISIBLE);
            } else {
                mode.setImageResource(R.drawable.ic_baseline_location_searching_24);
                toggleLayoutBtn.setVisibility(View.INVISIBLE);
            }
        });

        videoViewModel.currentAltitude.observe(this, i -> {
            altitude.setText("H: " + String.valueOf(i) + "m");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && (requestCode == REQUEST_CODE_STREAM
                || requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK)) {
            videoViewModel.startScreenMirror(AppConfiguration.SCREEN_MIRROR_RTSP_SERVER_ADDR
                    , resultCode, data, 1920, getOutputHeight());
        }
    }

    private void initScreenMirror() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mirrorScreenBtn = findViewById(R.id.screenmirror_btn);
        mirrorScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DisplayService displayService = videoViewModel.getDisplayService();
                if (displayService != null) {
                    if (!displayService.isStreaming()) {
                        startActivityForResult(displayService.sendIntent(), REQUEST_CODE_STREAM);
                    } else {
                        displayService.stopStream();
                    }
                }
            }
        });

        DisplayService displayService = videoViewModel.getDisplayService();
        if (displayService == null) {
            startService(new Intent(this, DisplayService.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DisplayService displayService = videoViewModel.getDisplayService();
        if (displayService != null && !displayService.isStreaming() && !displayService.isRecording()) {
            //stop service only if no streaming or recording
            stopService(new Intent(this, DisplayService.class));
        }
    }

    private void hideAllUI() {
        mode.setVisibility(View.INVISIBLE);
        chaseBtn.setVisibility(View.INVISIBLE);
        settingsBtn.setVisibility(View.INVISIBLE);
        landBtn.setVisibility(View.INVISIBLE);
        toggleLayoutBtn.setVisibility(View.INVISIBLE);
        mirrorScreenBtn.setVisibility(View.INVISIBLE);
        startRTMPBtn.setVisibility(View.INVISIBLE);
        toggleLayoutBtn.setVisibility(View.INVISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        hideSystemBars();
    }

    private void showAllUI() {
        mode.setVisibility(View.VISIBLE);
        chaseBtn.setVisibility(View.VISIBLE);
        settingsBtn.setVisibility(View.VISIBLE);
        landBtn.setVisibility(View.VISIBLE);
        toggleLayoutBtn.setVisibility(View.VISIBLE);
        mirrorScreenBtn.setVisibility(View.VISIBLE);
        startRTMPBtn.setVisibility(View.VISIBLE);
        toggleLayoutBtn.setVisibility(View.VISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
        showSystemBars();
    }

    private void showSystemBars() {
        View decorView = getWindow().getDecorView();
        // Show the status bar.
        decorView.setSystemUiVisibility(View.VISIBLE);
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private int getScreenWidth() {
        Point metrics = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(metrics);
        return metrics.x;
    }

    private int getScreenHeight() {
        Point metrics = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(metrics);
        return metrics.y;
    }

    private int getOutputHeight() {
        float ratio = (float) getScreenWidth()/getScreenHeight();
        return (int) (1920 / ratio);
    }

    private void initRtspClient() {
        rtspClient = new RtspClient(new ConnectCheckerRtsp() {
            @Override
            public void onConnectionStartedRtsp(@NonNull String s) {
                Log.e(TAG, "connection success");
            }

            @Override
            public void onConnectionSuccessRtsp() {
                Log.e(TAG, "connection success");
            }

            @Override
            public void onConnectionFailedRtsp(String reason) {

            }

            @Override
            public void onNewBitrateRtsp(long bitrate) {

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
    }

    @Override
    public void onAuthErrorRtsp() {
    }

    @Override
    public void onAuthSuccessRtsp() {
    }

    @Override
    public void onConnectionFailedRtsp(@NonNull String s) {
    }

    @Override
    public void onConnectionStartedRtsp(@NonNull String s) {
    }

    @Override
    public void onConnectionSuccessRtsp() {
    }

    @Override
    public void onDisconnectRtsp() {
    }

    @Override
    public void onNewBitrateRtsp(long l) {
    }


    // To Explore: Send raw video to RTSP server directly
    // Currently unable to get keyframes/missing pps
    /*
    private void startRawStream(byte[] videoBuffer, int size) {
        videoInfo.size = size;
        videoInfo.offset = 0;
        videoInfo.flags = MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
        videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
        int naluType = UtilsKt.getVideoStartCodeSize(ByteBuffer.wrap(videoBuffer));
        naluType = videoBuffer[naluType] & 0x1f;
        //First keyframe received and you start stream.
        // Change conditional as you want but stream must start with a keyframe
        if (naluType == 7 && !rtspClient.isStreaming()) {
            videoInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            Pair<ByteBuffer, ByteBuffer> videoData = decodeSpsPpsFromBuffer(videoBuffer, size);
            if (videoData != null) {
                presentTimeUs = System.nanoTime() / 1000;
                ByteBuffer newSps = videoData.first;
                ByteBuffer newPps = videoData.second;
                rtspClient.setVideoInfo(newSps, newPps, null);
                rtspClient.setOnlyVideo(true);
                rtspClient.connect("rtsp://10.255.252.78:8554/test");
            } else {
                Log.e(TAG, "Error to extract video data");
            }
        }
        ByteBuffer h264Buffer = ByteBuffer.wrap(videoBuffer);
        rtspClient.sendVideo(h264Buffer, videoInfo);
    }

    private Pair<ByteBuffer, ByteBuffer> decodeSpsPpsFromBuffer(byte[] csd, int length) {
        byte[] mSPS = null, mPPS = null;
        int i = 0;
        int spsIndex = -1;
        int ppsIndex = -1;
        while (i < length - 4) {
            if (csd[i] == 0 && csd[i + 1] == 0 && csd[i + 2] == 0 && csd[i + 3] == 1) {
                if (spsIndex == -1) {
                    spsIndex = i;
                } else {
                    ppsIndex = i;
                    break;
                }
            }
            i++;
        }
        if (spsIndex != -1 && ppsIndex != -1) {
            mSPS = new byte[ppsIndex];
            System.arraycopy(csd, spsIndex, mSPS, 0, ppsIndex);
            mPPS = new byte[length - ppsIndex];
            System.arraycopy(csd, ppsIndex, mPPS, 0, length - ppsIndex);
        }
        if (mSPS != null && mPPS != null) {
            return new Pair<>(ByteBuffer.wrap(mSPS), ByteBuffer.wrap(mPPS));
        }
        return null;
    }
     */
}