package ru.kpfu.itis.robotics.dji_tracking.presenter;

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

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.mission.activetrack.ActiveTrackMode;
import dji.common.mission.activetrack.ActiveTrackState;
import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.common.mission.activetrack.ActiveTrackTrackingState;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.sdkmanager.DJISDKManager;
import ru.kpfu.itis.robotics.dji_tracking.DJIApplication;
import ru.kpfu.itis.robotics.dji_tracking.R;
import ru.kpfu.itis.robotics.dji_tracking.util.GPSUtil;
import ru.kpfu.itis.robotics.dji_tracking.util.StringUtil;
import ru.kpfu.itis.robotics.dji_tracking.view.TrackingView;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public class TrackingPresenter implements TextureView.SurfaceTextureListener, ActiveTrackMissionOperatorListener {

    public static final String TAG = TrackingPresenter.class.getName();

    private static final float TAKE_OFF_ALTITUDE = 1.2f;
    private static final float ACTIVE_TRACK_START_ALTITUDE = 2.0f;

    private TrackingView view;
    private Activity activity;

    private BaseProduct product;

    private TextureView videoSurface;
    private VideoFeeder.VideoDataCallback videoDataCallback;
    private DJICodecManager djiCodecManager = null;

    // Drone control elements
    private MissionControl missionControl;
    private TimelineEvent previousEvent;
    private TimelineElement previousElement;
    private DJIError previousError;
    // TODO how to get actual using GPS?
    private LocationCoordinate2D homePoint = new LocationCoordinate2D(55.792031, 49.122023);
    private LocationCoordinate3D currentLocation;

    // ActiveTrack
    private ActiveTrackMission activeTrackMission;

    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            view.updateConnectionStatus();
            onProductChange();
        }
    };

    private StringUtil stringUtil;

    public TrackingPresenter(TrackingView view, Activity activity) {
        Log.d(TAG, "ActiveTrackTestPresenter(). view: " + view + ", activity: " + activity);
        this.view = view;
        this.activity = activity;
        this.stringUtil = new StringUtil(activity);

        registerConnectionChangeReceiver();

        videoSurface = view.getVideoSurface();
        if (videoSurface != null) {
            videoSurface.setSurfaceTextureListener(this);
        }
        // The callback for receiving the raw H264 video data for camera live view
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

        // set ActiveTrackOperation event listener
        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            activeTrackOperator.addListener(this);
        }
    }

    private void registerConnectionChangeReceiver() {
        Log.d(TAG, "registerConnectionChangeReceiver(). connectionChangeReceiver : " + connectionChangeReceiver);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(connectionChangeReceiver, filter);
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

//---------------------------------------- Active Track management -------------------------------//
    public void activeTrackStart() {
        RectF rectF = view.getAimRect(currentLocation.getAltitude());
        view.showAimRect(rectF.left, rectF.top, rectF.width(), rectF.height());

        activeTrackMission = new ActiveTrackMission(rectF, ActiveTrackMode.TRACE);
        // ActiveTrackMode.TRACE - Aircraft moves in behind the subject keeping a constant distance to it.
        // Mission with this mode can only be started when the aircraft is flying.

        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            activeTrackOperator.startTracking(activeTrackMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    view.activeTrackStart(
                            "Start Tracking: " +
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

    public void activeTrackSetRecommendedConfiguration() {
        ActiveTrackOperator activeTrackOperator = getActiveTrackOperator();
        if (activeTrackOperator != null) {
            activeTrackOperator.setRecommendedConfiguration(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    view.activeTrackSetRecommendedConfiguration(
                            "Set Recommended Config: " +
                                    (error == null ? "Success" : error.getDescription())
                    );
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
                    // TODO can slow down UI
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

//------------------------------------Tracking Task management------------------------------------//
    public void activeTrackingTaskStart() {
        initFlightController();
        initTimeline();
        startTimeline();
    }

    public void startTimeline() {
        Log.d(TAG, "startTimeline. scheduledCount: " +
                (MissionControl.getInstance() != null ?
                        MissionControl.getInstance().scheduledCount() :
                        "MissionControl is null"));
        if (MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().startTimeline();
            view.startTimelineSuccess("Timeline started successfully.");
        } else {
            view.startTimelineError("Timeline is empty.");
        }
    }

    public void stopTimeline() {
        Log.d(TAG, "stopTimeline.");
        MissionControl.getInstance().stopTimeline();
        view.stopTimeline("Timeline stopped successfully.");
    }

    public void pauseTimeline() {
        Log.d(TAG, "pauseTimeline.");
        MissionControl.getInstance().pauseTimeline();
    }

    public void resumeTimeline() {
        Log.d(TAG, "resumeTimeline.");
        MissionControl.getInstance().resumeTimeline();
    }

//---------------------------------------Tracking Task stuff--------------------------------------//
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
                    // update home point if it is not exist
                    if (homePoint == null) {
                        homePoint = new LocationCoordinate2D(currentLocation.getLatitude(), currentLocation.getLongitude());
                    }

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

    // Initializing flight mission
    private void initTimeline() {
        Log.d(TAG, "initTimeline().");

        if (homePoint == null) {
            initHomePoint();
        }
        if (!GPSUtil.checkGpsCoordinate(homePoint)) {
            view.initTimelineError("No home point.");
            return;
        }

        List<TimelineElement> timelineElements = new ArrayList<>();

        missionControl = MissionControl.getInstance();
        MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {
                Log.d(TAG, "MissionControl.Listener.onEvent(). " +
                        "TimelineElement: " + element +
                        "TimelineEvent: " + event +
                        "DJIError: " + error);
                updateTimelineStatus(element, event, error);
                // TODO catch the last action
                // log format
                // the last action djierror
            }
        };

        if (homePoint != null) {
            float currentAltitude = 0;
            double currentLatitude = homePoint.getLatitude();
            double currentLongitude = homePoint.getLongitude();
            Log.d(TAG, "currentAltitude: " + currentAltitude + ", " +
                    "currentLatitude: " + currentLatitude + ", " +
                    "currentLongitude: " + currentLongitude);

            // Clean past mission control configuration
            cleanTimelineData();
            view.cleanTimelineInfo();
            view.cleanTimelineStatus();

            //Step 1: takeoff from the ground
            currentAltitude = ACTIVE_TRACK_START_ALTITUDE;
            timelineElements.add(new GoToAction(currentAltitude));  // altitude 2.0
            view.addTimelineInfo("Step 1: takeoff from the ground was added.");

            //Step 2: reset the gimbal to horizontal angle in 2 seconds.
//            Attitude attitude = new Attitude(-90, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
//            GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
//            gimbalAction.setCompletionTime(2);
//            timelineElements.add(gimbalAction);
//            view.addTimelineInfo("Step 2: set the gimbal pitch -90 angle in 2 seconds");

            // Set up new mission control configuration
            Log.d(TAG, "Set up new mission control configuration");
            Log.d(TAG, "timelineElements: " + timelineElements);
            Log.d(TAG, "MissionControl.Listener: " + listener);

            missionControl.scheduleElements(timelineElements);
            missionControl.addListener(listener);

            // Callback to UI
            view.initTimelineSuccess("Init timeline success.");
        }
    }

    private void initHomePoint() {
        Log.d(TAG, "initHomePoint(). homeLocation: " + homePoint);

        if (homePoint == null) {
            FlightController flightController = DJIApplication.getFlightController();
            if (flightController != null) {
                FlightControllerState state = flightController.getState();
                if (state != null) {
                    LocationCoordinate3D locationAircraft = state.getAircraftLocation();
                    if (locationAircraft != null) {
                        homePoint = new LocationCoordinate2D(locationAircraft.getLatitude(), locationAircraft.getLongitude());

                        flightController.setHomeLocation(homePoint, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    view.setHomePointLocationSuccess(
                                            homePoint.getLongitude(),
                                            homePoint.getLatitude());
                                } else {
                                    view.setHomePointLocationError("Can not set home point.");
                                }
                            }
                        });

                        view.setHomePointLocationSuccess(
                                homePoint.getLongitude(),
                                homePoint.getLatitude());
                    }
                }
            }
        }
        Log.d(TAG, "initHomePoint() after. homeLocation: " + homePoint);
    }

    // Cleaning mission and listeners
    private void cleanTimelineData() {
        Log.d(TAG, "cleanTimelineData(). missionControl: " + missionControl);
        if (missionControl != null && missionControl.scheduledCount() > 0 && !missionControl.isTimelineRunning()) {
            Log.d(TAG, "missionControl.scheduledCount(): " + missionControl.scheduledCount());

            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
            Log.d(TAG, "missionControl unscheduled all parts and removed all listeners.");
        }
    }

    private void updateTimelineStatus(TimelineElement element, TimelineEvent event, DJIError error) {
        Log.d(TAG, "updateTimelineStatus().");
        if (element == previousElement &&
                event == previousEvent &&
                error == previousError) {
            Log.d(TAG, "There is no timeline changes.");
            return;
        }

        String timelineStatus = "element: " + (error == null ? "null" : element) + ", " +
                "event: " + (event == null ? "null" : event.name()) + ", " +
                "error: " + (error == null ? "null" : error.getDescription());
        view.updateTimelineStatus(timelineStatus);

        Log.d(TAG, "previousEvent: " + previousEvent + ", new event: " + event);
        Log.d(TAG, "previousElement: " + previousElement + ", new element: " + element);
        Log.d(TAG, "previousError: " + previousError + ", new error: " + error);

        // TODO rude
        // ! Active Track is injected here
        // after execution of actions start tracking
        if (previousEvent != null && event != null &&
                previousEvent == TimelineEvent.FINISHED && event == TimelineEvent.FINISHED) {
            activeTrackStart();
        }
        previousEvent = event;
        previousElement = element;
        previousError = error;
    }
}