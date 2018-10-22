package ru.kpfu.itis.robotics.djivideostreamanalysis.presenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.thirdparty.afinal.core.AsyncTask;
import ru.kpfu.itis.robotics.djivideostreamanalysis.DJIApplication;
import ru.kpfu.itis.robotics.djivideostreamanalysis.util.media.DJIVideoStreamDecoder;
import ru.kpfu.itis.robotics.djivideostreamanalysis.util.media.NativeHelper;
import ru.kpfu.itis.robotics.djivideostreamanalysis.util.VideoDataUtil;
import ru.kpfu.itis.robotics.djivideostreamanalysis.view.VideoStreamView;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public class VideoStreamPresenter implements TextureView.SurfaceTextureListener, SurfaceHolder.Callback, DJICodecManager.YuvDataCallback {
    public static final String TAG = VideoStreamPresenter.class.getName();

    // Video streaming stuff
    private TextureView tvLivestreamPreview;
//    private SurfaceView svLivestreamPreview;
//    private SurfaceHolder shLivestreamPreview;

    private long lastUpdate;
    private long count;
    private int videoViewWidth;
    private int videoViewHeight;

    private VideoFeeder.VideoFeed standardVideoFeeder;
    private VideoFeeder.VideoDataCallback videoDataCallback;
    private DJICodecManager djiCodecManager = null; // DJI Mobile SDK implementation of codec

    private VideoDataUtil videoDataUtil;    // Writing content of H.264 frames
    // Video streaming stuff

    private VideoStreamView view;
    private Activity activity;
    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            view.updateConnectionStatus();
            onProductChange();
        }
    };

    private BaseProduct product;

    private LocationCoordinate3D currentLocation;

    public VideoStreamPresenter(final VideoStreamView view, final Activity activity) {
        Log.d(TAG, "VideoStreamPresenter(). view: " + view + ", activity: " + activity);
        this.view = view;
        this.activity = activity;

        registerConnectionChangeReceiver();
        initFlightController();
        getCameraSettings();
    }

    private void registerConnectionChangeReceiver() {
        Log.d(TAG, "registerConnectionChangeReceiver(). connectionChangeReceiver : " + connectionChangeReceiver);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(connectionChangeReceiver, filter);
    }

    // Adjust FlightController to get aircraft state
    private void initFlightController() {
        Log.d(TAG, "initFlightController().");

        FlightController flightController = DJIApplication.getFlightController();
        if (flightController != null) {
            // set flight controller state listener
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    Log.d(TAG, "FlightController.onUpdate(). flightControllerState: " + flightControllerState);

                    currentLocation = flightControllerState.getAircraftLocation();
                    // update UI
                    view.updateAircraftLocation(
                            currentLocation.getLongitude(),
                            currentLocation.getLatitude(),
                            currentLocation.getAltitude()
                    );
                }
            });
        }
    }

    private void getCameraSettings() {
        Camera camera = DJIApplication.getCameraInstance();
        if (camera != null) {
            camera.getVideoResolutionAndFrameRate(new CommonCallbacks.CompletionCallbackWith<ResolutionAndFrameRate>() {
                @Override
                public void onSuccess(ResolutionAndFrameRate resolutionAndFrameRate) {
                    if (resolutionAndFrameRate != null) {
                        // resolution
                        SettingsDefinitions.VideoResolution resolution = resolutionAndFrameRate.getResolution();
                        if (resolution != null) {
                            // RESOLUTION_4096x2160(8, 22, 4)
                            String resolutionSettings = String.format(
                                    "Cmd value: %d. Ratio: %d. Value: %d",
                                    resolution.cmdValue(), resolution.ratio(), resolution.value());
                            view.setCameraSettingsResolution(resolutionSettings);
                        } else {
                            view.setCameraSettingsResolution("Resolution: null");
                        }

                        // frame rate
                        SettingsDefinitions.VideoFrameRate frameRate = resolutionAndFrameRate.getFrameRate();
                        if (frameRate != null) {
                            // FRAME_RATE_29_DOT_970_FPS(3, 3). The camera's video frame rate is 29.97fps (frames per second).
                            String frameRateSettings = String.format(
                                    "Cmd value: %d. Value: %d",
                                    frameRate.cmdValue(), frameRate.value());
                            view.setCameraSettingsFrameRate(frameRateSettings);
                        } else {
                            view.setCameraSettingsFrameRate("Frame Rate: null");
                        }
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {
                    if (djiError != null) {
                        view.setCameraSettingsError(djiError.getDescription());
                    } else {
                        view.setCameraSettingsError("DJI Error: null");
                    }
                }
            });
        }
    }

    public void initVideoDataHandler() {
//        svLivestreamPreview = view.getLivestreamPreviewSurfaceView();
//        if (svLivestreamPreview != null) {
//            svLivestreamPreview.getHolder().addCallback(this);
//        }

        tvLivestreamPreview = view.getLivestreamPreviewTextureView();
        if (tvLivestreamPreview != null) {
            tvLivestreamPreview.setSurfaceTextureListener(this);
        }

        // The callback for receiving the raw H264 video data for camera live view
        // https://developer.dji.com/api-reference/android-api/Components/Camera/DJICamera.html?search=videodatacall&i=1&#djicamera_camerareceivedvideodatacallbackinterface_inline
        videoDataCallback = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, final int size) {
                Log.d(TAG, "VideoFeeder.VideoDataCallback.onReceive(). " +
                        "videoBuffer: " + videoBuffer + ", size: " + size);
                view.videoDataCallbackOnReceive(String.valueOf(size));

                if (System.currentTimeMillis() - lastUpdate > 1000) {
                    lastUpdate = System.currentTimeMillis();
                }

                /**
                 we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                 * on surfaceView
                 */
                DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
            }
        };
    }

    public void onProductChange() {
        Log.d(TAG, "onProductChange().");
        view.updateConnectionStatus();
        initVideoDataHandler();

        try {
            product = DJIApplication.getProductInstance();
        } catch (Exception exception) {
            product = null;
        }

        if (product == null || !product.isConnected()) {
            Log.d(TAG, "Product :" + product + " is disconnected.");
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                Camera camera = product.getCamera();
                camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            view.setCameraModeError("Can't change mode of camera, error:" + djiError.getDescription());
                        }
                    }
                });

                if (VideoFeeder.getInstance() != null) {
                    standardVideoFeeder = VideoFeeder.getInstance().provideTranscodedVideoFeed();
                    standardVideoFeeder.setCallback(videoDataCallback);
                    view.videoFeedSetCallback(
                            VideoFeeder.getInstance().provideTranscodedVideoFeed().getVideoSource() != null ?
                                    String.valueOf(VideoFeeder.getInstance().provideTranscodedVideoFeed().getVideoSource().name()) : "null"
                    );
                }
            }
        }
    }

    public void initVideoSurface() {
        Log.d(TAG, "initVideoSurface().");

        if (tvLivestreamPreview != null) {
            tvLivestreamPreview.setSurfaceTextureListener(this);
        }
//        if (svLivestreamPreview != null) {
//            shLivestreamPreview = svLivestreamPreview.getHolder();
//            shLivestreamPreview.addCallback(this);
//        }
    }

    public void uninitVideoSurface() {
        Log.d(TAG, "uninitVideoSurface().");
        if (DJIApplication.getCameraInstance() != null) {
            // Reset the callback
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
            }
            if (standardVideoFeeder != null) {
                standardVideoFeeder.setCallback(null);
            }
        }
    }

    public void destroy() {
        if (djiCodecManager != null) {
            djiCodecManager.cleanSurface();
            djiCodecManager.destroyCodec();
        }
    }

