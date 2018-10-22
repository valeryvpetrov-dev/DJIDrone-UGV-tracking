package ru.kpfu.itis.robotics.djivideostreamanalysis.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import ru.kpfu.itis.robotics.djivideostreamanalysis.DJIApplication;
import ru.kpfu.itis.robotics.djivideostreamanalysis.R;
import ru.kpfu.itis.robotics.djivideostreamanalysis.presenter.VideoStreamPresenter;

public class VideoStreamActivity extends AppCompatActivity implements VideoStreamView {

    private static final String TAG = VideoStreamActivity.class.getName();

    private TextView tvConnectionStatus;

    // Aircraft location
    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvAltitude;

    // Camera settings
    private TextView tvVideoSettingsResolution;
    private TextView tvVideoSettingsFrameRate;

    // VideoData callback
    private TextView tvVideoDataCallbackRegistration;
    private TextView tvVideoDataCallbackOnReceive;

    // TODO activity lifecycle presenter management
    private VideoStreamPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate().");
        setContentView(R.layout.activity_video_stream);
        super.onCreate(savedInstanceState);

        initView();

        presenter = new VideoStreamPresenter(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.initVideoSurface();
            presenter.onProductChange();
        }
    }

    @Override
    protected void onPause() {
        if (presenter != null) {
            presenter.uninitVideoSurface();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy().");
        if (presenter != null) {
            presenter.destroy();
        }
        presenter = null;
        super.onDestroy();
    }

    @Override
    public TextureView getLivestreamPreviewTextureView() {
        Log.d(TAG, "getLivestreamPreviewTextureView().");
        return findViewById(R.id.tv_livestream_preview);
    }

    @Override
    public SurfaceView getLivestreamPreviewSurfaceView() {
//        Log.d(TAG, "getLivestreamPreviewSurfaceView().");
//        return findViewById(R.id.sv_livestream_preview);
        return null;
    }

    @Override
    public void updateAircraftLocation(final double longitude, double latitude, final float altitude) {
        Log.d(TAG, "updateAircraftLocation().");

        double longitudeScaled = -1;
        double latitudeScaled = -1;
        double altitudeScaled = -1;
        if (!Double.isNaN(longitude)) {
            longitudeScaled = BigDecimal.valueOf(longitude)
                    .setScale(3, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        if (!Double.isNaN(latitude)) {
            latitudeScaled = BigDecimal.valueOf(latitude)
                    .setScale(3, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        if (!Double.isNaN(altitude)) {
            altitudeScaled = BigDecimal.valueOf(altitude)
                    .setScale(3, RoundingMode.HALF_UP)
                    .floatValue();
        }

        final double finalLongitudeScaled = longitudeScaled;
        final double finalLatitudeScaled = latitudeScaled;
        final double finalAltitudeScaled = altitudeScaled;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                tvLongitude.setText("Longitude:" + finalLongitudeScaled);
                tvLatitude.setText("Latitude:" + finalLatitudeScaled);
                tvAltitude.setText("Altitude:" + finalAltitudeScaled);
            }
        });
    }

    @Override
    public void videoFeedSetCallback(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvVideoDataCallbackRegistration.setText("Registration: " + message);
            }
        });
    }

    @Override
    public void videoDataCallbackOnReceive(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvVideoDataCallbackOnReceive.setText("OnReceive: " + message);
            }
        });
    }

    @Override
    public void setCameraSettingsResolution(final String resolutionSettings) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvVideoSettingsResolution.setText(resolutionSettings);
            }
        });
    }

    @Override
    public void setCameraSettingsFrameRate(final String frameRateSettings) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvVideoSettingsFrameRate.setText(frameRateSettings);
            }
        });
    }

    @Override
    public void setCameraSettingsError(final String errorDescription) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvVideoSettingsResolution.setText(errorDescription);
                tvVideoSettingsFrameRate.setText(errorDescription);
            }
        });
    }

    @Override
    public void setCameraModeError(final String errorDescription) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoStreamActivity.this, errorDescription, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void updateConnectionStatus() {
        Log.d(TAG, "updateConnectionStatus().");
        if (tvConnectionStatus != null) {
            boolean isAircraftOrRCConnected = false;
            BaseProduct product = DJIApplication.getProductInstance();
            if (product != null) {
                if (product.isConnected()) {
                    tvConnectionStatus.setText(DJIApplication.getProductInstance().getModel().getDisplayName() + " Connected.");
                    isAircraftOrRCConnected = true;
                } else {
                    if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                            tvConnectionStatus.setText(getString(R.string.info_connection_rc_only_connected));
                            isAircraftOrRCConnected = true;
                        }
                    }
                }
            }

            if (!isAircraftOrRCConnected) {
                tvConnectionStatus.setText(getString(R.string.info_connection_aircraft_no_connection));
            }
        }
    }

//------------------------------------------------------------------------------------------------//
    private void initView() {
        Log.d(TAG, "initView().");
        tvConnectionStatus = findViewById(R.id.tv_connection_status);

        tvLongitude = findViewById(R.id.tv_state_longitude);
        tvLatitude = findViewById(R.id.tv_state_latitude);
        tvAltitude = findViewById(R.id.tv_state_altitude);

        tvVideoSettingsResolution = findViewById(R.id.tv_video_settings_resolution);
        tvVideoSettingsFrameRate = findViewById(R.id.tv_video_settings_frame_rate);

        tvVideoDataCallbackRegistration = findViewById(R.id.tv_state_video_data_registration);
        tvVideoDataCallbackOnReceive = findViewById(R.id.tv_state_video_data_callback_on_receive);
    }
}