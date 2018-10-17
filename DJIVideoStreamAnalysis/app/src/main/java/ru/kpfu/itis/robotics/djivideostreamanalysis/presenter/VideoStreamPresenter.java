package ru.kpfu.itis.robotics.djivideostreamanalysis.presenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import ru.kpfu.itis.robotics.djivideostreamanalysis.DJIApplication;
import ru.kpfu.itis.robotics.djivideostreamanalysis.util.VideoDataUtil;
import ru.kpfu.itis.robotics.djivideostreamanalysis.view.VideoStreamView;

/**
 * Created by valera071998@gmail.com on 28.04.2018.
 */
public class VideoStreamPresenter implements TextureView.SurfaceTextureListener {

    public static final String TAG = VideoStreamPresenter.class.getName();

    private VideoStreamView view;
    private Activity activity;

    private BaseProduct product;

    private TextureView videoSurface;
    private VideoFeeder.VideoDataCallback videoDataCallback;
    // DJI Mobile SDK implementation of codec
    private DJICodecManager djiCodecManager = null;

    // Writing content of H.264 frames
    private VideoDataUtil videoDataUtil;

    // Andorid SDK implementation of codec
    public static final String VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static final int[] VIDEO_RESOLUTION = new int[] {4096, 2160};
    private ByteBuffer videoByteBuffer; // buffer for DJI Camera data
    private int videoBufferSize;
    private MediaCodec androidCodec;

    private BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            view.updateConnectionStatus();
            onProductChange();
        }
    };

    private LocationCoordinate3D currentLocation;

    public VideoStreamPresenter(final VideoStreamView view, final Activity activity) {
        Log.d(TAG, "VideoStreamPresenter(). view: " + view + ", activity: " + activity);
        this.view = view;
        this.activity = activity;

        // Writing content of H.264 frames
//        videoDataUtil = new VideoDataUtil();
//        videoDataUtil.initOutput();

        registerConnectionChangeReceiver();
        initFlightController();
        getCameraSettings();
        initVideoDataHandler();
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

    private void initVideoDataHandler() {
        videoSurface = view.getVideoSurface();
        if (videoSurface != null) {
            videoSurface.setSurfaceTextureListener(this);
        }

        // The callback for receiving the raw H264 video data for camera live view
        // https://developer.dji.com/api-reference/android-api/Components/Camera/DJICamera.html?search=videodatacall&i=1&#djicamera_camerareceivedvideodatacallbackinterface_inline
        videoDataCallback = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, final int size) {
                Log.d(TAG, "VideoFeeder.VideoDataCallback.onReceive(). " +
                        "videoBuffer: " + videoBuffer + ", size: " + size);
                view.videoDataCallbackOnReceive(String.valueOf(size));

                // Writing content of H.264 frames
//                videoDataUtil.writeUnit(videoBuffer, size);

                // DJI Mobile SDK implementation
//                if (djiCodecManager != null) {
//                    djiCodecManager.sendDataToDecoder(videoBuffer, size);
//                } else {
//
//                }

                // Android SDK implementation
                // TODO process SPS unit
                if (videoBuffer == null) {
                    videoByteBuffer = ByteBuffer.wrap(videoBuffer);
                    videoBufferSize = size; // ? Analyser gets 30716 NAL size
                }

                // decode
                // asks for index of an input buffer to be filled with valid data
                int inputIndex = androidCodec.dequeueInputBuffer(-1); // wait for buffer indefinitely
                view.videoDataCallbackDequeueInputBuffer(inputIndex);
                if (inputIndex >= 0) {
                    ByteBuffer buffer = androidCodec.getInputBuffer(inputIndex);
                    if (buffer != null)
                        buffer.put(videoBuffer);
                    // process frame
                    androidCodec.queueInputBuffer(inputIndex, 0, videoBufferSize, 0, 0);
                }

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                // asks for index of an output buffer that has been successfully decoded
                int outputIndex = androidCodec.dequeueOutputBuffer(info, 60000);
                view.videoDataCallbackDequeueOutputBuffer(outputIndex);
                if (outputIndex >= 0) {
                    // returns output buffer to render surface
                    androidCodec.releaseOutputBuffer(outputIndex, true);
                }
            }
        };
    }

    public void onProductChange() {
        Log.d(TAG, "onProductChange().");
        initVideoSurface();
    }

    public void initVideoSurface() {
        Log.d(TAG, "initVideoSurface().");

        try {
            product = DJIApplication.getProductInstance();
        } catch (Exception exception) {
            product = null;
        }

        if (product == null || !product.isConnected()) {
            Log.d(TAG, "Product :" + product + " is disconnected.");
        } else {
            if (videoSurface != null) {
                videoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                view.videoFeedSetCallback(
                        VideoFeeder.getInstance().getPrimaryVideoFeed().getVideoSource() != null ?
                                String.valueOf(VideoFeeder.getInstance().getPrimaryVideoFeed().getVideoSource().name()) : "null"
                );
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(videoDataCallback);
            }
        }
    }

    private void uninitVideoSurface() {
        Log.d(TAG, "uninitVideoSurface().");
        if (DJIApplication.getCameraInstance() != null) {
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }

    public void destroy() {
        uninitVideoSurface();
        if (djiCodecManager != null) {
            djiCodecManager.destroyCodec();
        }
        // Writing content of H.264 frames
//        videoDataUtil.closeOutput();
    }

//------------------------------------ Video Surface listener ------------------------------------//
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        // DJI Mobile SDK implementation
//        if (djiCodecManager == null) {  // attach DJICodecManager to surface
//            djiCodecManager = new DJICodecManager(
//                    activity,
//                    surfaceTexture,
//                    width, height);
//        }

        // Android SDK implementation
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIMETYPE, VIDEO_RESOLUTION[0], VIDEO_RESOLUTION[1]);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100000);
//        format.setInteger(MediaFormat.KEY_WIDTH, VIDEO_RESOLUTION[0]);
//        format.setInteger(MediaFormat.KEY_HEIGHT, VIDEO_RESOLUTION[1]);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);

        try {
            androidCodec = MediaCodec.createDecoderByType(VIDEO_MIMETYPE);
            // as codec is configured with MediaFormat
            androidCodec.configure(format, new Surface(videoSurface.getSurfaceTexture()), null, 0);
            // configuration buffers are sent automatically after .start()
            androidCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        // DJI Mobile SDK implementation
//        if (djiCodecManager != null) {  // clean the surface from DJICodecManager
//            djiCodecManager.cleanSurface();
//        }

        // Android SDK implementation
        if (androidCodec != null) {
            androidCodec.stop();
            androidCodec.release();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}