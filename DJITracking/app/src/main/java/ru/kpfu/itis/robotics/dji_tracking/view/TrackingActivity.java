package ru.kpfu.itis.robotics.dji_tracking.view;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;

import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import ru.kpfu.itis.robotics.dji_tracking.DJIApplication;
import ru.kpfu.itis.robotics.dji_tracking.R;
import ru.kpfu.itis.robotics.dji_tracking.presenter.TrackingPresenter;
import ru.kpfu.itis.robotics.dji_tracking.util.AimUtil;
import ru.kpfu.itis.robotics.dji_tracking.util.ThreadUtil;

public class TrackingActivity extends AppCompatActivity implements TrackingView, OnClickListener {

    private static final String TAG = TrackingActivity.class.getName();

    public static final String PROCESS_MOVING = "MOVING";
    public static final String PROCESS_TRACKING = "TRACKING";
    public static final String PROCESS_NONE = "NONE";
    public static final String[] PROCESSES = new String[] {PROCESS_MOVING , PROCESS_TRACKING, PROCESS_NONE};

    private TextView tvConnectionStatus;

    // Aim representation
    private ImageView ivTrackingAim;
    private ImageView ivTrackingTarget;

    // Processes info
    private SlidingDrawer sdTrackingTaskInfo;
    private TextView tvTrackingInfo;
    private TextView tvMissionControlInfo;
    private TextView tvTimelineInfo;

    private int timelineActionIndex;

    // Aircraft state info
    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvAltitude;

    // Control buttons
    private Button btnStartTrackingTask;
    private ImageButton btnStopTracking;
    // Indicates executing process
    private String currentProcess;

    // TODO activity lifecycle presenter management
    private TrackingPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate().");
        timelineActionIndex = 1;
        currentProcess = PROCESS_NONE;

        setContentView(R.layout.activity_tracking);
        super.onCreate(savedInstanceState);

        initView();

        presenter = new TrackingPresenter(this, this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy().");
        presenter.destroy();
        super.onDestroy();
    }

//---------------------------- ActiveTrackTestView implementation -------------------------------//
    @Override
    public TextureView getVideoSurface() {
        Log.d(TAG, "getVideoSurface().");
        return findViewById(R.id.tv_video_surface);
    }

