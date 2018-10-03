package ru.kpfu.itis.robotics.dji_video_stream_analysis.presenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.TextureView;

import java.nio.ByteBuffer;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import ru.kpfu.itis.robotics.dji_video_stream_analysis.DJIApplication;
import ru.kpfu.itis.robotics.dji_video_stream_analysis.util.VideoDataUtil;
import ru.kpfu.itis.robotics.dji_video_stream_analysis.view.VideoStreamView;

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

    // Catching H.264 frames
    private VideoDataUtil videoDataUtil;

    // Andorid SDK implementation of codec
    public static final String VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static final int[] VIDEO_RESOLUTION = new int[] {1920, 1000};
    private ByteBuffer videoByteBuffer; // buffer for DJI Camera data
    private int videoBufferSize;
    private MediaCodec androidCodec;

    private ActiveTrackMission activeTrackMission;

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

        videoDataUtil = new VideoDataUtil();
        videoDataUtil.initOutput();

        registerConnectionChangeReceiver();
        initFlightController();
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

                // catching videobuffers
                videoDataUtil.writeUnit(videoBuffer, size);

                // DJI Mobile SDK implementation
                if (djiCodecManager != null) {
                    djiCodecManager.sendDataToDecoder(videoBuffer, size);
                } else {

                }
            }
        };
//
//                // Android SDK implementation
//                // TODO tmp, файл до колбека. достать из вью
////                dataUnitsCount++;
////                if (dataUnitsCount <= 20) {
////                    VideoDataUtil.writeUnits(activity, dataUnitsCount, videoBuffer, size);
////                } else {
////                    activity.runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            Toast.makeText(activity, "Data was written.", Toast.LENGTH_SHORT).show();
////                            activity.finish();
////                            System.exit(0);
////                        }
////                    });
////                }
////
////                // TODO process SPS unit
////                if (videoBuffer == null) {
////                    videoByteBuffer = ByteBuffer.wrap(videoBuffer);
////                    videoBufferSize = size;
////                }
////
////                // decode
////                // asks for index of an input buffer to be filled with valid data
////                int inputIndex = androidCodec.dequeueInputBuffer(-1); // wait for buffer indefinitely
////                if (inputIndex >= 0) {
////                    ByteBuffer buffer = androidCodec.getInputBuffer(inputIndex);
////                    buffer.put(videoBuffer);
////                    // process frame
////                    androidCodec.queueInputBuffer(inputIndex, 0, videoBuffer.length, 0, 0);
////                }
////
////                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
////                // asks for index of an output buffer that has been successfully decoded
////                int outputIndex = androidCodec.dequeueOutputBuffer(info, 0);
////                if (outputIndex >= 0) {
////                    // returns output buffer to render surfa
////                    androidCodec.releaseOutputBuffer(outputIndex, true);
////                }
//            }
//        };

        // init flight controller to get aircraft location
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
        videoDataUtil.closeOutput();
    }

    //------------------------------------ Video Surface listener ------------------------------------//
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        // DJI Mobile SDK implementation
        if (djiCodecManager == null) {  // attach DJICodecManager to surface
            djiCodecManager = new DJICodecManager(
                    activity,
                    surfaceTexture,
                    width, height);
        }

        // TODO attach another decoder to surface
        // Android SDK implementation
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(activity, "onSurfaceTextureAvailable.", Toast.LENGTH_SHORT).show();
//            }
//        });

//        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIMETYPE,
//                width, height);
//        if (videoByteBuffer != null) {
//            // TODO use SPS unit
//            format.setByteBuffer("csd-0", videoByteBuffer); // H.264 / AVC codec specific packet for SPS
//            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, videoBufferSize);
//        }
//
//        try {
//            androidCodec = MediaCodec.createDecoderByType(VIDEO_MIMETYPE);
//            // as codec is configured with MediaFormat
//            androidCodec.configure(format, new Surface(videoSurface.getSurfaceTexture()), null, 0);
//            // configuration buffers are sent automatically after .start()
//            androidCodec.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        // DJI Mobile SDK implementation
        if (djiCodecManager != null) {  // clean the surface from DJICodecManager
            djiCodecManager.cleanSurface();
        }

        // Android SDK implementation
//        if (androidCodec != null) {
//            androidCodec.stop();
//            androidCodec.release();
//        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}