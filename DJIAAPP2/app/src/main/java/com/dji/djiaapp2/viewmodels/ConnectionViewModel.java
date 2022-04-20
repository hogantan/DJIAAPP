package com.dji.djiaapp2.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dji.djiaapp2.logic.Callback;
import com.dji.djiaapp2.logic.ConnectionHandler;
import com.dji.djiaapp2.models.Drone;

/**
 * ViewModel for the connection event when the application
 * is launched
 */
public class ConnectionViewModel extends AndroidViewModel {
    private ConnectionHandler connectionHandler;

    public MutableLiveData<Boolean> sdkStatus = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> productStatus = new MutableLiveData<>(false);
    public MutableLiveData<String> productModel = new MutableLiveData<>();

    public ConnectionViewModel(@NonNull Application application) {
        super(application);

        connectionHandler = new ConnectionHandler();
    }

    public void startRegistration(Context context) {
        connectionHandler.startSDKRegistration(new Callback() {
            @Override
            public void onComplete() {
                updateInfo();
            }
        }, context);
    }

    public void updateInfo() {
        Drone drone = Drone.getInstance();
        sdkStatus.postValue(connectionHandler.isSDKRegistered());
        productStatus.postValue(drone.isConnected());
        productModel.postValue(drone.getModel());
    }
}
