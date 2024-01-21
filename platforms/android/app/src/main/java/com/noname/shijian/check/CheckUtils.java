package com.noname.shijian.check;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CheckUtils {

    private static final String[] SHALIST = new String[]{
            "CC3EE232E9A60DAF9F47A9E71F02F07148F8EFF87347F74F87D04BE548C64203",
            "A804D0723B4C211F561320BD60D79C40C66FCDC7B44FC2B856F3FFE36B0DBCB5"
    };

    public static void check(Context context, Executor executor){
        final String apkPath = context.getApplicationInfo().sourceDir;
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ZipFile zipFile = new ZipFile(apkPath);
                        ZipEntry zipEntry = zipFile.getEntry("AndroidManifest.xml");
                        InputStream inputStream = zipFile.getInputStream(zipEntry);
                        byte[] buffer = new byte[1024];
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            md.update(buffer, 0, bytesRead);
                        }
                        byte[] digestBytes = md.digest();
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digestBytes) {
                            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
                        }
                        String sha = sb.toString().toUpperCase(Locale.ROOT);
                        Log.e("sha", sha);
                        for(String s:SHALIST){
                            if(s.equals(sha)){
                                return;
                            }
                        }
                        System.exit(0);
                    }catch (Throwable e){
                        System.exit(0);
                    }
                }
            });
        }catch (Throwable e){
            System.exit(0);
        }
    }

}
