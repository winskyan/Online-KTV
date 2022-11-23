package io.agora.baselibrary.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileUtils {
    private static final String TAG = "FileUtils";

    // 将字符串写入到文本文件中
    public static void writeTxtToFile(String content, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFile(filePath, fileName);
        String mFilePath = filePath + fileName;
        // 每次写入时，都换行写
        String mContent = content + "\r\n";
        try {
            File file = new File(mFilePath);
            if (file.exists()) {
                file.delete();
            }
            file.getParentFile().mkdirs();
            file.createNewFile();

            RandomAccessFile mRandomAccessFile = new RandomAccessFile(file, "rwd");
            mRandomAccessFile.seek(file.length());
            mRandomAccessFile.write(mContent.getBytes());
            mRandomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //生成文件
    public static File makeFile(String filePath, String fileName) {
        File file = null;
        makeDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    //生成文件夹
    public static void makeDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
