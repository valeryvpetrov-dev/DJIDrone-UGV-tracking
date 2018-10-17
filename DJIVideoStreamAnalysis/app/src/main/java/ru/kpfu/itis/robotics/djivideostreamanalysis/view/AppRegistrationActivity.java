package ru.kpfu.itis.robotics.djivideostreamanalysis.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ru.kpfu.itis.robotics.djivideostreamanalysis.MainActivity;
import ru.kpfu.itis.robotics.djivideostreamanalysis.R;
import ru.kpfu.itis.robotics.djivideostreamanalysis.presenter.AppRegistrationPresenter;

public class AppRegistrationActivity extends AppCompatActivity implements View.OnClickListener, AppRegistrationView {

    private static final String TAG = AppRegistrationActivity.class.getName();

    private TextView tvConnectionStatus;
    private TextView tvProduct;
    private Button btnOpenVideoStreamActivity;
    private Button btnOpenTest;

    private AppRegistrationPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate().");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_registration);
        initView();

        presenter = new AppRegistrationPresenter(this, this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy().");
        presenter.unregisterConnectionChangeReceiver();
        super.onDestroy();
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult().");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        presenter.checkForGrantedPermissions(requestCode, permissions, grantResults);
    }

    @Override
    public void showProductConnectionSuccess(String productName) {
        Log.d(TAG,"showProductConnectionSuccess(). productName: " + productName);

        tvConnectionStatus.setText(R.string.info_connection_aircraft_is_connected);

        if (productName != null && !productName.trim().equals("")) {
            tvProduct.setText(productName);
        } else {
            tvProduct.setText(R.string.info_connection_loose);
        }

        // if (isApplicationRegistered)
        btnOpenVideoStreamActivity.setEnabled(true);
        btnOpenTest.setEnabled(true);
    }

    @Override
    public void showProductConnectionError() {
        Log.d(TAG,"showProductConnectionError().");

        btnOpenVideoStreamActivity.setEnabled(false);
        btnOpenTest.setEnabled(false);

        tvProduct.setText(R.string.ui_tv_product_information);
        tvConnectionStatus.setText(R.string.info_connection_loose);
    }

    @Override
    public void showAppRegistrationSuccess() {
        Log.d(TAG,"showAppRegistrationSuccess().");
        showToast("Registration was performed successfully.");
        btnOpenVideoStreamActivity.setEnabled(true);
        btnOpenTest.setEnabled(true);
    }

    @Override
    public void showAppRegistrationError() {
        Log.d(TAG,"showAppRegistrationSuccess().");
        showToast("SDK Registration was failed, check network is available.");
        btnOpenVideoStreamActivity.setEnabled(false);
        btnOpenTest.setEnabled(false);
    }

    @Override
    public void showToast(final String message) {
        Log.d(TAG, "showToast(). message: " + message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick(). view: " + v);
        switch (v.getId()) {
            case R.id.btn_open_video_stream_activity: {
                Log.d(TAG, "btn_open_active_track click.");
                Intent intent = new Intent(AppRegistrationActivity.this, VideoStreamActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btn_open_test: {
                Intent intent = new Intent(AppRegistrationActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    private void initView() {
        Log.d(TAG, "initView().");
        tvConnectionStatus = findViewById(R.id.text_connection_status);
        tvProduct = findViewById(R.id.text_product_info);

        btnOpenVideoStreamActivity = findViewById(R.id.btn_open_video_stream_activity);
        btnOpenTest = findViewById(R.id.btn_open_test);

        btnOpenVideoStreamActivity.setOnClickListener(this);
        btnOpenTest.setOnClickListener(this);

        btnOpenVideoStreamActivity.setEnabled(false);
        btnOpenTest.setEnabled(false);
    }
}