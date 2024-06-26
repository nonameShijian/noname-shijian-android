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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.noname.api.NonameJavaScriptInterface;
import com.noname.shijian.check.CheckUtils;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;
import com.norman.webviewup.lib.source.UpgradePackageSource;
import com.norman.webviewup.lib.source.UpgradeSource;
import com.norman.webviewup.lib.util.ProcessUtils;
import com.norman.webviewup.lib.util.VersionUtils;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.Executors;


public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    public CordovaPreferences getPreferences() {
        return preferences;
    }

    private static boolean inited = false;

    private ProgressDialog WebViewUpgradeProgressDialog;

    private void ActivityOnCreate(Bundle extras) {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        try {
            if (extras != null && extras.getString("importExtensionName") != null) {
                String extName = extras.getString("importExtensionName");
                FinishImport.ext = extName;
                URI uri = new URI(launchUrl);
                String newQuery = uri.getQuery();
                String appendQuery = "importExtensionName=" + extName;
                if (newQuery == null) {
                    newQuery = appendQuery;
                } else {
                    newQuery += "&" + appendQuery;
                }
                URI newUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
                Log.e(TAG, newUri.toString());
                loadUrl(newUri.toString());
            }
            else {
                loadUrl(launchUrl);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            // Set by <content src="index.html" /> in config.xml
            loadUrl(launchUrl);
        }

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
        LOG.e("onCreate", String.valueOf(savedInstanceState));
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        boolean is64Bit = ProcessUtils.is64Bit();
        String[] supportBitAbis = is64Bit ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;

        // 内置的apk只有这两种，如果都不包含，就不触发升级内核操作（例如: 虚拟机需要x86）
        int indexOfArm64 = Arrays.binarySearch(supportBitAbis,"arm64-v8a");
        int indexOfArmeabi = Arrays.binarySearch(supportBitAbis,"armeabi-v7a");

        Log.e(TAG, Arrays.toString(supportBitAbis));

        if (inited || (indexOfArm64 < 0 && indexOfArmeabi < 0)) {
            ActivityOnCreate(extras);
        }
        else {
            inited = true;

            if (WebViewUpgradeProgressDialog == null) {
                WebViewUpgradeProgressDialog = new ProgressDialog(this);
                WebViewUpgradeProgressDialog.setTitle("正在更新Webview内核");
                WebViewUpgradeProgressDialog.setCancelable(false);
                WebViewUpgradeProgressDialog.setIndeterminate(false);
                WebViewUpgradeProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                WebViewUpgradeProgressDialog.setMax(100);
                WebViewUpgradeProgressDialog.setProgress(0);
                if (WebViewUpgradeProgressDialog.isShowing()) WebViewUpgradeProgressDialog.hide();
            }

            WebViewUpgrade.addUpgradeCallback(new UpgradeCallback() {
                @Override
                public void onUpgradeProcess(float percent) {
                    if (percent <= 0.9 && !WebViewUpgradeProgressDialog.isShowing()) {
                        WebViewUpgradeProgressDialog.show();
                    }
                    WebViewUpgradeProgressDialog.setProgress((int) (percent * 100));
                }

                @Override
                public void onUpgradeComplete() {
                    Log.e(TAG, "onUpgradeComplete");
                    WebViewUpgradeProgressDialog.setProgress(100);
                    ActivityOnCreate(extras);
                }

                @Override
                public void onUpgradeError(Throwable throwable) {
                    Log.e(TAG, "onUpgradeError: " + throwable.getMessage());
                    ActivityOnCreate(extras);
                }
            });

            try {
                // 添加webview
                UpgradeSource upgradeSource;

                // 兼容版需要内置webview
                UpgradeAssetSource webviewUpgradeSource = new UpgradeAssetSource(
                        getApplicationContext(),
                        "com.google.android.webview_119.0.6045.194.apk",
                        new File(getApplicationContext().getFilesDir(), "com.google.android.webview/119.0.6045.194.apk")
                );

                // 其他的使用chrome就行
                UpgradePackageSource chromeUpgradeSource = new UpgradePackageSource(
                        getApplicationContext(),
                        "com.android.chrome"
                );

                if ("yuri.nakamura.noname".equals(getPackageName())) {
                    upgradeSource = webviewUpgradeSource;
                } else {
                    upgradeSource = chromeUpgradeSource;
                }

                String SystemWebViewPackageName = WebViewUpgrade.getSystemWebViewPackageName();
                // 如果webview就是chrome
                if ("com.android.chrome".equals(SystemWebViewPackageName)) {
                    ActivityOnCreate(extras);
                    return;
                }

                PackageInfo upgradePackageInfo = getPackageManager().getPackageInfo(chromeUpgradeSource.getPackageName(), 0);
                if (upgradePackageInfo != null) {
                    // google webview应当等同于chrome
                    if (upgradeSource == chromeUpgradeSource && "com.google.android.webview".equals(SystemWebViewPackageName) && "com.android.chrome".equals(chromeUpgradeSource.getPackageName())) {
                        SystemWebViewPackageName = "com.android.chrome";
                    }
                    if (SystemWebViewPackageName.equals(chromeUpgradeSource.getPackageName())
                            && VersionUtils.compareVersion( WebViewUpgrade.getSystemWebViewPackageVersion(), upgradePackageInfo.versionName) >= 0) {
                        Toast.makeText(getApplicationContext(), "系统Webview版本较新，无需升级", Toast.LENGTH_LONG).show();
                        ActivityOnCreate(extras);
                        return;
                    }
                    WebViewUpgrade.upgrade(upgradeSource);
                } else {
                    ActivityOnCreate(extras);
                }
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
                ActivityOnCreate(extras);
            }
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
        webview.addJavascriptInterface(new NonameJavaScriptInterface(this, webview, preferences), "NonameAndroidBridge");
        WebView.setWebContentsDebuggingEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LOG.e("onNewIntent" ,"111");
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String extName = extras.getString("importExtensionName");
            boolean importPackage = extras.getBoolean("importPackage", false);
            if (extName != null) {
                FinishImport.ext = extName;
            }
            View view = appView.getView();
            SystemWebView webview = (SystemWebView) view;
            if (webview != null) {
                if (extName != null) {
                    webview.evaluateJavascript("(() => {" +
                            "const event = new CustomEvent('importExtension', { " +
                            "detail: { extensionName: '" + extName + "'}" +
                            "});" +
                            "window.dispatchEvent(event);" +
                            "})();", null);
                }
                if (importPackage) {
                    webview.evaluateJavascript("(() => {" +
                            "const event = new CustomEvent('importPackage', { " +
                            "detail: { importPackage: true }" +
                            "});" +
                            "window.dispatchEvent(event);" +
                            "})();", null);
                }
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
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (appView != null) {
            SystemWebView webview = (SystemWebView) appView.getView();
            if (webview != null && webview.canGoBack()) {
                webview.goBack();
            }
            else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
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
}