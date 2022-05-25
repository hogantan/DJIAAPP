package com.dji.djiaapp2.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dji.djiaapp2.logic.Callback;
import com.dji.djiaapp2.logic.SafetyHandler;
import com.dji.djiaapp2.logic.WaypointMissionHandler;

/**
 * ViewModel for home activity
 */
public class HomeViewModel extends AndroidViewModel {
    private WaypointMissionHandler waypointMissionHandler;
    private SafetyHandler safetyHandler;

    public MutableLiveData<String> selectedFile = new MutableLiveData<>();
    public MutableLiveData<Boolean> hasUploaded = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(Context context) {
        waypointMissionHandler = new WaypointMissionHandler(context);
        safetyHandler = new SafetyHandler();
    }

    public void uploadWaypointFile(Uri file, Context context) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, file);
        waypointMissionHandler.parseWaypointFile(documentFile);
        waypointMissionHandler.uploadWaypointMission(new Callback() {
            @Override
            public void onComplete() {
                hasUploaded.postValue(waypointMissionHandler.hasUploaded());
                selectedFile.postValue(waypointMissionHandler.getFilename());
            }
        });
    }

    public void cleanUp() {
        waypointMissionHandler.cleanUp();
        hasUploaded.postValue(waypointMissionHandler.hasUploaded());
        selectedFile.postValue(waypointMissionHandler.getFilename());
    }
}
