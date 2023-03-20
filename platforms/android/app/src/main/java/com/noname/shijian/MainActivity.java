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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.*;

public class MainActivity extends CordovaActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate" ,"111");
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

        WebView.setWebContentsDebuggingEnabled(true);

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        WebView webview = (WebView) appView.getView();
        WebSettings settings = webview.getSettings();
        int textZoom = settings.getTextZoom();
        Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
        settings.setTextZoom(100);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(this));
        webview.addJavascriptInterface(new JavaScriptInterface(this, MainActivity.this, webview) , "noname_shijianInterfaces");
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        /*
        // 判断code
        if (requestCode == 2 && resultCode == 3) {
            // 从data中取出数据
            String path = intent.getStringExtra("path");
            String type = intent.getStringExtra("type");
            String readFile = intent.getStringExtra("readFile");
            Scanner scanner = null;
            try {
                path = path.replace(getPackageName(), getExternalFilesDir(null).getParentFile().getPath());
                StringBuilder string = new StringBuilder();
                File file = new File(path);
                boolean isDirectory = file.isDirectory();
                if (!isDirectory && readFile != null) {
                    scanner = new Scanner(file);
                    while (scanner.hasNextLine()) {
                        string.append(scanner.nextLine());
                    }
                }
                String result = "{ \"path\": \"" + path + "\", \"isDirectory\": " + isDirectory + ", \"type\": " + ( type == null ? null : ("\"" + type + "\"") ) + ", \"data\": \"" + string.toString() + "\" }";
                loadUrl("javascript:window.noname_shijianInterfaces.finishChooseFile(" + result + ")");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (scanner != null) scanner.close();
            }
        }
        */

        // 请求权限
        if (requestCode == 10085) {
            Log.e("resultCode", String.valueOf(resultCode));
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }
}