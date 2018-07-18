package ru.kpfu.itis.robotics.dji_video_stream_analysis.view;

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
}