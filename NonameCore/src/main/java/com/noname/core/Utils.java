package com.noname.core;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import 	android.content.pm.PackageManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String TAG = "Utils";

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

    /**
     * Ipv4 address check.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     *
     * @return True if the input parameter is a valid IPv4 address.
     */
    public static boolean isIPv4Address(String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /**
     * Get local Ip address.
     */
    public static InetAddress getLocalIPAddress() {
        Enumeration<NetworkInterface> enumeration = null;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                NetworkInterface nif = enumeration.nextElement();
                Enumeration<InetAddress> inetAddresses = nif.getInetAddresses();
                if (inetAddresses != null) {
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (!inetAddress.isLoopbackAddress() && isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean captureAndSaveScreenshot(Activity activity, String fileName) {
        try {
            View dView = activity.getWindow().getDecorView();
            // 清除DrawingCache
            dView.setDrawingCacheEnabled(false);
            dView.destroyDrawingCache();
            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                    dView.getWidth(),
                    dView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            // 使用Canvas绘制视图到Bitmap中
            Canvas canvas = new Canvas(bitmap);
            dView.draw(canvas);
            if (bitmap != null) {
                // 获取应用图标和名称
                ApplicationInfo appInfo = activity.getApplicationInfo();
                Drawable appIcon = activity.getPackageManager().getApplicationIcon(appInfo);
                String appName = activity.getPackageManager().getApplicationLabel(appInfo).toString();

                // todo 添加水印
                // 将图标转换为固定大小的Bitmap
                int iconSizeInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, activity.getResources().getDisplayMetrics());
                Bitmap iconBitmap = Bitmap.createBitmap(iconSizeInPx, iconSizeInPx, Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(iconBitmap);
                appIcon.setBounds(0, 0, iconSizeInPx, iconSizeInPx);
                appIcon.draw(tempCanvas);

                // 添加图标和名称水印
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.WHITE); // 文字颜色
                paint.setTextSize(50); // 文字大小
                Rect textBounds = new Rect();
                paint.getTextBounds(appName, 0, appName.length(), textBounds);

                // 计算图标和文本的位置，避免重叠
                int padding = 10; // 边距
                int iconPadding = 10; // 图标与文字之间的间距

                int textWidth = textBounds.width();
                int textHeight = textBounds.height();
                int iconHalfHeight = iconSizeInPx / 2;
                int textHalfHeight = textHeight / 2;

                // 计算垂直居中的位置
                int centerY = bitmap.getHeight() - padding - Math.max(iconHalfHeight, textHalfHeight);

                // 图标的位置
                int iconX = bitmap.getWidth() - textWidth - iconSizeInPx - padding - iconPadding;
                int iconY = centerY - iconHalfHeight;

                // 文本的位置
                int textX = bitmap.getWidth() - textWidth - padding;
                int textY = centerY - textHalfHeight;

                // 绘制应用图标
                canvas.drawBitmap(iconBitmap, iconX, iconY, paint);

                // 绘制应用名称
                canvas.drawText(appName, textX, textY + textHeight, paint);

                // 插入到MediaStore中
                Uri imageUri = insertImage(activity, fileName, bitmap);
                if (imageUri != null) {
                    // 显示带有截图预览的PopupWindow
                    showScreenshotToastWithPreview(activity, bitmap, imageUri);
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing and saving screenshot.", e);
        }
        return false;
    }

    private static Uri insertImage(Context context, String fileName, Bitmap bitmap) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        byte[] imageBytes = bytes.toByteArray();

        ContentValues values = new ContentValues();
        // 设置文件名
        String uniqueFileName = fileName + ".png";
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        // 设置到DCIM目录下
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/" + context.getPackageName());
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (item != null) {
            OutputStream imageOutStream = contentResolver.openOutputStream(item);
            if (imageOutStream != null) {
                imageOutStream.write(imageBytes);
                imageOutStream.close();
            }
        }
        return item;
    }

    private static void showScreenshotToastWithPreview(Context context, Bitmap screenshot, Uri imageUri) {
        // 创建一个PopupWindow来显示截图
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.toast_layout, null);

        ImageView imageIcon = layout.findViewById(R.id.app_icon);
        int mipmapResId = context.getResources().getIdentifier("ic_launcher_round", "mipmap", context.getPackageName());
        if (mipmapResId != 0) {
            imageIcon.setImageResource(mipmapResId);
        }

        TextView appName = layout.findViewById(R.id.app_name);
        int stringResId = context.getResources().getIdentifier("app_name", "string", context.getPackageName());
        if (stringResId != 0) {
            appName.setText(context.getString(stringResId));
        }

        ImageView imageView = layout.findViewById(R.id.popup_image);
        Button shareButton = layout.findViewById(R.id.btn_share);

        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            context.startActivity(Intent.createChooser(shareIntent, "分享截图"));
        });

        // 计算适合屏幕的预览大小
        int screenWidth = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
        int screenHeight = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();

        int targetWidth = (int) (screenWidth * 0.6f); // 调整宽度以适应屏幕
        int targetHeight = (int) (targetWidth * (float) screenshot.getHeight() / (float) screenshot.getWidth()); // 保持比例

        // 缩放截图
        Bitmap resizedScreenshot = Bitmap.createScaledBitmap(screenshot, targetWidth, targetHeight, false);
        imageView.setImageBitmap(resizedScreenshot);

        // 设置ImageView的大小
        imageView.getLayoutParams().width = targetWidth;
        imageView.getLayoutParams().height = targetHeight;

        PopupWindow popup = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background));
        popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

        // 设置一个延时关闭PopupWindow
        new Handler().postDelayed(() -> {
            popup.dismiss();
        }, 3000); // 延迟2秒关闭PopupWindow
    }
}