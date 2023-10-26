package com.noname.shijian.tbs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.tencent.smtt.sdk.QbSdk;

public class X5ProcessInitService extends Service {

    private static final String TAG = "X5ProcessInitService";

    @Override
    public void onCreate() {
        /* 只进行本地内核的预加载、不做版本检测及内核下载 */
        QbSdk.preInit(this.getApplicationContext(), new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {

            }

            @Override
            public void onViewInitFinished(boolean b) {
                Log.i(TAG, "init web process x5: " + b);
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service OnBind");
        return null;
    }
}
