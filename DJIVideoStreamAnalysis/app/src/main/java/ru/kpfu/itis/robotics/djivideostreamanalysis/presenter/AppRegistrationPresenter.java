package ru.kpfu.itis.robotics.djivideostreamanalysis.presenter;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import ru.kpfu.itis.robotics.djivideostreamanalysis.DJIApplication;
import ru.kpfu.itis.robotics.djivideostreamanalysis.view.AppRegistrationView;

/**
 * Created by valera071998@gmail.com on 25.04.2018.
 */
public class AppRegistrationPresenter {

    private static final String TAG = AppRegistrationPresenter.class.getName();

    private static final int REQUEST_PERMISSION_CODE = 12345;
    
    private AppRegistrationView view;
    private Activity activity;

    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDeviceStatus();
        }
    };
    
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    
    public AppRegistrationPresenter(AppRegistrationView view, Activity activity) {
        Log.d(TAG, "Creating with view: " + view + ", activity: " + activity);
        this.view = view;
        this.activity = activity;

        Log.d(TAG, "checkAndRequestPermissions().");
        checkAndRequestPermissions();

        Log.d(TAG, "Registering connection change receiver: " + connectionChangeReceiver);
        registerConnectionChangeReceiver();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            boolean isMissed =
                    ContextCompat.checkSelfPermission(activity, eachPermission) != PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Permission: " + eachPermission + " is missed: " + isMissed);
            if (isMissed) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Missing Permissions: " + missingPermission.toString());
            ActivityCompat.requestPermissions(activity,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void refreshDeviceStatus() {
        BaseProduct product = DJIApplication.getProductInstance();

        if (product != null && product.isConnected()) {
            Log.d(TAG,"Product: " + product + " is connected.");
            view.showProductConnectionSuccess(product.getModel().getDisplayName());
        } else {
            Log.d(TAG,"Product: " + product + " is not connected.");
            view.showProductConnectionError();
        }
    }

    public void checkForGrantedPermissions(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "callback - onRequestPermissionsResult().");

        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Log.d(TAG, "requestCode: " + requestCode);
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, grantResults[i] + " permission is granted.");
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            Log.d(TAG,"All required permissions are granted.");
            view.showToast("All permissions are granted.");
            startSDKRegistration();
        } else {
            Log.d(TAG,"Missed permissions: " + missingPermission.toString());
            view.showToast("Missing permissions.");
        }
    }

    // Register the broadcast receiver for receiving the device connection's changes.
    private void registerConnectionChangeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(connectionChangeReceiver, filter);
    }

    public void unregisterConnectionChangeReceiver() {
        Log.d(TAG, " unregisterConnectionChangeReceiver(). Receiver: " + connectionChangeReceiver);
        activity.unregisterReceiver(connectionChangeReceiver);
    }

    private void startSDKRegistration() {
        Log.d(TAG, "startSDKRegistration().");
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "startSDKRegistration(). asyncTask running...");
                    Log.d(TAG, "DJISDKManager.registerApp()");
                    DJISDKManager.getInstance().registerApp(activity, new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e(TAG,
                                        String.format("onRegister(). djiError %s: ", DJISDKError.REGISTRATION_SUCCESS.getDescription()));
                                view.showAppRegistrationSuccess();
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                view.showAppRegistrationError();
                            }
                            isRegistrationInProgress.set(false);
                        }

                        @Override
                        public void onProductDisconnect() {
                            view.showToast("Product disconnected.");
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            view.showToast("Product connected.");
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                            if (newComponent != null && oldComponent == null) {
                                Log.v(TAG,componentKey.name() + " Component Found index:" + newComponent.getIndex());
                            }
                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                                    @Override
                                    public void onConnectivityChange(boolean b) {
                                        Log.v(TAG," Component " + (b ? "connected" : "disconnected"));
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
    }
}