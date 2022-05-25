package com.dji.djiaapp2.activities;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_SEARCH;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dji.djiaapp2.R;
import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.OnScreenJoystick;
import com.dji.djiaapp2.viewmodels.VideoViewModel;
import com.pedro.rtplibrary.view.OpenGlView;

/**
 * For getting live video feed to show on application as well as
 * streaming it via RTSP / RTMP
 * Provides virtual controls to control drone movement and
 * a command listener to listen to a server for commands (for autonomous moving)
 * Start waypoint mission that has been uploaded in HomeActivity
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private ImageView mode;
    private ToggleButton commandListener;
    private Button landBtn;
    private Button takeoffBtn;
    private ToggleButton startRTSPBtn;
    private ToggleButton startRTMPBtn;
    private ToggleButton startMissionBtn;
    private ToggleButton toggleLayoutBtn;
    private OnScreenJoystick joystickRight;
    private OnScreenJoystick joystickLeft;
    private ProgressDialog loadingBar;
    private TextView altitude;
    private TextView velocity;

    private VideoViewModel videoViewModel;
    private OpenGlView openGlView;

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
        videoViewModel.initCommandReceiver();
    }

    private void initUI() {
        mode = findViewById(R.id.mode);

        commandListener = findViewById(R.id.chaseBtn);
        commandListener.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                videoViewModel.startChase();
            } else {
                videoViewModel.startFree();
            }
        });

        joystickRight = findViewById(R.id.joystickRight);
        joystickLeft = findViewById(R.id.joystickLeft);

        joystickRight.setJoystickListener((joystick, pX, pY) -> {
            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            videoViewModel.moveRightStick(pX, pY);
        });

        joystickLeft.setJoystickListener((joystick, pX, pY) -> {
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

        takeoffBtn = findViewById(R.id.takeoff_btn);
        takeoffBtn.setOnClickListener(view -> videoViewModel.startTakeoff());

        toggleLayoutBtn = findViewById(R.id.toggleLayoutBtn);
        toggleLayout();

        startRTMPBtn = findViewById(R.id.rtmp_btn);
        startRTMPBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                videoViewModel.startRTMP();
            } else {
                videoViewModel.stopRTMP();
            }
        });

        startRTSPBtn = findViewById(R.id.rtsp_btn);
        startRTSPBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                videoViewModel.startRTSP();
            } else {
                videoViewModel.stopRTSP();
            }
        });

        startMissionBtn = findViewById(R.id.startmission_btn);
        startMissionBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                videoViewModel.startMission();
            } else {
                videoViewModel.stopMission();
            }
        });

        loadingBar = new ProgressDialog(VideoActivity.this);

        altitude = findViewById(R.id.altitude);
        velocity = findViewById(R.id.velocity);
        openGlView = findViewById(R.id.liveVideoFeed);
        openGlView.getHolder().addCallback(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (getIntent().getExtras().getBoolean("hasUploaded")) {
            startMissionBtn.setEnabled(true);
            startMissionBtn.setVisibility(View.VISIBLE);
        } else {
            startMissionBtn.setEnabled(false);
            startMissionBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void toggleLayout() {
        toggleLayoutBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    landBtn.setVisibility(View.VISIBLE);
                    takeoffBtn.setVisibility(View.VISIBLE);
                    joystickRight.setVisibility(View.VISIBLE);
                    joystickLeft.setVisibility(View.VISIBLE);
                    commandListener.setVisibility(View.INVISIBLE);
                    startRTSPBtn.setVisibility(View.INVISIBLE);
                    startRTMPBtn.setVisibility(View.INVISIBLE);
                    startMissionBtn.setVisibility(View.INVISIBLE);

                } else {
                    landBtn.setVisibility(View.INVISIBLE);
                    takeoffBtn.setVisibility(View.INVISIBLE);
                    joystickRight.setVisibility(View.INVISIBLE);
                    joystickLeft.setVisibility(View.INVISIBLE);
                    commandListener.setVisibility(View.VISIBLE);
                    startRTSPBtn.setVisibility(View.VISIBLE);
                    startRTMPBtn.setVisibility(View.VISIBLE);
                    if (startMissionBtn.isEnabled()) {
                        startMissionBtn.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void subscribeToViewModel() {
        videoViewModel.currentMode.observe(this, i -> {
            if (i == DRONE_MODE_FREE) {
                mode.setImageResource(R.drawable.ic_baseline_control_camera_24);
                toggleLayoutBtn.setVisibility(View.VISIBLE);
                startMissionBtn.setChecked(false);
            } else if (i == DRONE_MODE_SEARCH) {
                mode.setImageResource(R.drawable.ic_baseline_location_on_24);
                toggleLayoutBtn.setVisibility(View.VISIBLE);
            } else {
                mode.setImageResource(R.drawable.ic_baseline_location_searching_24);
                commandListener.setChecked(true);
                toggleLayoutBtn.setVisibility(View.INVISIBLE);
                startMissionBtn.setChecked(false);
            }
        });

        videoViewModel.currentAltitude.observe(this, i -> {
            altitude.setText("H: " + String.valueOf(i) + "m");
        });

        videoViewModel.currentVelocity.observe(this, i -> {
            velocity.setText("V: " + String.valueOf(i) + "ms");
        });

        videoViewModel.hasMission.observe(this, i -> {
            if (i) {
                startMissionBtn.setEnabled(true);
                startMissionBtn.setVisibility(View.VISIBLE);
            } else {
                startMissionBtn.setEnabled(false);
                startMissionBtn.setVisibility(View.INVISIBLE);
            }
        });
    }

    // This is called when returning from HomeActivity (i.e. when VideoActivity is reopened)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        startRTSPBtn.setChecked(false);
        startRTMPBtn.setChecked(false);
        startMissionBtn.setChecked(false);
        commandListener.setChecked(false);
        toggleLayoutBtn.setChecked(false);

        if (intent.getExtras().getBoolean("hasUploaded")) {
            startMissionBtn.setEnabled(true);
            startMissionBtn.setVisibility(View.VISIBLE);
        } else {
            startMissionBtn.setEnabled(false);
            startMissionBtn.setVisibility(View.INVISIBLE);
        }
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
                                videoViewModel.startFree();

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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        videoViewModel.cleanUp();
        Drone.getInstance().setMode(DRONE_MODE_FREE);
        Drone.getInstance().setListeningToCommands(false);
        startActivity(intent);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        videoViewModel.initLiveVideoFeed(this, openGlView);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        videoViewModel.cleanUp();
    }
}