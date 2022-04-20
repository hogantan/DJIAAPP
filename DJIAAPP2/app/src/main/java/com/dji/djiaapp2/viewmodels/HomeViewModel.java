package com.dji.djiaapp2.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dji.djiaapp2.logic.GimbalHandler;
import com.dji.djiaapp2.logic.WaypointMissionHandler;

public class HomeViewModel extends AndroidViewModel {
    private WaypointMissionHandler waypointMissionHandler;
    private GimbalHandler gimbalHandler;

    public MutableLiveData<String> selectedFile = new MutableLiveData<>();
    public MutableLiveData<Boolean> hasUploaded = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(Context context) {
        waypointMissionHandler = new WaypointMissionHandler(context);
        gimbalHandler = new GimbalHandler();
    }

    public void uploadWaypointFile(Uri file, Context context) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, file);
        waypointMissionHandler.parseWaypointFile(documentFile);
        selectedFile.postValue(waypointMissionHandler.getFilename());
        waypointMissionHandler.uploadWaypointMission(() -> hasUploaded.postValue(waypointMissionHandler.hasUploaded()));
    }

    public void startMission() {
        waypointMissionHandler.startWaypointMission();
    }

    public void resetMission() {
        waypointMissionHandler.reset();
        selectedFile.postValue(waypointMissionHandler.getFilename());
        hasUploaded.postValue(waypointMissionHandler.hasUploaded());
    }
}
