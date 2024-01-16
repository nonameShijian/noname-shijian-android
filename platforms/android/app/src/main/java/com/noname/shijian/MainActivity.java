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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.noname.shijian.tbs.X5ProcessInitService;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsCommonCode;
import com.tencent.smtt.sdk.TbsListener;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;
import org.jeremyup.cordova.x5engine.X5WebView;

import java.io.File;


public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    @Override
    public CordovaWebViewEngine makeWebViewEngine() {
        if (false) {
            initX5();
            return new org.jeremyup.cordova.x5engine.X5WebViewEngine(this, this.preferences);
        }
        return super.makeWebViewEngine();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate" ,"111");
        super.onCreate(savedInstanceState);
        super.init();
        startX5WebProcessPreinitService();

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

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        View view = appView.getView();
        Log.e("webview", String.valueOf(view));
        if (view instanceof org.jeremyup.cordova.x5engine.X5WebView) {
            X5WebView webview = (X5WebView) view;
            com.tencent.smtt.sdk.WebSettings settings = webview.getSettings();
            int textZoom = settings.getTextZoom();
            Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
            settings.setTextZoom(100);
            String userAgent = settings.getUserAgentString();
            settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
            webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");
            org.jeremyup.cordova.x5engine.X5WebView.setWebContentsDebuggingEnabled(true);
        } else {
            SystemWebView webview = (SystemWebView) view;
            WebSettings settings = webview.getSettings();
            int textZoom = settings.getTextZoom();
            Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
            settings.setTextZoom(100);
            String userAgent = settings.getUserAgentString();
            settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
            webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");
            WebView.setWebContentsDebuggingEnabled(true);
        }
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

    private ProgressDialog dialog;

    private void initX5() {
        Context context = this;
        dialog = new ProgressDialog(this);
        dialog.setTitle("正在下载X5内核");
        dialog.setCancelable(false);
        dialog.setIcon(R.mipmap.ic_launcher);
        // 水平进度条
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        /* 设置允许移动网络下进行内核下载。默认不下载，会导致部分一直用移动网络的用户无法使用x5内核 */
        QbSdk.setDownloadWithoutWifi(true);

        QbSdk.setCoreMinVersion(QbSdk.CORE_VER_ENABLE_202207);

        Log.e("initX5Environment", "initX5Environment");

        QbSdk.setTbsListener(new TbsListener() {
            /**
             * @param stateCode 用户可处理错误码请参考{@link TbsCommonCode}
             */
            @Override
            public void onDownloadFinish(int stateCode) {
                Log.i("App", "onDownloadFinished: " + stateCode);
                if (stateCode == TbsCommonCode.DOWNLOAD_SUCCESS) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.setTitle("正在安装内核");
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                }
            }

            /**
             * @param stateCode 用户可处理错误码请参考{@link TbsCommonCode}
             */
            @Override
            public void onInstallFinish(int stateCode) {
                Log.i("App", "onInstallFinished: " + stateCode);
                String m;
                if (stateCode == TbsCommonCode.INSTALL_SUCCESS) {
                    m = "内核安装完成";
                    Log.i("App", m);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                }
            }

            /**
             * 首次安装应用，会触发内核下载，此时会有内核下载的进度回调。
             * @param progress 0 - 100
             */
            @Override
            public void onDownloadProgress(int progress) {
                Log.i("App", "Core Downloading: " + progress);
                new Handler(Looper.getMainLooper()).post(() -> {
                    dialog.setProgress(progress);
                });
            }
        });

        /* 此过程包括X5内核的下载、预初始化，接入方不需要接管处理x5的初始化流程，希望无感接入 */
        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                // 内核初始化完成，可能为系统内核，也可能为系统内核
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖wifi网络下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * 内核下发请求发起有24小时间隔，卸载重装、调整系统时间24小时后都可重置
             * 调试阶段建议通过 WebView 访问 debugtbs.qq.com -> 安装线上内核 解决
             * @param isX5 是否使用X5内核
             */
            @Override
            public void onViewInitFinished(boolean isX5) {
                Log.i("onViewInitFinished", String.valueOf(isX5));
                // hint: you can use QbSdk.getX5CoreLoadHelp(context) anytime to get help.
                Log.i("getX5CoreLoadHelp", QbSdk.getX5CoreLoadHelp(context));
            }
        });

    }

    /**
     * 启动X5 独立Web进程的预加载服务。优点：
     * 1、后台启动，用户无感进程切换
     * 2、启动进程服务后，有X5内核时，X5预加载内核
     * 3、Web进程Crash时，不会使得整个应用进程crash掉
     * 4、隔离主进程的内存，降低网页导致的App OOM概率。
     *
     * 缺点：
     * 进程的创建占用手机整体的内存，demo 约为 150 MB
     */
    @SuppressLint("LongLogTag")
    private void startX5WebProcessPreinitService() {
        String currentProcessName = QbSdk.getCurrentProcessName(this);
        // 设置多进程数据目录隔离，不设置的话系统内核多个进程使用WebView会crash，X5下可能ANR
        com.tencent.smtt.sdk.WebView.setDataDirectorySuffix(QbSdk.getCurrentProcessName(this));
        Log.e("startX5WebProcessPreinitService", currentProcessName);
        if (currentProcessName.equals(this.getPackageName())) {
            this.startService(new Intent(this, X5ProcessInitService.class));
        }
    }
}