package ru.kpfu.itis.robotics.dji_tracking;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJIApplication extends Application {

    public static final String TAG = DJIApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "dji_mobile_sdk_test_connection_change";

    private Application instance;
    public Handler handler;

    private static BaseProduct product;

    private DJISDKManager.SDKManagerCallback DJISDKManagerCallback;
    private BaseProduct.BaseProductListener DJIBaseProductListener;
    private BaseComponent.ComponentListener DJIComponentListener;

    private static boolean isRegistered;

    public DJIApplication() { }

    @Override
    public void onCreate() {
        Log.d(TAG, "DJIApplication.onCreate()");
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        isRegistered = false;

        DJIComponentListener = new BaseComponent.ComponentListener() {

            @Override
            public void onConnectivityChange(boolean isConnected) {
                Log.d(TAG, "DJIComponentListener.onConnectivityChange() " + isConnected);
                notifyStatusChange();
            }

        };

        DJIBaseProductListener = new BaseProduct.BaseProductListener() {
            @Override
            public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
                Log.d(TAG, "DJIBaseProductListener.onComponentChange() " + key.name() +
                        ". Old component: " + oldComponent +
                        "; New component: " + newComponent);
                newComponent.setComponentListener(DJIComponentListener);
                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean isConnected) {
                Log.d(TAG, "DJIBaseProductListener.onConnectivityChange() " + isConnected);
                notifyStatusChange();
            }
        };

        /**
         * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
         * the SDK Registration result and the product changing.
         */
        DJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

            //Listens to the SDK registration result
            @Override
            public void onRegister(DJIError error) {
                Log.d(TAG, "DJISDKManagerCallback.onRegister() " + error.getDescription());
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.d(TAG, "DJISDKManager.startConnectionToProduct()");

                    isRegistered = true;
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            //Listens to the connected product changing, including two parts, component changing or product connection changing.
            @Override
            public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                Log.d(TAG, "DJISDKManagerCallback.onProductChange() " +
                        ". Old product: " + oldProduct +
                        "; New product: " + newProduct);
                product = newProduct;
                if (product != null) {
                    product.setBaseProductListener(DJIBaseProductListener);
                }
                notifyStatusChange();
            }
        };

        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            Log.i(TAG, "SDK Version less than M(23): " + (Build.VERSION.SDK_INT < Build.VERSION_CODES.M));
            Log.i(TAG, "Persmissions WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE are available: "
                    + (permissionCheck == 0 && permissionCheck2 == 0));
            //This is used to start SDK services and initiate SDK.
            Log.d(TAG, "DJISDKManager.registerApp() starting... " +
                    "with application context " + getApplicationContext() +
                    " and DJISDKManagerCallback " + DJISDKManagerCallback.toString());

            DJISDKManager.getInstance().registerApp(getApplicationContext(), DJISDKManagerCallback);

            Toast.makeText(getApplicationContext(), "Regestring, please wait...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public void setContext(Application application) {
        instance = application;
    }

    public static boolean isRegistered() {
        return isRegistered;
    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized BaseProduct getProductInstance() {
        if (product == null) {
            product = DJISDKManager.getInstance().getProduct();
        }
        Log.d(TAG, "DJIApplication.getProductInstance(): " + product);
        return product;
    }

    public static synchronized FlightController getFlightController() {
        FlightController flightController = null;
        if (getProductInstance() instanceof Aircraft) {
            flightController = ((Aircraft) getProductInstance()).getFlightController();
        }

        Log.d(TAG, "DJIApplication.getFlightController(): " + flightController);
        return flightController;
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null)
            return null;

        Camera camera = null;
        if (getProductInstance() instanceof Aircraft) {
            camera = ((Aircraft) getProductInstance()).getCamera();
        }

        Log.d(TAG, "DJIApplication.getCameraInstance(): " + camera);
        return camera;
    }

    public static boolean isAircraftConnected() {
        boolean isAircraftConnected = getProductInstance() != null && getProductInstance() instanceof Aircraft;

        Log.d(TAG, "DJIApplication.isAircraftConnected(): " + isAircraftConnected);
        return isAircraftConnected;
    }

    private void notifyStatusChange() {
        Log.d(TAG, "DJIApplication.notifyStatusChange().");
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "DJIApplication.updateRunnable.run()");
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };
}