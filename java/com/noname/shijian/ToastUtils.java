package com.noname.shijian;

import android.widget.Toast;
import android.content.Context;
import android.os.Looper;

public class ToastUtils {
    static Toast toast = null;
    public static void show(Context context, String text) {
        try {
            if(toast != null){
                toast.setText(text);
            } else {
                toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {
            //解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }
}