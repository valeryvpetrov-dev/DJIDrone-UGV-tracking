package ru.kpfu.itis.robotics.dji_activetrack.view;

import android.graphics.RectF;
import android.view.TextureView;

import dji.common.mission.activetrack.ActiveTrackMissionEvent;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public interface ActiveTrackView {

    TextureView getVideoSurface();

    void updateConnectionStatus();

    // RectF target
    RectF getAimRect(float altitude);

    void showAimRect(RectF rectF);

    void showAimRect(float x, float y, int width, int height);

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
}