package com.dji.djiaapp2.viewmodels;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_CHASE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.GCS_TAG;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dji.djiaapp2.logic.GimbalHandler;
import com.dji.djiaapp2.logic.LiveStreamHandler;
import com.dji.djiaapp2.logic.VirtualControllerHandler;
import com.dji.djiaapp2.logic.WaypointMissionHandler;
import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.AppConfiguration;
import com.pedro.rtplibrary.view.OpenGlView;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;

/**
 * ViewModel for video activity
 * This is where the ZMQ listener logic is
 * as well as logging of key information
 */
public class VideoViewModel extends AndroidViewModel {
    
    private WaypointMissionHandler missionHandler;
    private VirtualControllerHandler virtualControllerHandler;
    private LiveStreamHandler liveStreamHandler;
    private GimbalHandler gimbalHandler;

    public MutableLiveData<Integer> currentMode = new MutableLiveData<Integer>();
    public MutableLiveData<Float> currentAltitude = new MutableLiveData<Float>();
    public MutableLiveData<Double> currentVelocity = new MutableLiveData<Double>();
    public MutableLiveData<Boolean> hasMission = new MutableLiveData<>();

    public VideoViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(Context context) {
        missionHandler = new WaypointMissionHandler(context);
        virtualControllerHandler = new VirtualControllerHandler();
        gimbalHandler = new GimbalHandler();
        liveStreamHandler = new LiveStreamHandler();

        currentMode.postValue(Drone.getInstance().getMode());
        listenMissionEnd();
        onUpdate();
    }

    public void moveLeftStick(float x, float y) {
        stopMission();
        virtualControllerHandler.moveLeftStick(x, y);
    }

    public void moveRightStick(float x, float y) {
        stopMission();
        virtualControllerHandler.moveRightStick(x, y);
    }

    public void move(float x, float y) {
        virtualControllerHandler.move(x, y);
    }

    public void stopMoving() { virtualControllerHandler.stopMoving();}

