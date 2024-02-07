package com.noname.shijian.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.yanzhenjie.andserver.annotation.Config;
import com.yanzhenjie.andserver.framework.config.Multipart;
import com.yanzhenjie.andserver.framework.config.WebConfig;
import com.yanzhenjie.andserver.framework.website.AssetsWebsite;
import com.yanzhenjie.andserver.framework.website.StorageWebsite;

import java.io.File;

@Config
public class AppConfig implements WebConfig {

    @Override
    public void onConfig(Context context, Delegate delegate) {
        File root = context.getExternalFilesDir(null).getParentFile();

        if (!root.exists()) {
            boolean result = root.mkdirs();
            Log.e("AppConfig-mkdirs", String.valueOf(result));
        } else {
            Log.e("AppConfig", "exists");
        }

        // 上传文件
        delegate.setMultipart(Multipart.newBuilder()
                .allFileMaxSize(1024 * 1024 * 20) // 20M
                .fileMaxSize(1024 * 1024 * 5) // 5M
                .maxInMemorySize(1024 * 10) // 1024 * 10 bytes
                .uploadTempDir(new File(root, "_server_upload_cache_"))
                .build());

        StorageWebsite sdWebsite = new StorageWebsite(root.getAbsolutePath());
        Log.e("AppConfig-sdcard", root.toString());
        // 增加一个静态网站
        // 同时生效，但文件以第一个有的为准

        AssetsWebsite asWebsite = new AssetsWebsite(context, "/www");
        delegate.addWebsite(asWebsite);

        delegate.addWebsite(sdWebsite);
    }
}