package ru.kpfu.itis.robotics.dji_activetrack.util;

import android.graphics.RectF;
import android.support.annotation.NonNull;

/**
 * Created by valera071998@gmail.com on 10.07.2018.
 */
public class AimUtil {

    public static final double SCALING_FACTOR = 0.2f;

    @NonNull
    public static RectF getAimRect(double altitude) {
        float left = (float) (SCALING_FACTOR * altitude);
        float top = (float) (SCALING_FACTOR * altitude);

        // case when aircraft is too high
        if (left >= 0.5) {
            left = 0.4f;
        }
        if (top >= 0.5) {
            top = 0.4f;
        }

        float right = 1 - left;
        float bottom = 1 - top;

        return new RectF(left, top, right, bottom);
    }

    @NonNull
    public static RectF getAimRect(double altitude, double width, double height) {
        // TODO implementation
        return null;
    }
}