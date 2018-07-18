package ru.kpfu.itis.robotics.dji_tracking.util;

import android.util.Log;

import dji.common.model.LocationCoordinate2D;

/**
 * Created by valera071998@gmail.com on 26.04.2018.
 */
public class GPSUtil {

    private static final String TAG = GPSUtil.class.getName();

    public static boolean checkGpsCoordinate(double latitude, double longitude) {
        Log.d(TAG, "checkGpsCoordinate(). " +
                "(latitude, longitude) = (" + latitude + ", " + longitude + ")");

        boolean isValidCoordinates = (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) &&
                (latitude != 0f && longitude != 0f);

        Log.d(TAG, "isValidCoordinates: " + isValidCoordinates);
        return isValidCoordinates;
    }

    public static boolean checkGpsCoordinate(LocationCoordinate2D locationCoordinate) {
        if (locationCoordinate != null) {
            double latitude = locationCoordinate.getLatitude();
            double longitude = locationCoordinate.getLongitude();

            return checkGpsCoordinate(latitude, longitude);
        } else {
            return false;
        }
    }
}
