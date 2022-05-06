package com.dji.djiaapp2.activities;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_SEARCH;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dji.djiaapp2.R;
import com.dji.djiaapp2.viewmodels.HomeViewModel;

/**
 * For uploading and loading waypoint xml file to drone
 * Parses waypoint xml file and loads into drone
 * Can either start a waypoint mission or just jump to live camera view (without starting any mission)
 */
public class HomeActivity extends AppCompatActivity {

    private static final int IMPORT_FILE_CODE = 1234;
    private static final int RESET_CODE = 5678;

    private ImageView uploadBtn;
    private TextView filename;
    private Button startMissionBtn;
    private Button startWithoutMissionBtn;
    private ProgressDialog loadingBar;

    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.init(getApplicationContext());

        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
        }

        setContentView(R.layout.activity_home);
        initUI();
        subscribeToViewModel();
    }

    private void initUI() {
        uploadBtn = findViewById(R.id.uploadBtn);
        uploadBtn.setOnClickListener(view -> openFile());
        filename = findViewById(R.id.waypointFilename);

        startMissionBtn = findViewById(R.id.startMission);
        startMissionBtn.setOnClickListener(view -> {
            homeViewModel.startMission();
            initStartingBar();
        });
        startMissionBtn.setAlpha(.5f);
        startMissionBtn.setEnabled(false);

        startWithoutMissionBtn = findViewById(R.id.startWithoutMission);
        startWithoutMissionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, VideoActivity.class);
            intent.putExtra("mode", DRONE_MODE_FREE);
            startActivityForResult(intent, RESET_CODE);
        });

        loadingBar = new ProgressDialog(HomeActivity.this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    // For file browsing on android phone
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");

        startActivityForResult(intent, IMPORT_FILE_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // When file has been selected
            case IMPORT_FILE_CODE:
                if (resultCode == Activity.RESULT_OK ) {
                    if(data != null)  {
                        homeViewModel.uploadWaypointFile(data.getData(), getApplicationContext());
                        initUploadingBar();
                    }
                }
                break;

            // When coming back from video activity
            case RESET_CODE:
                homeViewModel.resetMission();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Loading animation when uploading waypoint file
    private void initUploadingBar() {
        loadingBar.setTitle("Uploading Mission");
        loadingBar.setMessage("Please wait uploading waypoint mission...");
        loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingBar.setIndeterminate(true);
        loadingBar.show();
    }

    // Loading animation when starting mission (to buffer time before starting next activity)
    private void initStartingBar() {
        loadingBar.setTitle("Starting Mission");
        loadingBar.setMessage("Please wait starting waypoint mission...");
        loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingBar.setIndeterminate(true);
        loadingBar.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                loadingBar.dismiss();

                Intent intent = new Intent(HomeActivity.this, VideoActivity.class);
                intent.putExtra("mode", DRONE_MODE_SEARCH);
                startActivityForResult(intent, RESET_CODE);
            }
        }, 2000);
    }

    private void subscribeToViewModel() {
        homeViewModel.selectedFile.observe(this, s -> {
            if (s != null) {
                filename.setText(s);
            } else {
                filename.setText("UPLOAD WAYPOINT FILE");
            }
        });

        homeViewModel.hasUploaded.observe(this, isTrue -> {
            startMissionBtn.setEnabled(isTrue);
            startMissionBtn.setClickable(isTrue);
            loadingBar.dismiss();
            if (isTrue) {
                startMissionBtn.setAlpha(1.0f);
            } else {
                startMissionBtn.setAlpha(.5f);
            }
        });
    }
}