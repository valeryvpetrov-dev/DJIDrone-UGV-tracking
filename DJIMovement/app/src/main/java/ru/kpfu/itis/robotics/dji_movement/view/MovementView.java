package ru.kpfu.itis.robotics.dji_movement.view;

import dji.common.error.DJIError;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent; /**
 * Created by valera071998@gmail.com on 25.04.2018.
 */
public interface MovementView {

    void addRunningInfo(String info);

    void addTimelineInfo(String info);

    void updateAircraftLocation(double longitude, double latitude, float altitude);

    void setHomePointLocationSuccess(double longitude, double latitude);

    void setHomePointLocationError(String error);

    void cleanInfo();

    void showInfoNoHomePoint(String message);

    void showInfoEmptyTimeline(String message);
}