    @Override
    public void updateAircraftLocation(double longitude, double latitude, float altitude) {
        Log.d(TAG, "updateAircraftLocation().");

        final double longitudeScaled = BigDecimal.valueOf(longitude)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
        final double latitudeScaled = BigDecimal.valueOf(latitude)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
        final double altitudeScaled = BigDecimal.valueOf(altitude)
                .setScale(3, RoundingMode.HALF_UP)
                .floatValue();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLongitude.setText("Longitude:" + longitudeScaled);
                tvLatitude.setText("Latitude:" + latitudeScaled);
                tvAltitude.setText("Altitude:" + altitudeScaled);
            }
        });
    }

    @Override
    public void updateActiveTrackMissionState(final String state) {
        Log.d(TAG, "updateActiveTrackMissionState(). state: " + state);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state != null && state.trim().length() > 0) {
                    tvTrackingInfo.setText(state);
                }
            }
        });
    }

    @Override
    public void updateActiveTrackMissionState(final boolean isActiveTrackWorking) {
        Log.d(TAG, "updateActiveTrackMissionState(). isActiveTrackWorking: " + isActiveTrackWorking);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActiveTrackWorking) {
                    btnStopTracking.setClickable(true);
                    btnStopTracking.setVisibility(View.VISIBLE);
                    btnStartTrackingTask.setVisibility(View.INVISIBLE);

                    ivTrackingTarget.setVisibility(View.VISIBLE);
                    ivTrackingAim.setVisibility(View.INVISIBLE);
                } else {
                    btnStopTracking.setClickable(false);
                    btnStopTracking.setVisibility(View.INVISIBLE);
                    btnStartTrackingTask.setVisibility(View.VISIBLE);

                    ivTrackingTarget.setVisibility(View.INVISIBLE);
                    ivTrackingAim.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void updateActiveTrackRect(final ActiveTrackMissionEvent event) {
        Log.d(TAG, "updateActiveTrackRect(). ActiveTrackMissionEvent: " +
                (event != null ? event : "null"));

        if (ivTrackingTarget == null || event == null) return;
        View parent = (View) ivTrackingTarget.getParent();

        if (event.getTrackingState() != null) {
            RectF trackingRect = event.getTrackingState().getTargetRect();
            final int l = (int) ((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
            final int t = (int) ((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
            final int r = (int) ((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
            final int b = (int) ((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ActiveTrackTargetState targetState = event.getTrackingState().getState();

                    if ((targetState == ActiveTrackTargetState.CANNOT_CONFIRM)
                            || (targetState == ActiveTrackTargetState.UNKNOWN)) {
                        ivTrackingTarget.setImageResource(R.drawable.visual_track_cannot_confirm);
                    } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
                        ivTrackingTarget.setImageResource(R.drawable.visual_track_need_confirm);
                    } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
                        ivTrackingTarget.setImageResource(R.drawable.visual_track_low_confidence);
                    } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
                        ivTrackingTarget.setImageResource(R.drawable.visual_track_high_confidence);
                    }
                    showTargetRect(l, t, r - l, b - t);
                }
            });
        }
    }

    @NonNull
    @Override
    public RectF getAimRect(float altitude) {
        Log.d(TAG, "getAimRect().");

        // TODO rect = function(altitude, target size)
        RectF aimRect = AimUtil.getAimRect(altitude);

        Log.d(TAG, "getAimRect(). " + aimRect.toShortString());
        return aimRect;
    }

    @Override
    public void showAimRect(RectF rectF) {
        Log.d(TAG, "showAimRect(). rectF: " + rectF != null ? rectF.toShortString() : "null");

        View parent = (View) ivTrackingAim.getParent();

        if (parent != null) {
            final int l = (int) ((rectF.centerX() - rectF.width() / 2) * parent.getWidth());
            final int t = (int) ((rectF.centerY() - rectF.height() / 2) * parent.getHeight());
            final int r = (int) ((rectF.centerX() + rectF.width() / 2) * parent.getWidth());
            final int b = (int) ((rectF.centerY() + rectF.height() / 2) * parent.getHeight());
            showAimRect(l, t, r - l, b - t);
        }
    }

    @Override
    public void showAimRect(final float x, final float y, final float width, final float height) {
        Log.d(TAG, "showAimRect(). ");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivTrackingAim.setVisibility(View.VISIBLE);
                ivTrackingAim.setX(x);
                ivTrackingAim.setY(y);
                ivTrackingAim.getLayoutParams().width = (int) width;
                ivTrackingAim.getLayoutParams().height = (int) height;
                ivTrackingAim.requestLayout();
                ivTrackingAim.invalidate();
            }
        });
    }

    @Override
    public void hideAimRect() {
        Log.d(TAG, "hideAimRect(). ");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivTrackingAim.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void showTargetRect(final float x, final float y, final int width, final int height) {
        Log.d(TAG, "showTargetRect(). ");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivTrackingTarget.setVisibility(View.VISIBLE);
                ivTrackingTarget.setX(x);
                ivTrackingTarget.setY(y);
                ivTrackingTarget.getLayoutParams().width = width;
                ivTrackingTarget.getLayoutParams().height = height;
                ivTrackingTarget.requestLayout();
            }
        });
    }

    @Override
    public void activeTrackStart(final String message) {
        Log.d(TAG, "activeTrackStart(). message: " + message);

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.runOnUiToast(activity, message);
                ivTrackingAim.setVisibility(View.INVISIBLE);
                btnStopTracking.setVisibility(View.VISIBLE);
                currentProcess = PROCESS_TRACKING;
            }
        });
    }

    @Override
    public void activeTrackOperatorError(final String message) {
        Log.d(TAG, "activeTrackOperatorError(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void activeTrackStop(final String message) {
        Log.d(TAG, "activeTrackStop(). message: " + message);

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTrackingInfo.setText(getString(R.string.ui_tv_tracking_info));
                ThreadUtil.runOnUiToast(activity, message);
                currentProcess = PROCESS_NONE;
            }
        });
    }

    @Override
    public void activeTrackSetRecommendedConfiguration(final String message) {
        Log.d(TAG, "activeTrackSetRecommendedConfiguration(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void activeTrackAcceptConfirmation(final String message) {
        Log.d(TAG, "activeTrackAcceptConfirmation(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void activeTrackRejectConfirmation(final String message) {
        Log.d(TAG, "activeTrackRejectConfirmation(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void initTimelineSuccess(String message) {
        Log.d(TAG, "initTimelineSuccess(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void initTimelineError(String message) {
        Log.d(TAG, "initTimelineError(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void setHomePointLocationSuccess(double longitude, double latitude) {
        Log.d(TAG, "setHomePointLocationSuccess().");

        updateAircraftLocation(longitude, latitude, 0f);
        ThreadUtil.runOnUiToast(this, getString(R.string.info_flight_set_home_point_success));
    }

    @Override
    public void setHomePointLocationError(String message) {
        Log.d(TAG, "setHomePointLocationError(). message: " + message);

        ThreadUtil.runOnUiToast(this, getString(R.string.info_flight_set_home_point_error));
    }

    @Override
    public void addTimelineInfo(final String timelineInfo) {
        Log.d(TAG, "addTimelineInfo(). message: " + timelineInfo);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (timelineInfo != null && timelineInfo.trim().length() > 0) {
                    tvTimelineInfo.append(timelineInfo + "\n");
                }
            }
        });
    }

    @Override
    public void cleanTimelineInfo() {
        Log.d(TAG, "cleanTimelineInfo().");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTimelineInfo.setText("");
            }
        });
    }

    @Override
    public void updateTimelineStatus(final String timelineStatus) {
        Log.d(TAG, "updateTimelineStatus(). timelineStatus: " + timelineStatus);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (timelineStatus != null && timelineStatus.trim().length() > 0) {
                    tvMissionControlInfo.append(timelineActionIndex + ". " + timelineStatus + "\n");
                    timelineActionIndex++;
                }
            }
        });
    }

    @Override
    public void cleanTimelineStatus() {
        Log.d(TAG, "cleanTimelineStatus().");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMissionControlInfo.setText("");
            }
        });
    }

    @Override
    public void startTimelineSuccess(final String message) {
        Log.d(TAG, "startTimelineSuccess(). message: " + message);

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.runOnUiToast(activity, message);
                btnStartTrackingTask.setVisibility(View.INVISIBLE);

                btnStopTracking.setVisibility(View.VISIBLE);
                currentProcess = PROCESS_MOVING;
            }
        });

    }

    @Override
    public void startTimelineError(String message) {
        Log.d(TAG, "startTimelineError(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void stopTimeline(String message) {
        Log.d(TAG, "stopTimeline(). message: " + message);

        ThreadUtil.runOnUiToast(this, message);
        currentProcess = PROCESS_NONE;
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

//--------------------------------- OnClickListener implementation -------------------------------//
    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick().");
        switch (v.getId()) {
            case R.id.btn_stop_tracking:
                Log.d(TAG, "onClick( tracking_stop_btn ).");
                switch (currentProcess) {
                    case PROCESS_NONE:
                        break;
                    case PROCESS_MOVING:
                        presenter.stopTimeline();
                        currentProcess = PROCESS_NONE;
                        break;
                    case PROCESS_TRACKING:
                        presenter.activeTrackStop();
                        currentProcess = PROCESS_NONE;
                        break;
                }
                break;
            case R.id.tv_connection_status:
                Log.d(TAG, "onClick( tv_connection_status ).");
                if (sdTrackingTaskInfo.isOpened()) {
                    sdTrackingTaskInfo.animateClose();
                } else {
                    sdTrackingTaskInfo.animateOpen();
                }
                break;

            // specific actions
            case R.id.btn_start_tracking_task:
                Log.d(TAG, "onClick( btn_start_tracking_task ).");
                presenter.activeTrackingTaskStart();
                break;
        }
    }

//------------------------------------------------------------------------------------------------//
    private void initView() {
        Log.d(TAG, "initView().");
        ivTrackingAim = findViewById(R.id.iv_tracking_aim);
        ivTrackingAim.setImageResource(R.drawable.visual_track_target_bg);

        ivTrackingTarget = findViewById(R.id.iv_tracking_target);

        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        
        tvTrackingInfo = findViewById(R.id.tv_tracking_info);
        tvMissionControlInfo = findViewById(R.id.tv_mission_control_info);
        tvTimelineInfo = findViewById(R.id.tv_timeline_info);

        tvAltitude = findViewById(R.id.tv_state_altitude);
        tvLongitude = findViewById(R.id.tv_state_longitude);
        tvLatitude = findViewById(R.id.tv_state_latitude);

        btnStartTrackingTask = findViewById(R.id.btn_start_tracking_task);

        btnStopTracking = findViewById(R.id.btn_stop_tracking);
        btnStopTracking.setVisibility(View.INVISIBLE);

        tvConnectionStatus.setOnClickListener(this);
        btnStartTrackingTask.setOnClickListener(this);
        btnStopTracking.setOnClickListener(this);

        sdTrackingTaskInfo = findViewById(R.id.sd_tracking_task_info);

        // hide ActiveTrack execution management buttons
        updateActiveTrackMissionState(false);
    }
}