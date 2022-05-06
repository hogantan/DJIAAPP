package com.dji.djiaapp2.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.transition.Fade;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dji.djiaapp2.R;
import com.dji.djiaapp2.viewmodels.ConnectionViewModel;

import java.util.ArrayList;
import java.util.List;

public class ConnectionActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private ImageView sdkStatus;
    private ImageView connectionStatus;
    private TextView droneModel;
    private ProgressBar loadingBar;
    private ConnectionViewModel connectionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        super.onCreate(savedInstanceState);
        connectionViewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setExitTransition(new Fade());
        }

        setContentView(R.layout.activity_connection);
        initUI();
        subscribeToViewModel();
        connectionViewModel.startRegistration(getApplicationContext());
    }

    private void initUI() {
        sdkStatus = findViewById(R.id.sdkStatus);
        connectionStatus = findViewById(R.id.connectionStatus);
        droneModel = findViewById(R.id.drone);
        loadingBar = findViewById(R.id.loadingBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            connectionViewModel.startRegistration(getApplicationContext());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            connectionViewModel.startRegistration(getApplicationContext());
        }
    }

    private void launchApp() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                finish();
            }
        }, 3000);
    }

    private void subscribeToViewModel() {
        connectionViewModel.sdkStatus.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isTrue) {
                if (isTrue) {
                    sdkStatus.setImageResource(R.drawable.ic_baseline_check_circle_24);
                } else {
                    sdkStatus.setImageResource(R.drawable.ic_baseline_cancel_24);
                }
            }
        });

        connectionViewModel.productStatus.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isTrue) {
                if (isTrue) {
                    connectionStatus.setImageResource(R.drawable.ic_baseline_check_circle_24);
                } else {
                    connectionStatus.setImageResource(R.drawable.ic_baseline_cancel_24);
                }
            }
        });

        connectionViewModel.productModel.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (s != null) {
                    droneModel.setText(s);
                    droneModel.setTextColor(getResources().getColor(R.color.colorGreen));
                    loadingBar.setVisibility(View.INVISIBLE);
                    launchApp();
                } else {
                    droneModel.setTextColor(getResources().getColor(R.color.colorRed));
                }
            }
        });
    }
}