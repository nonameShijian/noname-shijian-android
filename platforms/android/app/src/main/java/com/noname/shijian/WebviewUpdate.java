package com.noname.shijian;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.UpgradeOptions;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.download.DownloadAction;

import org.apache.cordova.LOG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WebviewUpdate extends Activity {
    private static final String TAG = "WebviewUpdate";

    private static boolean inited = false;

    private ProgressDialog WebViewUpgradeProgressDialog;

    private String data;

    private void ActivityOnCreate() {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog = null;
        }
        Intent intent = new Intent(this, MainActivity.class);
        if (data != null) {
            intent.putExtra("extensionImport", data);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String ext = extras.getString("extensionImport");
            if (ext != null) data = ext;
        }

        if (inited) {
            ActivityOnCreate();
        } else {
            inited = true;

            if (WebViewUpgradeProgressDialog == null) {
                WebViewUpgradeProgressDialog = new ProgressDialog(this);
                WebViewUpgradeProgressDialog.setTitle("正在更新Webview内核");
                WebViewUpgradeProgressDialog.setCancelable(false);
                WebViewUpgradeProgressDialog.setIndeterminate(false);
                WebViewUpgradeProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                WebViewUpgradeProgressDialog.setMax(100);
                WebViewUpgradeProgressDialog.setProgress(0);
            }

//        AlertDialog alertDialog = new AlertDialog.Builder(this)
//                .setTitle("无名杀")
//                .setMessage("Webview更新完成，请关闭并重启app")
//                .setCancelable(false)
//                .setIcon(R.mipmap.ic_launcher)
//                .setPositiveButton("确定", (DialogInterface dialog, int which) -> {
//                    finish();
//                })
//                .create();


            WebViewUpgrade.addUpgradeCallback(new UpgradeCallback() {
                @Override
                public void onUpgradeProcess(float percent) {
                    WebViewUpgradeProgressDialog.setProgress((int) percent);
                }

                @Override
                public void onUpgradeComplete() {
                    Log.e(TAG, "onUpgradeComplete");
                    WebViewUpgradeProgressDialog.setProgress(100);
                    if (WebViewUpgradeProgressDialog.isShowing()) WebViewUpgradeProgressDialog.hide();
                    ActivityOnCreate();
                }

                @Override
                public void onUpgradeError(Throwable throwable) {
                    Log.e(TAG, "onUpgradeError: " + throwable.getMessage());
                    WebViewUpgradeProgressDialog.setProgress(0);
                    if (WebViewUpgradeProgressDialog.isShowing()) WebViewUpgradeProgressDialog.hide();
                    ActivityOnCreate();
                }
            });

            if (!WebViewUpgradeProgressDialog.isShowing()) WebViewUpgradeProgressDialog.show();

            // 添加webview
            // com.google.android.webview_118.0.5993.65
            WebViewUpgrade.upgrade(new UpgradeOptions
                    .Builder(getApplicationContext(),
                    "com.google.android.webview",
                    "asset:com.google.android.webview_119.0.6045.194.apk",
                    "119.0.6045.194",
                    (url, path) -> new DownloadActionImpl(url, path, this,
                            "com.google.android.webview",
                            "119.0.6045.194"
                    ))
                    .build());

            // com.huawei.webview_12.1.2.322
//            WebViewUpgrade.upgrade(new UpgradeOptions
//                    .Builder(getApplicationContext(),
//                    "com.huawei.webview",
//                    "asset:com.huawei.webview_12.1.2.322.apk",
//                    "12.1.2.322",
//                    new DownloadSink() {
//                        @Override
//                        public DownloadAction createDownload(String url, String path) {
//                            return null;
//                        }
//                    })
//                    .build());
        }

    }

    @Override
    protected void onDestroy() {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog = null;
        }
        super.onDestroy();
    }

    private static class DownloadActionImpl implements DownloadAction {
        private final String url;
        private final String path;
        private boolean finish = false;
        private final Thread thread;
        private final List<Callback> callbackList = new ArrayList<>();
        private final Context context;
        private final String packageName;
        private final String version;
        public DownloadActionImpl(String url, String path, Context context,
                                  String packageName, String version) {
            if (url.startsWith("asset:")) {
                this.url  = url.substring("asset:".length());
            } else {
                this.url  = url;
            }
            this.context = context;
            this.path = path;
            this.packageName = packageName;
            this.version = version;
            thread = new Thread(() -> {
                try {
                    Log.e(TAG, "开始线程");
                    InputStream fis = context.getAssets().open(this.url);
                    int count = fis.available();
                    File ret = new File(this.path);
                    if(ret.exists()) {
                        ret.delete();
                    } else {
                        ret.getParentFile().mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(ret);
                    byte[] buffer = new byte[4096];
                    int readLength;
                    while ((readLength = fis.read(buffer))!=-1){
                        if (count > 0) {
                            int avail_bytes = fis.available();
                            float percentage = (count - avail_bytes) / (float) count * 100;
                            for (Callback callback : callbackList) {
                                callback.onProcess(percentage);
                            }
                        }
                        fos.write(buffer,0,readLength);
                    }
                    fos.close();
                    fis.close();
                    for (Callback callback : callbackList) {
                        callback.onProcess(100);
                        callback.onComplete(this.path);
                    }
                    Log.e(TAG, "onComplete");
                } catch (Exception e) {
                    for (Callback callback : callbackList) {
                        callback.onFail(e);
                    }
                    Log.e(TAG, "Exception:" + e.getMessage());
                } finally {
                    finish = true;
                }
            });
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public void start() {
            thread.start();
        }

        @Override
        public void stop() {
            // thread.stop();
        }

        @Override
        public void delete() {
            File ret = new File(this.path);
            if(ret.exists()) {
                ret.delete();
            }
        }

        @Override
        public boolean isCompleted() {
            File dir = new File(context.getFilesDir(), "WebViewUpgrade/" + packageName + "/" + version);
            if (dir.exists() && dir.isDirectory()) return true;
            return finish;
        }

        @Override
        public boolean isProcessing() {
            return !this.isCompleted();
        }

        @Override
        public void addCallback(Callback callback) {
            if (callbackList.contains(callback)) {
                return;
            }
            callbackList.add(callback);
        }

        @Override
        public void removeCallback(Callback callback) {
            if (!callbackList.contains(callback)) {
                return;
            }
            callbackList.remove(callback);
        }
    }
}