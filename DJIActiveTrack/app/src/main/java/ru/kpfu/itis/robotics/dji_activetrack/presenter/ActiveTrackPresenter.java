package ru.kpfu.itis.robotics.dji_activetrack.presenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.mission.activetrack.ActiveTrackMode;
import dji.common.mission.activetrack.ActiveTrackState;
import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.common.mission.activetrack.ActiveTrackTrackingState;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.sdkmanager.DJISDKManager;
import ru.kpfu.itis.robotics.dji_activetrack.DJIApplication;
import ru.kpfu.itis.robotics.dji_activetrack.R;
import ru.kpfu.itis.robotics.dji_activetrack.util.StringUtil;
import ru.kpfu.itis.robotics.dji_activetrack.view.ActiveTrackView;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public class ActiveTrackPresenter implements TextureView.SurfaceTextureListener, ActiveTrackMissionOperatorListener {

    public static final String TAG = ActiveTrackPresenter.class.getName();

    private ActiveTrackView view;
    private Activity activity;

    private BaseProduct product;
    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            view.updateConnectionStatus();
            onProductChange();
        }
    };

    // Video stuff
    private TextureView videoSurface;
    private VideoFeeder.VideoDataCallback videoDataCallback;
    private DJICodecManager djiCodecManager = null;

    private ActiveTrackMission activeTrackMission;

    private StringUtil stringUtil;

    private LocationCoordinate3D currentLocation;

    public ActiveTrackPresenter(ActiveTrackView view, Activity activity) {
        Log.d(TAG, "ActiveTrackPresenter(). view: " + view + ", activity: " + activity);
        this.view = view;
        this.activity = activity;
        this.stringUtil = new StringUtil(activity);

        registerConnectionChangeReceiver();

        // init video stuff
        initVideoStuff();
        // init flight controller to get aircraft location
        initFlightController();
    }

    private void initVideoStuff() {
        videoSurface = view.getVideoSurface();
        if (videoSurface != null) {
            videoSurface.setSurfaceTextureListener(this);
        }

        // The callback for receiving the raw H264 video data for camera live view
        // https://developer.dji.com/api-reference/android-api/Components/Camera/DJICamera.html?search=videodatacall&i=1&#djicamera_camerareceivedvideodatacallbackinterface_inline
        videoDataCallback = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                Log.d(TAG, "VideoFeeder.VideoDataCallback.onReceive(). " +
                        "videoBuffer: " + videoBuffer + ", size: " + size);

                if (djiCodecManager != null) {
                    djiCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    private void registerConnectionChangeReceiver() {
        Log.d(TAG, "registerConnectionChangeReceiver(). connectionChangeReceiver : " + connectionChangeReceiver);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(connectionChangeReceiver, filter);
    }

    // Adjust FlightController to get aircraft state
    private void initFlightController() {
        Log.d(TAG, "initFlightController().");

        FlightController flightController = DJIApplication.getFlightController();
        if (flightController != null) {
            // set flight controller state listener
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    Log.d(TAG, "FlightController.onUpdate(). flightControllerState: " + flightControllerState);
                    currentLocation = flightControllerState.getAircraftLocation();

                    // update UI
                    view.updateAircraftLocation(
                            currentLocation.getLongitude(),
                            currentLocation.getLatitude(),
                            currentLocation.getAltitude()
                    );
                }
            });
        }
    }

    private void onProductChange() {
        Log.d(TAG, "onProductChange().");
        initVideoSurface();
    }

    private void initVideoSurface() {
        Log.d(TAG, "initVideoSurface().");
        try {
            product = DJIApplication.getProductInstance();
        } catch (Exception exception) {
            product = null;
        }

        if (product == null || !product.isConnected()) {
            Log.d(TAG, "Product :" + product + " is disconnected.");
        } else {
            if (null != videoSurface) {
                videoSurface.setSurfaceTextureListener(this);
            }

            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(videoDataCallback);
            }
        }
    }

    private void uninitVideoSurface() {
        Log.d(TAG, "uninitVideoSurface().");
        if (DJIApplication.getCameraInstance() != null) {
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }

    public void destroy() {
        uninitVideoSurface();
        if (djiCodecManager != null) {
            djiCodecManager.destroyCodec();
        }
    }

//------------------------------------ Video Surface listener ------------------------------------//
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (djiCodecManager == null) {  // attach DJICodecManager to surface
            djiCodecManager = new DJICodecManager(
                    activity,
                    surfaceTexture,
                    width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (djiCodecManager != null) {  // clean the surface from DJICodecManager
            djiCodecManager.cleanSurface();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

//--------------------------------------- Active Track stuff -------------------------------------//
    @Nullable
    private ActiveTrackOperator getActiveTrackOperator() {
        DJISDKManager djiSDKManager = DJISDKManager.getInstance();
        if (djiSDKManager != null) {
            MissionControl missionControl = djiSDKManager.getMissionControl();
            if (missionControl != null) {
                return missionControl.getActiveTrackOperator();
            }
        }
        return null;
    }

    // ActiveTrackMissionOperatorListener implementation
    @Override
    public void onUpdate(ActiveTrackMissionEvent event) {
        if (event != null) {
            StringBuffer sb = new StringBuffer();
            String errorInformation = (event.getError() == null ? "null" : event.getError().getDescription()) + "\n";
            String currentState = event.getCurrentState() == null ? "null" : event.getCurrentState().getName();
            String previousState = event.getPreviousState() == null ? "null" : event.getPreviousState().getName();

            ActiveTrackTargetState targetState = ActiveTrackTargetState.UNKNOWN;
            if (event.getTrackingState() != null) {
                targetState = event.getTrackingState().getState();
            }
            stringUtil.addLine(sb, stringUtil.getStringByResId(R.string.info_active_track_state_current), currentState);
            stringUtil.addLine(sb, stringUtil.getStringByResId(R.string.info_active_track_state_previous), previousState);
            stringUtil.addLine(sb, stringUtil.getStringByResId(R.string.info_active_track_state_target), targetState);
            stringUtil.addLine(sb, "Error:", errorInformation);

            ActiveTrackTrackingState trackingState = event.getTrackingState();
            if (trackingState != null) {
                RectF trackingRect = trackingState.getTargetRect();
                if (trackingRect != null) {
                    stringUtil.addLine(sb, "Rect center x: ", trackingRect.centerX());
                    stringUtil.addLine(sb, "Rect center y: ", trackingRect.centerY());
                    stringUtil.addLine(sb, "Rect Width: ", trackingRect.width());
                    stringUtil.addLine(sb, "Rect Height: ", trackingRect.height());
                    stringUtil.addLine(sb, "Reason: ", trackingState.getReason().name());
                    stringUtil.addLine(sb, "Target Index: ", trackingState.getTargetIndex());
                    stringUtil.addLine(sb, "Target Type: ", trackingState.getType().name());
                    stringUtil.addLine(sb, "Target State: ", trackingState.getState().name());

                    // ActiveTrack info updating
                    view.updateActiveTrackMissionState(sb.toString());
                }
            }

            // ActiveTrack target rect update
            view.updateActiveTrackRect(event);

            ActiveTrackState state = event.getCurrentState();
            if (state == ActiveTrackState.FINDING_TRACKED_TARGET ||
                    state == ActiveTrackState.AIRCRAFT_FOLLOWING ||
                    state == ActiveTrackState.ONLY_CAMERA_FOLLOWING ||
                    state == ActiveTrackState.CANNOT_CONFIRM ||
                    state == ActiveTrackState.WAITING_FOR_CONFIRMATION ||
                    state == ActiveTrackState.PERFORMING_QUICK_SHOT ||
                    state == ActiveTrackState.IDLE) {

                // ActiveTrack states in which it is able to fly relative to the target
                // https://developer.dji.com/api-reference/android-api/Components/Missions/DJIActiveTrackMissionOperator.html#djiactivetrackmissionoperator_acceptconfirmation_inline
                if (state == ActiveTrackState.IDLE || state == ActiveTrackState.WAITING_FOR_CONFIRMATION) {
                    activeTrackAcceptConfirmation();
                }

                // ActiveTrack execution management panel show
                view.updateActiveTrackMissionState(true);
            } else {
                // ActiveTrack execution management panel hide
                view.updateActiveTrackMissionState(false);
            }
        }
    }

//---------------------------------------- Active Track management -------------------------------//
    public void activeTrackStart() {
        RectF rectF = view.getAimRect(currentLocation.getAltitude());
        view.showAimRect(rectF.left, rectF.top, (int) rectF.width(), (int) rectF.height());
        activeTrackMission = new ActiveTrackMission(rectF, ActiveTrackMode.TRACE);
        // ActiveTrackMode.TRACE - Aircraft moves in behind the subject keeping a constant distance to it.
        // Mission with this mode can only be started when the aircraft is flying.

        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            // set ActiveTrackOperation event listener
            activeTrackOperator.addListener(this);

            activeTrackOperator.startTracking(activeTrackMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    view.activeTrackStart("Start Tracking: " +
                            (error == null ? "Success" : error.getDescription()));
                }
            });
        } else {
            view.activeTrackOperatorError("ActiveTrackOperator is not available.");
        }
    }

    public void activeTrackStop() {
        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            // reset ActiveTrackOperation event listener
            activeTrackOperator.removeListener(this);

            activeTrackOperator.stopTracking(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    view.activeTrackStop(error == null ? "Stop Tracking Success!" : error.getDescription());
                }
            });
        } else {
            view.activeTrackOperatorError("ActiveTrackOperator is not available.");
        }
    }

    public void activeTrackAcceptConfirmation() {
        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            activeTrackOperator.acceptConfirmation(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    view.activeTrackAcceptConfirmation(error == null ? "Accept Confirm Success!" : error.getDescription());
                }
            });
        } else {
            view.activeTrackOperatorError("ActiveTrackOperator is not available.");
        }
    }

    public void activeTrackRejectConfirmation() {
        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            activeTrackOperator.rejectConfirmation(new CommonCallbacks.CompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    view.activeTrackRejectConfirmation(error == null ? "Reject Confirm Success!" : error.getDescription());
                }
            });
        } else {
            view.activeTrackOperatorError("ActiveTrackOperator is not available.");
        }
    }

    @Nullable
    public LocationCoordinate3D getAircraftLocation() {
        return currentLocation;
    }
}