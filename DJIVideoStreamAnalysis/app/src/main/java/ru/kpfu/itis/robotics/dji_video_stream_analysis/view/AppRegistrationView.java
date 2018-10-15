package ru.kpfu.itis.robotics.dji_video_stream_analysis.view;

/**
 * Created by valera071998@gmail.com on 25.04.2018.
 */
public interface AppRegistrationView {

    void showProductConnectionSuccess(String productName);

    void showProductConnectionError();

    void showAppRegistrationSuccess();

    void showAppRegistrationError();

    void showToast(String message);
}