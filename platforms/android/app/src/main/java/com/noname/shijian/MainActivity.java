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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.noname.shijian.check.CheckUtils;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

import java.io.File;
import java.util.concurrent.Executors;


public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    public CordovaPreferences getPreferences() {
        return preferences;
    }

//    @Override
//    public CordovaWebViewEngine makeWebViewEngine() {
//        if (false) {
//            initX5();
//            return new org.jeremyup.cordova.x5engine.X5WebViewEngine(this, this.preferences);
//        }
//        return super.makeWebViewEngine();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate", "111");
        super.onCreate(savedInstanceState);
        super.init();

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
        SystemWebView webview = (SystemWebView) view;
        WebSettings settings = webview.getSettings();
        initWebviewSettings(webview, settings);
        Log.e("getUserAgentString", settings.getUserAgentString());
        CheckUtils.check(this, Executors.newFixedThreadPool(5));
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


}