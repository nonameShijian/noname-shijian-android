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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;

import androidx.appcompat.widget.ContentFrameLayout;
import androidx.webkit.WebViewAssetLoader;

import com.noname.api.NonameJavaScriptInterface;
import com.noname.shijian.check.CheckUtils;
import com.noname.shijian.view.DraggableButton;
import com.noname.shijian.websocket.SocketServer;
import com.noname.shijian.websocket.model.Client;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;
import com.norman.webviewup.lib.source.UpgradePackageSource;
import com.norman.webviewup.lib.source.UpgradeSource;
import com.norman.webviewup.lib.util.ProcessUtils;
import com.norman.webviewup.lib.util.VersionUtils;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    public CordovaPreferences getPreferences() {
        return preferences;
    }

    private static boolean inited = Build.VERSION.SDK_INT > 34;

    private ProgressDialog WebViewUpgradeProgressDialog;

    private WebView webview;

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
        webview = (WebView) view;
        WebSettings settings = webview.getSettings();
        initWebViewSettings(webview, settings);
        Log.e("getUserAgentString", settings.getUserAgentString());
        CheckUtils.check(this, Executors.newFixedThreadPool(5));

        initDevToolsFloatWindow();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate", String.valueOf(savedInstanceState));
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // initShizuku();

        boolean is64Bit = ProcessUtils.is64Bit();
        ArrayList<String> supportBitAbis = new ArrayList<>(Arrays.asList(is64Bit ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS));

        // 内置的apk只有这两种，如果都不包含，就不触发升级内核操作（例如: 虚拟机需要x86）
        boolean containsArm64 = supportBitAbis.contains("arm64-v8a");
        boolean containsArmeabi = supportBitAbis.contains("armeabi-v7a");
        // boolean containsX86 = Arrays.binarySearch(supportBitAbis, "x86");

        Log.e(TAG, supportBitAbis.toString());
        boolean useUpgrade = getSharedPreferences("nonameyuri", MODE_PRIVATE).getBoolean("useUpgrade", true);

        if (!useUpgrade || inited || (!containsArm64 && !containsArmeabi)) {
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
                Log.e(TAG, SystemWebViewPackageName);
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
                        // Toast.makeText(getApplicationContext(), "系统Webview版本较新，无需升级", Toast.LENGTH_LONG).show();
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

    private void initWebViewSettings(WebView webview, WebSettings settings) {
        // int textZoom = settings.getTextZoom();
        // Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
        settings.setTextZoom(100);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
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
        // destroyShizuku();
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

        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } catch (Exception ignored) {}
        if (devWebView != null) {
            devWebView.stopLoading();
            devWebView.removeAllViews();
            devWebView.destroy();
            if (devWebView.getParent() != null) {
                ((ViewGroup) devWebView.getParent()).removeView(devWebView);
            }
            devWebView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        View rootview = getWindow().getDecorView();
        View focusView = rootview.findFocus();
        Log.e(TAG, String.valueOf(focusView));
        if (appView != null && focusView == appView.getView() && ((SystemWebView) appView.getView()).canGoBack()) {
            Log.e(TAG, "SystemWebView");
            SystemWebView webview = (SystemWebView) appView.getView();
            webview.goBack();
            Log.e(TAG, "SystemWebView -> " + webview.getUrl());
        }
        if (devWebView != null && focusView == devWebView && devWebView.canGoBack()) {
            devWebView.goBack();
            Log.e(TAG, "devWebView -> " + devWebView.getUrl());
        }
        else {
            Log.e(TAG, "other");
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

    private static boolean initedDevTools = false;

    private SocketServer server = null;

    private WebView devWebView;

    private String devWebViewUrl = "about:blank";

    @SuppressLint("RestrictedApi")
    private ContentFrameLayout parentView;

    private DraggableButton draggableButton;

    private View floatingView;

    public boolean showDebugButtonState = false;

    public void showDebugButton() {
        if (parentView != null && draggableButton != null) {
            parentView.addView(draggableButton);
        }
    }

    public void hideDebugButton() {
        if (parentView != null && draggableButton != null) {
            parentView.removeView(draggableButton);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    public void initDevToolsFloatWindow() {
        parentView = (ContentFrameLayout) webview.getParent();
        // 创建并初始化可拖动按钮
        draggableButton = new DraggableButton(this);
        floatingView = LayoutInflater.from(this).inflate(R.layout.activity_dev_tools, null);
        // 设置点击事件拦截器
        floatingView.setOnTouchListener((v, event) -> {
            // 拦截所有点击事件
            return true;
        });
        floatingView.setAlpha(0.7f);
        devWebView = floatingView.findViewById(R.id.dev_webview);

        // 设置按钮的初始位置和大小
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // 设置初始margin，根据需要调整
        buttonParams.setMargins(50, 50, 0, 0);
        // 设置宽度和高度，这里以150dp为例
        buttonParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        buttonParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        draggableButton.setLayoutParams(buttonParams);
        draggableButton.setOnClickListener(view -> {
            showDevTools();
        });

        // parentView.addView(draggableButton);

        // 设置监听器来处理上一页/下一页按钮点击事件
        Button buttonBack = floatingView.findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> {
            if (devWebView.canGoBack()) {
                devWebView.goBack();
            }
        });

        Button buttonNext = floatingView.findViewById(R.id.button_next);
        buttonNext.setOnClickListener(v -> {
            if (devWebView.canGoForward()) {
                devWebView.goForward();
            }
        });

        CheckBox upgrade = floatingView.findViewById(R.id.upgrade);
        upgrade.setChecked(getSharedPreferences("nonameyuri", MODE_PRIVATE).getBoolean("useUpgrade", true));
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setMessage("更改配置可能会导致(下次启动时)Webview数据被清空，是否继续？");
        dlg.setTitle("警告");
        dlg.setCancelable(true);
        final boolean[] isHandlingStateChange = {false};
        upgrade.setOnCheckedChangeListener((v, bool) -> {
            if (isHandlingStateChange[0]) {
                isHandlingStateChange[0] = false;
                return;
            }
            dlg.setPositiveButton(android.R.string.ok,
                    (dialog, which) -> getSharedPreferences("nonameyuri", MODE_PRIVATE)
                            .edit()
                            .putBoolean("useUpgrade", bool)
                            .apply());
            dlg.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                getSharedPreferences("nonameyuri", MODE_PRIVATE)
                        .edit()
                        .putBoolean("useUpgrade", !bool)
                        .apply();
                isHandlingStateChange[0] = true;
                upgrade.setChecked(!bool);
            });
            dlg.setOnCancelListener(dialog -> {
                getSharedPreferences("nonameyuri", MODE_PRIVATE)
                        .edit()
                        .putBoolean("useUpgrade", !bool)
                        .apply();
                isHandlingStateChange[0] = true;
                upgrade.setChecked(!bool);
            });
            dlg.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    getSharedPreferences("nonameyuri", MODE_PRIVATE)
                            .edit()
                            .putBoolean("useUpgrade", !bool)
                            .apply();
                    isHandlingStateChange[0] = true;
                    upgrade.setChecked(!bool);
                    return false;
                }
                else return true;
            });
            dlg.show();
        });

        // 设置监听器来处理关闭按钮点击事件
        Button buttonClose = floatingView.findViewById(R.id.button_close);
        buttonClose.setOnClickListener(v -> {
            hideDevTools();
        });
    }

    public void showDevTools() {
        initDevTools();
        parentView.addView(floatingView);
        hideDebugButton();
    }

    public void hideDevTools() {
        parentView.removeView(floatingView);
        if (showDebugButtonState) {
            showDebugButton();
        }
    }

    public void initDevTools() {
        if (devWebView == null) return;
        // webSocket
        if (server == null) {
            server = new SocketServer(9222, devWebView);
        }
        if (initedDevTools) return;
        initedDevTools = true;
        server.start();
        initDevToolsWebViewClient();
        initDevToolsWebViewSettings();
    }

    public void initDevToolsWebViewClient() {
        // devWebView
        AssetManager assetManager = getAssets();

        // 初始化WebViewAssetLoader
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain("localhost")
                .setHttpAllowed(true)
                .addPathHandler("/", path -> {
                    try {
                        if (path.isEmpty()) {
                            path = "index.html";
                        }
                        if ("/json".equals(path) || "/json/".equals(path)) {
                            JSONObject data = new JSONObject();
                            JSONArray targets = new JSONArray();
                            if (server != null) {
                                for (HashMap.Entry<String, Client> clientEntry : server.clients.entrySet()) {
                                    Client client = clientEntry.getValue();
                                    JSONObject clientData = new JSONObject();
                                    clientData.put("id", client.id);
                                    clientData.put("pageUrl", client.pageUrl);
                                    clientData.put("time", client.time);
                                    clientData.put("title", client.title);
                                    clientData.put("favicon", client.favicon);
                                    targets.put(clientData);
                                }
                            }
                            data.put("targets", targets);
                            // 将JSONObject转换为字符串
                            String jsonString = data.toString();
                            // 将字符串转换为字节数组
                            byte[] byteArray = jsonString.getBytes(StandardCharsets.UTF_8);
                            // 使用字节数组创建InputStream
                            InputStream inputStream = new ByteArrayInputStream(byteArray);
                            return new WebResourceResponse("application/json", null, inputStream);
                        }
                        // InputStream is = assetManager.open("www/" + path, AssetManager.ACCESS_STREAMING);
                        InputStream is;
                        String[] split = ("www/" + path).split("/");
                        String[] newSplit = Arrays.copyOfRange(split, 0, split.length - 1);
                        List<String> list = Arrays.asList(assetManager.list(String.join("/", newSplit)));
                        if (list.contains(split[split.length - 1])) {
                            is = assetManager.open("www/" + path, AssetManager.ACCESS_STREAMING);
                        } else {
                            File file = new File(
                                    getExternalFilesDir(null).getParentFile(),
                                    path
                            );
                            is = new FileInputStream(file);
                        }
                        String mimeType = "text/html";
                        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                        if (extension != null) {
                            if (path.endsWith(".js") || path.endsWith(".mjs")) {
                                // Make sure JS files get the proper mimetype to support ES modules
                                mimeType = "application/javascript";
                            } else if (path.endsWith(".wasm")) {
                                mimeType = "application/wasm";
                            } else {
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                            }
                        }
                        return new WebResourceResponse(mimeType, null, is);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .build();

        // 设置WebViewClient
        devWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        CordovaDialogsHelper dialogsHelper = new CordovaDialogsHelper(this);

        devWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                dialogsHelper.showAlert(message, new CordovaDialogsHelper.Result() {
                    @Override public void gotResult(boolean success, String value) {
                        if (success) {
                            result.confirm();
                        } else {
                            result.cancel();
                        }
                    }
                });
                return true;
            }
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                dialogsHelper.showConfirm(message, new CordovaDialogsHelper.Result() {
                    @Override
                    public void gotResult(boolean success, String value) {
                        if (success) {
                            result.confirm();
                        } else {
                            result.cancel();
                        }
                    }
                });
                return true;
            }
            @Override
            public boolean onJsPrompt(WebView view, String origin, String message, String defaultValue, final JsPromptResult result) {
                dialogsHelper.showPrompt(message, defaultValue, new CordovaDialogsHelper.Result() {
                    @Override
                    public void gotResult(boolean success, String value) {
                        if (success) {
                            result.confirm(value);
                        } else {
                            result.cancel();
                        }
                    }
                });
                return true;
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initDevToolsWebViewSettings() {
        devWebView.setInitialScale(0);
        devWebView.setVerticalScrollBarEnabled(false);
        final WebSettings devSettings = devWebView.getSettings();
        devSettings.setJavaScriptEnabled(true);
        devSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        devSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        devSettings.setSaveFormData(false);
        if (preferences.getBoolean("AndroidInsecureFileModeEnabled", false)) {
            devSettings.setAllowFileAccess(true);
            devSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        devSettings.setMediaPlaybackRequiresUserGesture(false);
        devSettings.setDatabaseEnabled(true);
        devSettings.setDomStorageEnabled(true);
        devSettings.setGeolocationEnabled(true);

        if (!"about:blank".equals(devWebViewUrl)) devWebView.loadUrl(devWebViewUrl);
        devWebView.setHorizontalScrollBarEnabled(false);
        devSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }
}