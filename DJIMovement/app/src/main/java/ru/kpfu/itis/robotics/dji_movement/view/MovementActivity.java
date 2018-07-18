package ru.kpfu.itis.robotics.dji_movement.view;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.kpfu.itis.robotics.dji_movement.R;
import ru.kpfu.itis.robotics.dji_movement.presenter.MovementPresenter;
import ru.kpfu.itis.robotics.dji_movement.util.ThreadUtil;

public class MovementActivity extends AppCompatActivity implements View.OnClickListener, MovementView {

    public static final String TAG = MovementActivity.class.getName();

    private LinearLayout container;

    private List<String> runningInfo;
    private List<String> timelineInfo;
    private ListView lvRunningInfo;
    private ListView lvTimelineInfo;
    private ArrayAdapter<String> lvAdapterRunningInfo;
    private ArrayAdapter<String> lvAdapterTimelineInfo;

    private TextView tvLongitude;
    private TextView tvLatitude;
    private TextView tvAltitude;

    private Button btnInitMission;
    private Button btnClearMission;
    private Button btnStartMission;
    private Button btnPauseMission;
    private Button btnResumeMission;
    private Button btnStopMission;

    private MovementPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_movement);
        initUI();

        presenter = new MovementPresenter(this, this);
    }

    @Override
    protected void onDestroy() {
        presenter.cleanTimelineData();
        cleanInfo();
        super.onDestroy();
    }

    @Override
    public void addRunningInfo(final String info) {
        container.post(new Runnable() {
            @Override
            public void run() {
                runningInfo.add(info);
                lvAdapterRunningInfo.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void addTimelineInfo(final String info) {
        container.post(new Runnable() {
            @Override
            public void run() {
                timelineInfo.add(info);
                lvAdapterTimelineInfo.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void cleanInfo() {
        Log.d(TAG, "cleanInfo().");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runningInfo.clear();
                lvAdapterRunningInfo.clear();
                timelineInfo.clear();
                lvAdapterTimelineInfo.clear();
            }
        });
    }

    @Override
    public void showInfoNoHomePoint(String message) {
        Log.d(TAG, "showInfoNoHomePoint(). " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void showInfoEmptyTimeline(String message) {
        Log.d(TAG, "showInfoEmptyTimeline(). " + message);

        ThreadUtil.runOnUiToast(this, message);
    }

    @Override
    public void updateAircraftLocation(final double longitude, final double latitude, final float altitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLongitude.setText("Longitude:" + Double.toString(longitude));
                tvLatitude.setText("Latitude:" + Double.toString(latitude));
                tvAltitude.setText("Altitude:" + Float.toString(altitude));
            }
        });
    }

    @Override
    public void setHomePointLocationSuccess(final double longitude, final double latitude) {
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAircraftLocation(longitude, latitude, 0);
                ThreadUtil.runOnUiToast(activity, activity.getString(R.string.info_flight_set_home_point_success));
            }
        });
    }

    @Override
    public void setHomePointLocationError(final String error) {
        ThreadUtil.runOnUiToast(this, error);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_init_mission: {
                Log.d(TAG, "btnInitTimeline click.");
                presenter.initTimeline();
                break;
            }
            case R.id.btn_start_mission: {
                Log.d(TAG, "btnStartMission click.");
                presenter.startTimeline();
                break;
            }
            case R.id.btn_pause_mission: {
                Log.d(TAG, "btnPauseMission click.");
                presenter.pauseTimeline();
                break;
            }
            case R.id.btn_resume_mission: {
                Log.d(TAG, "btnResumeMission click.");
                presenter.resumeTimeline();
                break;
            }
            case R.id.btn_stop_mission: {
                Log.d(TAG, "btnStopMission click.");
                presenter.stopTimeline();
                break;
            }
            case R.id.btn_clear_timeline: {
                Log.d(TAG, "btnClearTimeline click.");
                presenter.cleanTimelineData();
                cleanInfo();
                break;
            }
            default:
                break;
        }
    }

    private void initUI() {
        container = findViewById(R.id.container);

        runningInfo = new ArrayList<>();
        lvRunningInfo = findViewById(R.id.lv_running_info);
        lvAdapterRunningInfo = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                runningInfo);
        lvRunningInfo.setAdapter(lvAdapterRunningInfo);

        timelineInfo = new ArrayList<>();
        lvTimelineInfo = findViewById(R.id.lv_timeline_info);
        lvAdapterTimelineInfo = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                timelineInfo);
        lvTimelineInfo.setAdapter(lvAdapterTimelineInfo);

        tvLongitude = findViewById(R.id.tv_state_longitude);
        tvLatitude = findViewById(R.id.tv_state_latitude);
        tvAltitude = findViewById(R.id.tv_state_altitude);

        btnInitMission = findViewById(R.id.btn_init_mission);
        btnClearMission = findViewById(R.id.btn_clear_timeline);
        btnStartMission = findViewById(R.id.btn_start_mission);
        btnPauseMission = findViewById(R.id.btn_pause_mission);
        btnResumeMission = findViewById(R.id.btn_resume_mission);
        btnStopMission = findViewById(R.id.btn_stop_mission);

        btnInitMission.setOnClickListener(this);
        btnClearMission.setOnClickListener(this);
        btnStartMission.setOnClickListener(this);
        btnPauseMission.setOnClickListener(this);
        btnResumeMission.setOnClickListener(this);
        btnStopMission.setOnClickListener(this);
    }
}