package ru.kpfu.itis.robotics.djivideostreamanalysis.view;

import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.TextureView;

import dji.common.mission.activetrack.ActiveTrackMissionEvent;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public interface VideoStreamView {

    TextureView getLivestreamPreviewTextureView();
    SurfaceView getLivestreamPreviewSurfaceView();

    void updateConnectionStatus();

    // Aircraft state
    void updateAircraftLocation(double longitude, double latitude, float altitude);

    // VideoDataCallback
    void videoFeedSetCallback(String message);

    void videoDataCallbackOnReceive(String message);

    // Camera Settings
    void setCameraSettingsResolution(String resolutionSettings);
    void setCameraSettingsFrameRate(String frameRateSettings);
    void setCameraSettingsError(String errorDescription);

    void setCameraModeError(String errorDescription);
}