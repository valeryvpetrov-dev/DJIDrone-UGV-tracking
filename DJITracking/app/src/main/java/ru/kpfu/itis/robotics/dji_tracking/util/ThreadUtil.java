package ru.kpfu.itis.robotics.dji_tracking.util;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by valera071998@gmail.com on 11.05.2018.
 */
public class ThreadUtil {

    public static final String TAG = ThreadUtil.class.getName();

    public static void runOnUiToast(final Activity parent, final String message) {
        Log.d(TAG, "runOnUiToast(). parent: " + parent + " , message: " + message);

        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(parent.getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}