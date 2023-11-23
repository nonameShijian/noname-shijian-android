package com.noname.shijian;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public interface ByteCallback{
        void onProgress(long bytes);
    }

    public static String getRandomString(int length){
        String pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        char[] cs = new char[length];
        for(int i=0;i<cs.length;i++){
            cs[i] = pool.charAt((int)(Math.random()*pool.length()));
        }
        return new String(cs);
    }

    public static void inputStreamToFile(InputStream inputStream,File file,long unit,ByteCallback callback) throws Exception{
        if(file.exists()){
            file.delete();
        }
        byte[] buffer = new byte[4096];
        int readLength = -1;
        FileOutputStream fos = new FileOutputStream(file);
        long totalLength = 0;
        long part = 0;
        while ((readLength = inputStream.read(buffer))!=-1){
            fos.write(buffer,0,readLength);
            totalLength += readLength;
            long newPart = totalLength/unit;
            if(newPart != part){
                part = newPart;
                if(callback!=null){
                    callback.onProgress(totalLength);
                }
            }
        }
        fos.close();
        inputStream.close();
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

    private static Pattern messyFilterPattern = Pattern.compile("\\s*|\t*|\r*|\n*");

    /**
     * 判断字符串是否包含乱码
     * @param strName  需要判断的字符串
     * @return 字符串包含乱码则返回true, 字符串不包含乱码则返回false
     */
    public static boolean isMessyCode(String strName) {
        Pattern p = messyFilterPattern;
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = 0 ;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!isCertainlyNotMessyCode(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
                chLength++;
            }
        }
        float result = count / chLength ;
        return result > 0.4;
    }

    private static boolean isCertainlyNotMessyCode(char c){
        if(Character.isLetterOrDigit(c))return true;
        return "!@#$%^&*()-_+/`~\\|[]{};:\",.<>/".contains(c+"");
    }

    /**
     * 判断字符是否为中文
     * @param c 字符
     * @return 字符是中文返回 true, 否则返回false
     */
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

}