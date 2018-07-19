package ru.kpfu.itis.robotics.dji_tracking.view;

import android.graphics.RectF;
import android.view.TextureView;

import dji.common.mission.activetrack.ActiveTrackMissionEvent;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public interface TrackingView {

    TextureView getVideoSurface();

    void updateConnectionStatus();

    // RectF target
    RectF getAimRect(float altitude);

    void showAimRect(RectF rectF);

    void showAimRect(float x, float y, float width, float height);

    void hideAimRect();

    void showTargetRect(float x, float y, int width, int height);

    void updateActiveTrackRect(ActiveTrackMissionEvent event);

    // Aircraft state
    void updateAircraftLocation(double longitude, double latitude, float altitude);

    // ActiveTrack state
    void updateActiveTrackMissionState(String state);

    void updateActiveTrackMissionState(boolean isActiveTrackWorking);

    // ActiveTrack management
    void activeTrackStart(String message);

    void activeTrackOperatorError(String message);

    void activeTrackStop(String message);

    void activeTrackSetRecommendedConfiguration(String message);

    void activeTrackAcceptConfirmation(String message);

    void activeTrackRejectConfirmation(String message);

    // Timeline state
    void setHomePointLocationSuccess(double longitude, double latitude);

    void setHomePointLocationError(String message);

    void initTimelineSuccess(String message);

    void initTimelineError(String message);

    void startTimelineSuccess(String message);

    void startTimelineError(String message);

    void stopTimeline(String message);

    void addTimelineInfo(String info);

    void addMissionControlInfo(String info);

    void cleanTimelineInfo();

    void cleanMissionControlInfo();
}