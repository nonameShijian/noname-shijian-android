package com.noname.core.activity;

import static com.noname.core.activity.WebViewSelectionActivity.SELECTED_WEBVIEW_PACKAGE;
import static com.noname.core.activity.WebViewSelectionActivity.WEBVIEW_PACKAGES;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.noname.core.NonameJavaScriptInterface;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradePackageSource;
import com.norman.webviewup.lib.source.UpgradeSource;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.LOG;
import org.apache.cordova.engine.SystemWebView;

import java.net.URI;
import java.net.URISyntaxException;

import cn.hle.skipselfstartmanager.util.MobileInfoUtils;

public class MainActivity extends CordovaActivity {
    protected ProgressDialog WebViewUpgradeProgressDialog;

    protected static boolean WebviewUpgraded = false;

    protected WebView webview;

    protected void ActivityOnCreate(Bundle extras) {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        try {
            if (extras != null && extras.getString("importExtensionName") != null) {
                String extName = extras.getString("importExtensionName");
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
        webview = (WebView) view;
        WebSettings settings = webview.getSettings();
        Log.e(TAG, settings.getUserAgentString());
        initWebViewSettings(webview, settings);
    }

    protected void initWebViewSettings(WebView webview, WebSettings settings) {
        settings.setTextZoom(100);
       settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webview.addJavascriptInterface(new NonameJavaScriptInterface(this, webview, preferences), "NonameAndroidBridge");
        WebView.setWebContentsDebuggingEnabled(true);
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

        if (SELECTED_WEBVIEW_PACKAGE == null) {
            SELECTED_WEBVIEW_PACKAGE = getSharedPreferences("nonameyuri", MODE_PRIVATE)
                    .getString("selectedWebviewPackage", WEBVIEW_PACKAGES[0]);
        }

        boolean useUpgrade = getSharedPreferences("nonameyuri", MODE_PRIVATE).getBoolean("useUpgrade", true);

        if (!useUpgrade || WebviewUpgraded) {
            ActivityOnCreate(extras);
        }
        else {
            WebviewUpgraded = true;

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

            Activity context = this;

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

                    try {
                        PackageInfo upgradePackageInfo = getPackageManager().getPackageInfo(SELECTED_WEBVIEW_PACKAGE, 0);
                        if (upgradePackageInfo != null) {

                            if (Build.VERSION.SDK_INT > 34) {
                                String serviceName =  "org.chromium.content.app.SandboxedProcessService0";

                                ServiceConnection mConnection = new ServiceConnection() {
                                    @Override
                                    public void onServiceConnected(ComponentName className, IBinder service) {
                                        Log.e(TAG, serviceName + "服务连接成功");
                                    }

                                    @Override
                                    public void onServiceDisconnected(ComponentName arg0) {
                                        Log.e(TAG, serviceName + "服务意外断开");
                                    }
                                };

                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName(SELECTED_WEBVIEW_PACKAGE, serviceName);
                                    boolean isServiceBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                                    if (isServiceBound) {
                                        Log.e(TAG, serviceName + "服务已启动并且绑定成功");
                                    }
                                    else {
                                        Log.e(TAG, serviceName + "是服务未启动或不存在");
                                        Log.e("MainActivity", serviceName + "是服务未启动或不存在");
                                        navigateToAppSettingsAndExit();
                                    }
                                } catch (java.lang.SecurityException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, serviceName + "服务已启动");
                                }
                            }

                            ActivityOnCreate(extras);
                        } else {
                            ActivityOnCreate(extras);
                        }
                    } catch (Exception e) {
                        ActivityOnCreate(extras);
                    }
                }

                @Override
                public void onUpgradeError(Throwable throwable) {
                    Log.e(TAG, "onUpgradeError: " + throwable.getMessage());
                    android.app.AlertDialog.Builder dlg = new android.app.AlertDialog.Builder(context);
                    dlg.setMessage("Webview内核升级失败，是否设置其它Webview实现？(" + throwable.getMessage() + ")");
                    dlg.setTitle("Alert");
                    dlg.setCancelable(false);
                    dlg.setPositiveButton("立即设置",
                            (dialog1, which1) -> {
                                changeWebviewProvider();
                                ActivityOnCreate(extras);
                            });
                    dlg.setNegativeButton("暂时不设置",
                            (dialog3, which2) -> {
                                dialog3.dismiss();
                                ActivityOnCreate(extras);
                            });
                    dlg.setOnKeyListener((dialog2, keyCode, event) -> {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            ActivityOnCreate(extras);
                            return false;
                        }
                        else {
                            changeWebviewProvider();
                            ActivityOnCreate(extras);
                            return true;
                        }
                    });
                    dlg.show();
                }
            });

            try {
                // 添加webview
                UpgradeSource upgradeSource = new UpgradePackageSource(
                        getApplicationContext(),
                        SELECTED_WEBVIEW_PACKAGE
                );

                String SystemWebViewPackageName = WebViewUpgrade.getSystemWebViewPackageName();
                Log.e(TAG, "SystemWebViewPackageName: " + SystemWebViewPackageName);
                Log.e(TAG, "SelectedWebviewPackage: " + SELECTED_WEBVIEW_PACKAGE);

                // 如果webview就是已经选的
                if (SELECTED_WEBVIEW_PACKAGE.equals(SystemWebViewPackageName)) {
                    ActivityOnCreate(extras);
                    return;
                }

                WebViewUpgrade.upgrade(upgradeSource);
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
                ActivityOnCreate(extras);
            }
        }
    }

    protected void navigateToAppSettingsAndExit() {
        android.app.AlertDialog.Builder dlg = new android.app.AlertDialog.Builder(this);
        dlg.setMessage("请授予Webview(" + SELECTED_WEBVIEW_PACKAGE + ")自启动权限后重新进入APP，否则本App将无法正常使用Webview组件！");
        dlg.setTitle("Alert");
        dlg.setCancelable(false);
        dlg.setPositiveButton("立即设置",
                (dialog1, which1) -> {
                    MobileInfoUtils.jumpStartInterface(this);
                    finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                });
        dlg.setNegativeButton("暂时不设置",
                (dialog3, which2) -> dialog3.dismiss());
        dlg.setOnKeyListener((dialog2, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
            else {
                MobileInfoUtils.jumpStartInterface(this);
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            }
        });
        dlg.show();
    }

    protected void changeWebviewProvider() {
        Intent newIntent = new Intent(this, WebViewSelectionActivity.class);
        newIntent.setAction(Intent.ACTION_VIEW);
        startActivity(newIntent);
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
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        View rootview = getWindow().getDecorView();
        View focusView = rootview.findFocus();
        Log.e(TAG, String.valueOf(focusView));
        if (webview != null && focusView == webview && webview.canGoBack()) {
            webview.goBack();
            Log.e(TAG, "SystemWebView -> " + webview.getUrl());
        }
        else {
            super.onBackPressed();
        }
    }
}
