package com.noname.shijian;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Utils {

    public static String getRandomString(int length){
        String pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        char[] cs = new char[length];
        for(int i=0;i<cs.length;i++){
            cs[i] = pool.charAt((int)(Math.random()*pool.length()));
        }
        return new String(cs);
    }

    public static void inputStreamToFile(InputStream inputStream,File file) throws Exception{
        if(file.exists()){
            file.delete();
        }
        byte[] buffer = new byte[4096];
        int readLength = -1;
        FileOutputStream fos = new FileOutputStream(file);
        while ((readLength = inputStream.read(buffer))!=-1){
            fos.write(buffer,0,readLength);
        }
        fos.close();
        inputStream.close();
    }

    public static void copyFile(File file,File toFile) throws Exception{
        if(!toFile.getParentFile().exists()){
            toFile.getParentFile().mkdir();
        }
        if(!toFile.exists()){
            toFile.createNewFile();
        }
        byte[] buffer = new byte[4096];
        int readLength = -1;
        FileOutputStream fos = new FileOutputStream(toFile);
        FileInputStream inputStream = new FileInputStream(file);
        while ((readLength = inputStream.read(buffer))!=-1){
            fos.write(buffer,0,readLength);
        }
        fos.close();
        inputStream.close();
    }

    public static File assetToFile(String asset, Context context, String savedName){
        try {
            InputStream fis = context.getAssets().open(asset);
            // File cache = context.getCacheDir();
            // File ret = new File(cache,savedName);
            File data = context.getExternalFilesDir(null).getParentFile();
            File ret = new File(data, savedName);
            if(ret.exists()){
                ret.delete();
            }
            FileOutputStream fos = new FileOutputStream(ret);
            byte[] buffer = new byte[4096];
            int readLength = -1;
            while ((readLength = fis.read(buffer))!=-1){
                fos.write(buffer,0,readLength);
            }
            fos.close();
            fis.close();
            return ret;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}