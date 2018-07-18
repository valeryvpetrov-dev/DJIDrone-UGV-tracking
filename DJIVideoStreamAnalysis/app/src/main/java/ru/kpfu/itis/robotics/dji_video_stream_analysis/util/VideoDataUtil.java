package ru.kpfu.itis.robotics.dji_video_stream_analysis.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by valera071998@gmail.com on 12.07.2018.
 */
public class VideoDataUtil {

    public static final String PACKAGE_NAME = "dji_camera";

    public static final String FILE_NAME_DATA_UNIT = "video_data";
    public static final String FILE_NAME_DATA_UNIT_SIZE = "video_data_size";

    public static final String FILE_NAME_DATA_EXTENSION = ".h264";
    public static final String FILE_NAME_DATA_UNIT_SIZE_EXTENSION = ".txt";

    public static void writeUnits(Context context, int dataUnitsCount, byte[] videoBuffer, int size) {
        try {
            // is SD available?
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            // getting path of SD, making directory
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + PACKAGE_NAME);
            if (!sdPath.exists()) {
                sdPath.mkdirs();
            }

            // write data unit
            String fileNameDataUnit = FILE_NAME_DATA_UNIT + dataUnitsCount + FILE_NAME_DATA_EXTENSION;
            File sdFileDataUnit = new File(sdPath, fileNameDataUnit);
            FileOutputStream outputStreamUnit = new FileOutputStream(sdFileDataUnit);
            outputStreamUnit.write(videoBuffer);

            // write data unit size
            String fileNameDataUnitSize = FILE_NAME_DATA_UNIT_SIZE + dataUnitsCount + FILE_NAME_DATA_UNIT_SIZE_EXTENSION;
            File sdFileDataUnitSize = new File(sdPath, fileNameDataUnitSize);
            FileOutputStream outputStreamUnitSize = new FileOutputStream(sdFileDataUnitSize);
            outputStreamUnitSize.write(size);

            outputStreamUnit.close();
            outputStreamUnitSize.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}