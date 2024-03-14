/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.noname.shijian;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.noname.shijian.check.CheckUtils;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.UpgradeOptions;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.download.DownloadAction;
import com.norman.webviewup.lib.util.ProcessUtils;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;


public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    public CordovaPreferences getPreferences() {
        return preferences;
    }

    private static boolean inited = false;

    private ProgressDialog WebViewUpgradeProgressDialog;

    private void ActivityOnCreate() {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog = null;
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        View view = appView.getView();
        Log.e("webview", String.valueOf(view));
        SystemWebView webview = (SystemWebView) view;
        WebSettings settings = webview.getSettings();
        initWebviewSettings(webview, settings);
        Log.e("getUserAgentString", settings.getUserAgentString());
        CheckUtils.check(this, Executors.newFixedThreadPool(5));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate", "111");
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        if (extras != null) {
            String ext = extras.getString("extensionImport");
            if (ext != null) {
                LOG.e("ext" ,ext);
                FinishImport.ext = ext;
            }
        }

        boolean is64Bit = ProcessUtils.is64Bit();
        String[] supportBitAbis = is64Bit ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;

        // 内置的apk只有这两种，如果都不包含，就不触发升级内核操作（例如: 虚拟机需要x86）
        int indexOfArm64 = Arrays.binarySearch(supportBitAbis,"arm64-v8a");
        int indexOfArmeabi = Arrays.binarySearch(supportBitAbis,"armeabi-v7a");

        if (inited || (indexOfArm64 < 0 && indexOfArmeabi < 0)) {
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
            // com.google.android.webview_119.0.6045.194
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

//            WebViewUpgrade.upgrade(new UpgradeOptions
//                    .Builder(getApplicationContext(),
//                    "com.android.chrome",
//                    "asset:com.android.chrome_122.0.6261.43.apk",
//                    "122.0.6261.43",
//                    (url, path) -> new DownloadActionImpl(url, path, this,
//                            "com.android.chrome",
//                            "122.0.6261.43"
//                    ))
//                    .build());
        }
    }

    private void initWebviewSettings(SystemWebView webview, WebSettings settings) {
        int textZoom = settings.getTextZoom();
        Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
        settings.setTextZoom(100);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");
        WebView.setWebContentsDebuggingEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LOG.e("onNewIntent" ,"111");
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getExtras() != null) {
            String ext = intent.getExtras().getString("extensionImport");
            if (ext != null) {
                LOG.e("ext" ,ext);
                FinishImport.ext = ext;
            }
        }
    }

    @Override
    public void onDestroy() {
        // 获取缓存目录
        File tempDir = getExternalCacheDir();
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
        }

        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog = null;
        }

        super.onDestroy();
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.e("onActivityResult-requestCode", String.valueOf(requestCode));
        Log.e("onActivityResult-resultCode", String.valueOf(resultCode));
        Log.e("onActivityResult-intent", String.valueOf(intent));
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri result = intent.getData();
                // 处理文件Uri对象
                Intent newIntent = new Intent(this, NonameImportActivity.class);
                newIntent.setData(result);
                newIntent.setAction(Intent.ACTION_VIEW);
                startActivity(newIntent);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
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
            if (ret.exists())  ret.delete();
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
            if (callbackList.contains(callback)) return;
            callbackList.add(callback);
        }

        @Override
        public void removeCallback(Callback callback) {
            if (!callbackList.contains(callback)) return;
            callbackList.remove(callback);
        }
    }
}