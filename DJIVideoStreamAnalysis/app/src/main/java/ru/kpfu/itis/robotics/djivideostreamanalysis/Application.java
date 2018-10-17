package ru.kpfu.itis.robotics.djivideostreamanalysis;

import android.content.Context;
import android.util.Log;

import com.secneo.sdk.Helper;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import dji.sdk.base.BaseProduct;

/**
 * Created by valera071998@gmail.com on 15.04.2018.
 */
@ReportsCrashes(mailTo = "valera071998@gmail.com",
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.info_application_crash_toast_text)
public class Application extends android.app.Application {
    public static final String TAG = Application.class.getName();

    private DJIApplication DJIApplication;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(Application.this);
        if (DJIApplication == null) {
            Log.i(TAG, "DJIApplication is null");
            DJIApplication = new DJIApplication();
            DJIApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate(). ");
        super.onCreate();
        ACRA.init(this);
        DJIApplication.onCreate();
    }
}