    private void listenMissionEnd() {
        if (missionHandler.getOperator() != null) {
            missionHandler.getOperator().addListener(new WaypointMissionOperatorListener() {
                @Override
                public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

                }

                @Override
                public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {

                }

                @Override
                public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

                }

                @Override
                public void onExecutionStart() {

                }

                @Override
                public void onExecutionFinish(@Nullable DJIError djiError) {
                    gimbalHandler.lookDown();
                    hasMission.postValue((missionHandler.hasUploaded()));
                    if (!Drone.getInstance().isChasing()) {
                        Drone.getInstance().setMode(DRONE_MODE_FREE);
                        currentMode.postValue(Drone.getInstance().getMode());
                    }
                }
            });
        }
    }

    public void startMission() {
        if (!Drone.getInstance().isOnMission()) {
            missionHandler.startWaypointMission(() -> currentMode.postValue(Drone.getInstance().getMode()));
        } else {
            stopMission();
        }
    }

    public void stopMission() {
        if (Drone.getInstance().isOnMission()) {
            missionHandler.stopWaypointMission(DRONE_MODE_FREE, () -> {
                currentMode.postValue(Drone.getInstance().getMode());
                hasMission.postValue((missionHandler.hasUploaded()));
            });
            gimbalHandler.lookDown();
        }
    }

    public void startChase() {
        gimbalHandler.lookDown();
        if (Drone.getInstance().isOnMission()) {
            missionHandler.stopWaypointMission(DRONE_MODE_CHASE, () -> currentMode.postValue(Drone.getInstance().getMode()));
            hasMission.postValue((missionHandler.hasUploaded()));
            gimbalHandler.lookDown();
        }
        Drone.getInstance().setMode(DRONE_MODE_CHASE);
        currentMode.postValue(Drone.getInstance().getMode());
        Drone.getInstance().setListeningToCommands(true);
    }

    public void startFree() {
        gimbalHandler.lookDown();
        if (Drone.getInstance().isOnMission()) {
            missionHandler.stopWaypointMission(DRONE_MODE_CHASE, () -> currentMode.postValue(Drone.getInstance().getMode()));
            hasMission.postValue((missionHandler.hasUploaded()));
            gimbalHandler.lookDown();
        }
        Drone.getInstance().setMode(DRONE_MODE_FREE);
        Drone.getInstance().setListeningToCommands(false);
        currentMode.postValue(Drone.getInstance().getMode());
        virtualControllerHandler.stopMoving();
    }

    public void startLand() { virtualControllerHandler.startLand(); }

    public void startTakeoff() {virtualControllerHandler.startTakeoff();}

    // For chase mode to listen to message queue for vX vY controller values
    // Only single instance as ZeroMQ does not support multithreading
    // Therefore, use a flag to decide whether to listen to commands and move
    public void initCommandReceiver() {
        if (Drone.getInstance().isOnMission()) {
            Drone.getInstance().setListeningToCommands(true);
        }

        if (!AppConfiguration.CONTROLLER_IP_ADDRESS.isEmpty()) {
            Thread zmqThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.e("Command Receiver", "Listening to Commands");
                    try (ZContext context = new ZContext()) {
                        // Socket to talk to clients
                        ZMQ.Socket socket = context.createSocket(SocketType.PULL);
                        socket.connect("tcp://" + AppConfiguration.CONTROLLER_IP_ADDRESS + ":5555");
                        Log.e("ZeroMQ", "Command Listener Opened");
                        while (!Thread.currentThread().isInterrupted()) {
                            String message = new String(socket.recv(0));
                            // Movement command
                            if (Drone.getInstance().isListeningToCommands()) {
                                if (message.split(",").length > 1) {
                                    String vX = message.split(",")[0];
                                    String vY = message.split(",")[1];
                                    Log.i("ZeroMQ", "vX:" + vX + " vY: " + vY);
                                    if (Drone.getInstance().isOnMission()) {
                                        startChase();
                                    }
                                    if (Drone.getInstance().isChasing()) {
                                        move(Float.parseFloat(vX), Float.parseFloat(vY));
                                    } else {
                                        stopMoving();
                                    }
                                }
                            }
                        }

                        socket.close();
                    }
                    catch (Exception error) {
                        Log.e("ZeroMQ", error.getMessage());
                    }
                }
            });
            zmqThread.start();
        } else {
            Log.e("ZeroMQ", "Invalid Host IP Address");
        }
    }

    public void initLiveVideoFeed(Context context, OpenGlView openGlView) {
        liveStreamHandler.init(context, openGlView);
    }

    public void startRTMP() {
        liveStreamHandler.startRTMP();
    }

    public void stopRTMP() {
        liveStreamHandler.stopRTMP();
    }

    public void startRTSP() {
        liveStreamHandler.startRTSP();
    }

    public void stopRTSP() {
        liveStreamHandler.stopRTSP();
    }

    public void cleanUp() {
        liveStreamHandler.cleanUp();
    }

    // Logging of key information
    public void onUpdate() {
        virtualControllerHandler.onUpdate(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                //Telemetry
                LocationCoordinate3D location = flightControllerState.getAircraftLocation();
                currentAltitude.postValue(location.getAltitude());
                double lat = location.getLatitude();
                double longi = location.getLongitude();
                float alt = location.getAltitude();
                Log.i(GCS_TAG, "Telemetry " + lat + " " + longi + " " + alt);

                // Bitrate
                if (liveStreamHandler != null) {
                    if (liveStreamHandler.isStreamingRTMP()) {
                        Log.i(GCS_TAG, "RTMP Bitrate " + String.valueOf(liveStreamHandler.getRTMPBitrate()) + " kbps");
                    }
                }

                // Current Speed
                float vX = flightControllerState.getVelocityX();
                float vY = flightControllerState.getVelocityY();
                float vZ = flightControllerState.getVelocityZ();
                double v = Math.round(Math.sqrt((vX * vX) + (vY * vY) + (vZ * vZ)) * 100.00) / 100.00;

                currentVelocity.postValue((v));
                Log.i(GCS_TAG, "Drone Speed " + String.valueOf(v));
            }
        });
    }
}
