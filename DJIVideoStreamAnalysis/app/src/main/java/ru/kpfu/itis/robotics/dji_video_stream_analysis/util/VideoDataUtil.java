package ru.kpfu.itis.robotics.dji_video_stream_analysis.util;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by valera071998@gmail.com on 12.07.2018.
 */
public class VideoDataUtil {

    public static final String PACKAGE_NAME = "dji_camera_h264";

    public static final String FILE_NAME_DATA_UNIT = "video_data";
    public static final String FILE_NAME_DATA_UNIT_SIZE = "video_data_size";

    public static final String FILE_NAME_DATA_EXTENSION = ".h264";
    public static final String FILE_NAME_DATA_UNIT_SIZE_EXTENSION = ".txt";

    public BufferedOutputStream outputStreamUnit;
    public DataOutputStream outputStreamUnitSize;

    public void initOutput() {
        try {
            // is SD available?
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            // getting path of SD, making directory
            File pathDirectoryOnSD = Environment.getExternalStorageDirectory();
            pathDirectoryOnSD = new File(pathDirectoryOnSD.getAbsolutePath() + "/" + PACKAGE_NAME);
            if (!pathDirectoryOnSD.exists()) {
                pathDirectoryOnSD.mkdirs();
            }

            String fileNameDataUnit = FILE_NAME_DATA_UNIT + FILE_NAME_DATA_EXTENSION;
            File sdFileDataUnit = new File(pathDirectoryOnSD, fileNameDataUnit);
            outputStreamUnit = new BufferedOutputStream(new FileOutputStream(sdFileDataUnit));

            String fileNameDataUnitSize = FILE_NAME_DATA_UNIT_SIZE + FILE_NAME_DATA_UNIT_SIZE_EXTENSION;
            File sdFileDataUnitSize = new File(pathDirectoryOnSD, fileNameDataUnitSize);
            outputStreamUnitSize = new DataOutputStream(new FileOutputStream(sdFileDataUnitSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeUnit(byte[] videoBuffer, int size) {
        try {
            outputStreamUnit.write(videoBuffer);
            outputStreamUnit.flush();

            outputStreamUnitSize.writeInt(size);
            outputStreamUnitSize.writeUTF("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeOutput() {
        try {
            outputStreamUnit.close();
            outputStreamUnitSize.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}