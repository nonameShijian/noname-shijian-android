package com.noname.shijian.check;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.noname.shijian.UpdateDataApplication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Executor;

public class CheckUtils {

    private static LinearLayout linearLayout;

    private static TextView textView;

    private static final byte[][] BYTES = new byte[][]{
            {-117, 61, -81, 38, 79, 104, -123, -99, -1, -56, -117, -19, -66, -72, 3, -97, -18, 110, 19, -28},
            {83, -29, -92, -55, 84, -1, 48, -6, 30, -11, -82, 13, -52, -121, 2, -51, 30, 90, 84, -36},
            {-70, 4, 49, -92, 59, 90, -60, -127, -32, 48, 54, 40, -116, -111, -9, -7, -30, 96, 127, -11}
    };

    private static final byte[] BYTE = new byte[] {98, 105, 110, 46, 109, 116, 46, 115, 105, 103, 110, 97, 116, 117, 114, 101, 46, 75, 105, 108, 108, 101, 114, 65, 112, 112, 108, 105, 99, 97, 116, 105, 111, 110};

    public static byte[] g(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (packageInfo.signatures == null) return null;
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return md.digest();
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void check(Context context, Executor executor) {
        executor.execute(() -> {
            Application app = (Application) context.getApplicationContext();
            if (!(app instanceof UpdateDataApplication) || UpdateDataApplication.class.getSuperclass() != Application.class) {
                setText(context);
            }
            try {
                Class.forName(new String(BYTE));
                setText(context);
            } catch (ClassNotFoundException ignored) {
            }
            try {
                byte[] si = g(context, context.getPackageName());
                if (si != null) {
                    for (byte[] s : BYTES) {
                        if (Arrays.equals(si, s)) {
                            return;
                        }
                    }
                    setText(context);
                }
            } catch (Throwable e){
                setText(context);
            }
        });
    }

    private static void setText(Context context) {
        // 创建一个垂直方向的 LinearLayout 作为容器
        if (linearLayout == null) {
            linearLayout = new LinearLayout(context);
        }
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        // 设置 LinearLayout 的布局参数
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(layoutParams);
        if (textView == null) {
            textView = new TextView(context);
        }
        textView.setText(-1);
        // 将 TextView 添加到 LinearLayout 中
        linearLayout.addView(textView);
        // 将 LinearLayout 设置为 Activity 的内容视图
        Activity activity = (Activity) context;
        activity.setContentView(linearLayout);
    }
}