//-------------------------------------- TextureView listener ------------------------------------//
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        videoViewWidth = width;
        videoViewHeight = height;
        if (djiCodecManager == null) {  // attach DJICodecManager to surface
            djiCodecManager = new DJICodecManager(
                    activity,
                    surfaceTexture,
                    width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        videoViewWidth = width;
        videoViewHeight = height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        // DJI Mobile SDK implementation
        if (djiCodecManager != null) {  // clean the surface from DJICodecManager
            djiCodecManager.cleanSurface();
        }
        return false;
    }

//------------------------------------ SurfaceHolder callback ------------------------------------//
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
//        videoViewWidth = svLivestreamPreview.getWidth();
//        videoViewHeight = svLivestreamPreview.getHeight();

        // This demo might not work well on P3C and OSMO.
        NativeHelper.getInstance().init();
        DJIVideoStreamDecoder.getInstance().init(activity, surfaceHolder.getSurface());
        DJIVideoStreamDecoder.getInstance().resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        videoViewWidth = width;
        videoViewHeight = height;

        DJIVideoStreamDecoder.getInstance().changeSurface(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        DJIVideoStreamDecoder.getInstance().stop();
        NativeHelper.getInstance().release();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

//------------------------------------------ YuvData callback ------------------------------------//
    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        //In this demo, we test the YUV data by saving it into JPG files.
        //DJILog.d(TAG, "onYuvDataReceived " + dataSize);
        if (count++ % 30 == 0 && yuvFrame != null) {
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);
            //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    saveYuvDataToJPEG(bytes, width, height);
                }
            });
        }
    }

    private void saveYuvDataToJPEG(byte[] yuvFrame, int width, int height) {
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }
        Log.d(TAG,
                "onYuvDataReceived: frame index: "
                        + DJIVideoStreamDecoder.getInstance().frameIndex
                        + ",array length: "
                        + bytes.length);
        screenShot(bytes, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);
    }

    /**
     * Save the buffered data into a JPG image file
     */
    private void screenShot(byte[] buf, String shotDir, int width, int height) {
        File dir = new File(shotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        YuvImage yuvImage = new YuvImage(buf,
                ImageFormat.NV21,
                width,
                height,
                null);
        OutputStream outputFile;
        final String path = dir + "/ScreenShot_" + System.currentTimeMillis() + ".jpg";
        try {
            outputFile = new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "test screenShot: new bitmap output file error: " + e);
            return;
        }
        if (outputFile != null) {
            yuvImage.compressToJpeg(new Rect(0,
                    0,
                    width,
                    height), 100, outputFile);
        }
        try {
            outputFile.close();
        } catch (IOException e) {
            Log.e(TAG, "test screenShot: compress yuv image error: " + e);
            e.printStackTrace();
        }
    }

    private void handleYUVClick() {
//        switch (demoType) {
//            case USE_TEXTURE_VIEW:
//            case USE_SURFACE_VIEW:
//                mCodecManager.enabledYuvData(true);
//                mCodecManager.setYuvDataCallback(this);
//                break;
//            case USE_SURFACE_VIEW_DEMO_DECODER:
//                DJIVideoStreamDecoder.getInstance().changeSurface(null);
//                DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
//                break;
//        }
    }
}