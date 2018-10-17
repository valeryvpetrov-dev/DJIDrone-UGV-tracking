package ru.kpfu.itis.robotics.djivideostreamanalysis.view;

import android.graphics.RectF;
import android.view.TextureView;

import dji.common.mission.activetrack.ActiveTrackMissionEvent;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public interface VideoStreamView {

    TextureView getVideoSurface();

    void updateConnectionStatus();

    // Aircraft state
    void updateAircraftLocation(double longitude, double latitude, float altitude);

    // VideoDataCallback
    void videoFeedSetCallback(String message);

    void videoDataCallbackOnReceive(String message);

    // Android SDK implementation
    void videoDataCallbackDequeueInputBuffer(int inputIndex);
    void videoDataCallbackDequeueOutputBuffer(int outputIndex);

    // Camera Settings
    void setCameraSettingsResolution(String resolutionSettings);
    void setCameraSettingsFrameRate(String frameRateSettings);
    void setCameraSettingsError(String description);

    void videoDataCallbackDequeueInputBuffer1(String s);
}