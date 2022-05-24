package com.dji.djiaapp2.logic;

import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_FREE;
import static com.dji.djiaapp2.utils.AppConfiguration.DRONE_MODE_SEARCH;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.dji.djiaapp2.models.Drone;
import com.dji.djiaapp2.utils.AppConfiguration;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * For handling anything related to waypoint missions,
 * from parsing of waypoint xml file to building and
 * uploading mission to drone
 */
public class WaypointMissionHandler {
    private static final String TAG = "Mission Handler";

    private Context app;
    private WaypointMissionOperator waypointMissionOperator;
    private DocumentFile wayPointFile;
    private ArrayList<Waypoint> waypoints = new ArrayList<>();
    private boolean hasUploaded = false;

    // Settings for waypoint mission behaviour
    private final WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private final WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private final float maxSpeed = 15.0f;

    public WaypointMissionHandler(Context context) {
        app = context;
        if (waypointMissionOperator == null) {
            MissionControl missionControl = DJISDKManager.getInstance().getMissionControl();
            if (missionControl != null) {
                waypointMissionOperator = missionControl.getWaypointMissionOperator();
            }
        }
    }

    public void parseWaypointFile(DocumentFile file) {
        wayPointFile = file;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            InputStream inputStream = app.getContentResolver().openInputStream(wayPointFile.getUri());
            xpp.setInput(new BufferedReader(new InputStreamReader(inputStream)));

            String tagValue = null, startTag = null, endTag = null;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        startTag = xpp.getName();
                        break;
                    case XmlPullParser.TEXT:
                        tagValue = xpp.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        endTag = xpp.getName();
                        if (endTag.equals("coordinates")) {
                            // Waypoint(lat, long, al, speed, radius)
                            String[] coordinates = tagValue.split((","));
                            if (coordinates.length > 4) {
                                Waypoint waypoint = new Waypoint(Double.parseDouble(coordinates[1]),
                                        Double.parseDouble(coordinates[0]),
                                        Float.parseFloat(coordinates[2]));
                                waypoint.speed = Float.parseFloat(coordinates[3]);
                                waypoint.cornerRadiusInMeters = Float.parseFloat(coordinates[4]);
                                waypoints.add(waypoint);
                            }
                        }
                        break;
                }
                if (startTag != null && startTag.equals("LineString")) {
                    break;
                } else {
                    eventType = xpp.next();
                }
            }
        }  catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadWaypointMission(Callback callback) {
        // Building waypoint mission
        if (waypointMissionOperator != null) {
            WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder();
            waypointMissionBuilder.waypointList(waypoints);
            waypointMissionBuilder.waypointCount(waypoints.size());

            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(AppConfiguration.MAX_SPEED)
                    .maxFlightSpeed(maxSpeed)
                    .setGimbalPitchRotationEnabled(false)
                    .flightPathMode(WaypointMissionFlightPathMode.CURVED);

            // Load and upload mission to drone
            DJIError error = waypointMissionOperator.loadMission(waypointMissionBuilder.build());
            if (error == null) {
                waypointMissionOperator.uploadMission(error1 -> {
                    if (error1 != null) {
                        Log.e(TAG, "Mission upload failed, error: " + error1.getDescription());
                        hasUploaded = false;
                        callback.onComplete();
                    } else {
                        hasUploaded = true;
                        callback.onComplete();
                    }
                });
            } else {
                Log.e(TAG, "loadWaypoint failed " + error.getDescription());
                hasUploaded = false;
                callback.onComplete();
            }
        }
    }

    public String getFilename() {
        if (wayPointFile != null) {
            return wayPointFile.getName();
        }
        return null;
    }

    public boolean hasUploaded() {
        return hasUploaded;
    }

    public void startWaypointMission(Callback callback) {
        waypointMissionOperator.startMission(error -> {
            if (error == null) {
                Drone.getInstance().setMode(DRONE_MODE_SEARCH);
                Drone.getInstance().setListeningToCommands(true);
            } else {
                Drone.getInstance().setMode(DRONE_MODE_FREE);
            }
            callback.onComplete();
            Log.e(TAG, "Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
        });
    }

    public void stopWaypointMission(int mode, Callback callback) {
        waypointMissionOperator.stopMission(error -> {
            if (error == null) {
                Drone.getInstance().setMode(mode);
                hasUploaded = false;
            } else {
                Drone.getInstance().setMode(DRONE_MODE_SEARCH);
            }
            callback.onComplete();
            Log.e(TAG, "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
        });
    }

    public WaypointMissionOperator getOperator() {
        return waypointMissionOperator;
    }

    public void cleanUp() {
        waypoints.clear();
        waypointMissionOperator.clearMission();
        wayPointFile = null;
        hasUploaded = false;
    }
}
