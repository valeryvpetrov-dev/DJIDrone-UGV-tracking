package ru.kpfu.itis.robotics.dji_activetrack.view;

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
import ru.kpfu.itis.robotics.dji_activetrack.DJIApplication;
import ru.kpfu.itis.robotics.dji_activetrack.R;
import ru.kpfu.itis.robotics.dji_activetrack.presenter.ActiveTrackPresenter;
import ru.kpfu.itis.robotics.dji_activetrack.util.AimUtil;
import ru.kpfu.itis.robotics.dji_activetrack.util.ThreadUtil;

public class ActiveTrackActivity extends AppCompatActivity implements ActiveTrackView, OnClickListener {

    private static final String TAG = ActiveTrackActivity.class.getName();

    private ImageView ivTrackingAim;
    private ImageView ivTrackingTarget;

    private TextView tvConnectionStatus;
    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvAltitude;

    private SlidingDrawer sdTrackingInfo;
    private TextView tvTrackingInfo;

    private Button btnConfig;
    private Button btnShowAim;
    private static boolean isShowingAim;    // turn on/off aim rect
    private Button btnStartTracking;

    private Button btnConfirmAccept;
    private ImageButton btnStopTracking;
    private Button btnConfirmReject;

    // TODO activity lifecycle presenter management
    private ActiveTrackPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate().");
        isShowingAim = false;

        setContentView(R.layout.activity_active_track);
        super.onCreate(savedInstanceState);

        initView();

        presenter = new ActiveTrackPresenter(this, this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy().");
        presenter.destroy();
        super.onDestroy();
    }

//---------------------------- ActiveTrackView implementation -------------------------------//
    @Override
    public TextureView getVideoSurface() {
        Log.d(TAG, "getVideoSurface().");
        return findViewById(R.id.tv_video_surface);
    }

