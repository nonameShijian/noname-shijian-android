package com.noname.shijian.check;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CheckUtils {

    private static final String[] SHALIST = new String[]{
            "A804D0723B4C211F561320BD60D79C40C66FCDC7B44FC2B856F3FFE36B0DBCB5",
            "cc3ee232e9a60daf9f47a9e71f02f07148f8eff87347f74f87d04be548c64203"
    };

    private static final byte[][] SHALIST2 = new byte[][]{
            {99, 111, 109, 46, 110, 111, 110, 97, 109, 101, 46, 115, 104, 105, 106, 105, 97, 110},
            {99, 111, 109, 46, 110, 111, 110, 97, 109, 101, 46, 115, 104, 105, 106, 105, 97, 110, 46, 72, 85, 73},
            {99, 111, 109, 46, 110, 111, 110, 97, 109, 101, 46, 115, 104, 105, 106, 105, 97, 111},
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
                        // Log.e("sha", sha);
                        if (SHALIST.length == 0) throw new Exception("SHALIST.length == 0");
                        for(String s:SHALIST){
                            if(s.equals(sha)){
                                return;
                            }
                        }
                        if (SHALIST2.length == 0) throw new Exception("SHALIST2.length == 0");
                        for(byte[] s:SHALIST2){
                            if (context.getPackageName().equals(new String(s))) return;
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
