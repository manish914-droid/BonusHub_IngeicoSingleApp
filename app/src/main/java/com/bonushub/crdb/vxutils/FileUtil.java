package com.bonushub.crdb.vxutils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    /**
     * 批量拷贝
     * @param context
     * @param path sd 卡存放目录
     * @param fileNames aseert 下文件名
     */
    public static void copyFilesToSD(Context context, String path, String[] fileNames){
        if (fileNames == null) {
            return;
        }
        for (String fileName : fileNames) {
            copyFileToSD(context, path, fileName);
        }
    }

    /**
     *
     * @param context
     * @param path sd 卡存放目录
     * @param assertFileName aseert 下文件名
     */
    public static void copyFileToSD(Context context, String path, String assertFileName){
        if (assertFileName == null) {
            return;
        }

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = context.getAssets().open(assertFileName);
            File parent = new File(path);
            if (!parent.exists()) {
                parent.mkdirs();
            }

            File file = new File(path, assertFileName);
            if(file.exists()) {
                return;
            }

            outputStream = new FileOutputStream(file);
            int len;
            byte[] buffer = new byte[1024];
            while((len = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存数据到文件
     * 同名文件，覆盖输入
     */
    public static void saveDataToFile(String path, String fileName, byte[] data) {
        FileOutputStream outputStream = null;
       try {
           File parent = new File(path);
           if (!parent.exists()) {
               parent.mkdirs();
           }

           File file = new File(path, fileName);
           outputStream = new FileOutputStream(file);
           outputStream.write(data);
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           try {
               if (outputStream != null) {
                   outputStream.close();
               }
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }
}