    @Override
    public void updateActiveTrackMissionState(final String state) {
        Log.d(TAG, "updateActiveTrackMissionState(). state: " + state);
        if (state != null && state.trim().length() > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvTrackingInfo.setText(state);
                }
            });
        }
    }

    @Override
    public void updateActiveTrackMissionState(final boolean isActiveTrackWorking) {
        Log.d(TAG, "updateActiveTrackMissionState(). isActiveTrackWorking: " + isActiveTrackWorking);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActiveTrackWorking) {
                    showActiveTrackManagementButtons();
                    hideButtons();

                    ivTrackingAim.setVisibility(View.INVISIBLE);
                } else {
                    hideActiveTrackManagementButtons();
                    showButtons();

                    ivTrackingTarget.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void showActiveTrackManagementButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStopTracking.setVisibility(View.VISIBLE);
                btnStopTracking.setClickable(true);
                btnConfirmAccept.setVisibility(View.VISIBLE);
                btnConfirmAccept.setClickable(true);
                btnConfirmReject.setVisibility(View.VISIBLE);
                btnConfirmReject.setClickable(true);
            }
        });
    }

    private void hideActiveTrackManagementButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStopTracking.setVisibility(View.INVISIBLE);
                btnStopTracking.setClickable(false);
                btnConfirmAccept.setVisibility(View.INVISIBLE);
                btnConfirmAccept.setClickable(false);
                btnConfirmReject.setVisibility(View.INVISIBLE);
                btnConfirmReject.setClickable(false);
            }
        });
    }

    private void showButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConfig.setVisibility(View.VISIBLE);
                btnShowAim.setVisibility(View.VISIBLE);
                btnStartTracking.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConfig.setVisibility(View.INVISIBLE);
                btnShowAim.setVisibility(View.INVISIBLE);
                btnStartTracking.setVisibility(View.INVISIBLE);
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

    @Override
    public void updateAircraftLocation(final double longitude, double latitude, final float altitude) {
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

                if (isShowingAim) {
                    showAimRect(AimUtil.getAimRect(altitude));
                }
            }
        });
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
    public void showAimRect(final float x, final float y, final int width, final int height) {
        Log.d(TAG, "showAimRect(). ");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivTrackingAim.setVisibility(View.VISIBLE);
                ivTrackingAim.setX(x);
                ivTrackingAim.setY(y);
                ivTrackingAim.getLayoutParams().width = width;
                ivTrackingAim.getLayoutParams().height = height;
                ivTrackingAim.requestLayout();
                ivTrackingAim.invalidate();
            }
        });
    }

    @Override
    public void showAimRect(RectF rectF) {
        Log.d(TAG, "showAimRect(). rectF: " + rectF != null ? rectF.toShortString() : "null");

        isShowingAim = true;

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
    public void hideAimRect() {
        Log.d(TAG, "hideAimRect(). ");

        isShowingAim = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivTrackingAim.setVisibility(View.INVISIBLE);
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
                hideAimRect();
                hideButtons();
                showActiveTrackManagementButtons();
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
                ThreadUtil.runOnUiToast(activity, message);
                tvTrackingInfo.setText(getString(R.string.ui_tv_tracking_info));
                ivTrackingTarget.setVisibility(View.INVISIBLE);
                hideAimRect();
                hideActiveTrackManagementButtons();
                showButtons();
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

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTrackingInfo.setText(getString(R.string.ui_tv_tracking_info));
                ThreadUtil.runOnUiToast(activity, message);
            }
        });
    }

    @Override
    public void activeTrackRejectConfirmation(final String message) {
        Log.d(TAG, "activeTrackRejectConfirmation(). message: " + message);

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTrackingInfo.setText(getString(R.string.ui_tv_tracking_info));
                ThreadUtil.runOnUiToast(activity, message);
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

//--------------------------------- OnClickListener implementation -------------------------------//
    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick().");
        switch (v.getId()) {
            case R.id.btn_stop_tracking:
                Log.d(TAG, "onClick( tracking_stop_btn ).");
                presenter.activeTrackStop();
                break;
            case R.id.tv_connection_status:
                Log.d(TAG, "onClick( tv_connection_status ).");
                if (sdTrackingInfo.isOpened()) {
                    sdTrackingInfo.animateClose();
                } else {
                    sdTrackingInfo.animateOpen();
                }
                break;
            case R.id.btn_confirmation_accept:
                Log.d(TAG, "onClick( confirm_btn ).");
                presenter.activeTrackAcceptConfirmation();
                break;
            case R.id.btn_confirmation_reject:
                Log.d(TAG, "onClick( reject_btn ).");
                presenter.activeTrackRejectConfirmation();
                break;

            // specific actions
            case R.id.btn_set_recommended_configuration:
                Log.d(TAG, "onClick( recommended_configuration_btn ).");
                presenter.activeTrackSetRecommendedConfiguration();
                break;
            case R.id.btn_show_aim_rect:
                Log.d(TAG, "onClick( btn_show_aim_rect ).");
                if (isShowingAim) {
                    hideAimRect();
                } else {
                    showAimRect(getAimRect(presenter.getAircraftLocation() != null ?
                            presenter.getAircraftLocation().getAltitude() : 0));
                }
                break;
            case R.id.btn_start_tracking:
                Log.d(TAG, "onClick( btn_start_tracking ).");
                presenter.activeTrackStart();
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
        tvLongitude = findViewById(R.id.tv_state_longitude);
        tvLatitude = findViewById(R.id.tv_state_latitude);
        tvAltitude = findViewById(R.id.tv_state_altitude);

        btnConfig = findViewById(R.id.btn_set_recommended_configuration);
        btnShowAim = findViewById(R.id.btn_show_aim_rect);
        btnStartTracking = findViewById(R.id.btn_start_tracking);

        btnConfirmAccept = findViewById(R.id.btn_confirmation_accept);
        btnStopTracking = findViewById(R.id.btn_stop_tracking);
        btnConfirmReject = findViewById(R.id.btn_confirmation_reject);

        tvConnectionStatus.setOnClickListener(this);
        btnConfig.setOnClickListener(this);
        btnShowAim.setOnClickListener(this);
        btnStartTracking.setOnClickListener(this);
        btnConfirmAccept.setOnClickListener(this);
        btnStopTracking.setOnClickListener(this);
        btnConfirmReject.setOnClickListener(this);

        sdTrackingInfo = findViewById(R.id.sd_tracking_info);

        // hide ActiveTrack execution management buttons
        updateActiveTrackMissionState(false);
    }
}