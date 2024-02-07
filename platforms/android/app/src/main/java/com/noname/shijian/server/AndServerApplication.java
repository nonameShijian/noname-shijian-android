package com.noname.shijian.server;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.util.IOUtils;

import java.io.File;

public class AndServerApplication extends Application {
    private static AndServerApplication mInstance;

    private File mRootDir;

    @Override
    public void onCreate() {
        super.onCreate();

        if (mInstance == null) {
            mInstance = this;
            initRootPath(this);
        }
    }

    @NonNull
    public static AndServerApplication getInstance() {
        return mInstance;
    }

    @NonNull
    public File getRootDir() {
        return mRootDir;
    }

    private void initRootPath(Context context) {
        if (mRootDir != null) {
            return;
        }
        mRootDir = context.getExternalFilesDir(null).getParentFile();
        IOUtils.createFolder(mRootDir);
    }
}
