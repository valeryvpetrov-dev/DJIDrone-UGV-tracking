package ru.kpfu.itis.robotics.dji_movement.presenter;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import ru.kpfu.itis.robotics.dji_movement.DJIApplication;
import ru.kpfu.itis.robotics.dji_movement.util.GPSUtil;
import ru.kpfu.itis.robotics.dji_movement.view.MovementView;

/**
 * Created by valera071998@gmail.com on 25.04.2018.
 */
public class MovementPresenter {

    private static final String TAG = MovementPresenter.class.getName();

    private MovementView view;

    // Drone control elements
    private MissionControl missionControl;
    private TimelineEvent previousEvent;
    private TimelineElement previousElement;
    private DJIError previousError;

    // TODO how to get actual using GPS?
    private LocationCoordinate2D homePoint = new LocationCoordinate2D(55.792031, 49.122023);
    private LocationCoordinate3D currentLocation;

    public MovementPresenter(MovementView view, Activity activity) {
        Log.d(TAG, "Creating with view: " + view + ", activity: " + activity);
        this.view = view;

        initFlightController();
        initHomePoint();
    }

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
                            currentLocation.getAltitude());
                }
            });
        }
    }

    // Initializing flight mission
    public void initTimeline() {
        Log.d(TAG, "initTimeline().");

        if (homePoint == null) {
            initHomePoint();
        }
        if (!GPSUtil.checkGpsCoordinate(homePoint)) {
            view.showInfoNoHomePoint("No home point.");
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
            }
        };

        if (homePoint != null) {
            float currentAltitude = 0;
            double currentLatitude = homePoint.getLatitude();
            double currentLongitude = homePoint.getLongitude();
            Log.d(TAG, "currentAltitude: " + currentAltitude + ", " +
                    "currentLatitude: " + currentLatitude + ", " +
                    "currentLongitude: " + currentLongitude);

            //Step 1: takeoff from the ground
            timelineElements.add(new TakeOffAction());
            view.addTimelineInfo("Step 1: takeoff from the ground was added.");

            //Step 2: Perform simple movement
            currentAltitude = currentAltitude + 3;
            timelineElements.add(new GoToAction(currentAltitude));
            view.addTimelineInfo("Step 2.1: Go 3 meters up was added.");

            currentLongitude = currentLongitude + 8.955e-6;
            timelineElements.add(new GoToAction(new LocationCoordinate2D(currentLatitude, currentLongitude), currentAltitude));
            view.addTimelineInfo("Step 2.2: Go 8.955e-6 degrees forward was added.");

            currentLatitude = currentLatitude + 5.157e-5;
            timelineElements.add(new GoToAction(new LocationCoordinate2D(currentLatitude, currentLongitude), currentAltitude));
            view.addTimelineInfo("Step 2.3: Go 5.157e-5 degrees right was added.");

            //Step 3: go back home
            GoHomeAction goHomeAction = new GoHomeAction();
            goHomeAction.setAutoConfirmLandingEnabled(true);
            timelineElements.add(goHomeAction);
            view.addTimelineInfo("Step 3: go back home was added.");

            Log.d(TAG, "After mission. currentAltitude: " + currentAltitude + ", " +
                    "currentLatitude: " + currentLatitude + ", " +
                    "currentLongitude: " + currentLongitude);

            // Clean past mission control configuration
            cleanTimelineData();
            view.cleanInfo();

            // Set up new mission control configuration
            Log.d(TAG, "Set up new mission control configuration");
            Log.d(TAG, "timelineElements: " + timelineElements);
            Log.d(TAG, "MissionControl.Listener: " + listener);
            missionControl.scheduleElements(timelineElements);
            missionControl.addListener(listener);
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
                                    view.setHomePointLocationSuccess(homePoint.getLongitude(), homePoint.getLatitude());
                                } else {
                                    view.setHomePointLocationError("Can not set home point.");
                                }
                            }
                        });

                        view.setHomePointLocationSuccess(homePoint.getLongitude(), homePoint.getLatitude());
                    }
                }
            }
        }
        Log.d(TAG, "initHomePoint() after. homeLocation: " + homePoint);
    }

    // Cleaning mission and listeners
    public void cleanTimelineData() {
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

        view.addRunningInfo("element: " + (error == null ? "null" : element) + ", " +
                "event: " + (event == null ? "null" : event.name()) + ", " +
                "error: " + (error == null ? "null" : error.getDescription()));

        Log.d(TAG, "previousEvent: " + previousEvent + ", new event: " + event);
        Log.d(TAG, "previousElement: " + previousElement + ", new element: " + element);
        Log.d(TAG, "previousError: " + previousError + ", new error: " + error);

        previousEvent = event;
        previousElement = element;
        previousError = error;
    }

    public void startTimeline() {
        Log.d(TAG, "startTimeline. scheduledCount: " +
                (MissionControl.getInstance() != null ?
                        MissionControl.getInstance().scheduledCount() :
                        "MissionControl is null"));
        if (MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().startTimeline();
        } else {
            view.showInfoEmptyTimeline("Init the timeline first.");
        }
    }

    public void stopTimeline() {
        Log.d(TAG, "stopTimeline.");
        MissionControl.getInstance().stopTimeline();
    }

    public void pauseTimeline() {
        Log.d(TAG, "pauseTimeline.");
        MissionControl.getInstance().pauseTimeline();
    }

    public void resumeTimeline() {
        Log.d(TAG, "resumeTimeline.");
        MissionControl.getInstance().resumeTimeline();
    }
}