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
package org.apache.cordova.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.ServiceWorkerWebSettings;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.cordova.AuthenticationToken;
import org.apache.cordova.CordovaClientCertRequest;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaPluginPathHandler;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import androidx.webkit.WebViewAssetLoader;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class is the WebViewClient that implements callbacks for our web view.
 * The kind of callbacks that happen here are regarding the rendering of the
 * document instead of the chrome surrounding it, such as onPageStarted(),
 * shouldOverrideUrlLoading(), etc. Related to but different than
 * CordovaChromeClient.
 */
public class SystemWebViewClient extends WebViewClient {

    private static final String TAG = "SystemWebViewClient";
    protected final SystemWebViewEngine parentEngine;
    private final WebViewAssetLoader assetLoader;
    private boolean doClearHistory = false;
    boolean isCurrentlyLoading;

    /** The authorization tokens. */
    private Hashtable<String, AuthenticationToken> authenticationTokens = new Hashtable<String, AuthenticationToken>();

    public SystemWebViewClient(SystemWebViewEngine parentEngine) {
        this.parentEngine = parentEngine;

        WebViewAssetLoader.Builder assetLoaderBuilder = new WebViewAssetLoader.Builder()
                .setDomain(parentEngine.preferences.getString("hostname", "localhost").toLowerCase())
                .setHttpAllowed(true);

        final Context context = parentEngine.webView.getContext();
        AssetManager assetManager =  context.getAssets();

        assetLoaderBuilder.addPathHandler("/android_asset/", new WebViewAssetLoader.AssetsPathHandler(context));
        assetLoaderBuilder.addPathHandler("/android_res/", new WebViewAssetLoader.ResourcesPathHandler(context));
        assetLoaderBuilder.addPathHandler("/", path -> {
            try {
                // Check if there a plugins with pathHandlers
                PluginManager pluginManager = this.parentEngine.pluginManager;
                if (pluginManager != null) {
                    for (CordovaPluginPathHandler handler : pluginManager.getPluginPathHandlers()) {
                        if (handler.getPathHandler() != null) {
                            WebResourceResponse response = handler.getPathHandler().handle(path);
                            if (response != null) {
                                return response;
                            }
                        };
                    }
                }

                if (path.isEmpty()) {
                    path = "index.html";
                }
                // LOG.e(TAG, path);
                InputStream is;
                // 原来cordova的路径
                String[] split = ("www/" + path).split("/");
                // 获取原路径所在的文件夹
                String[] newSplit = Arrays.copyOfRange(split, 0, split.length - 1);
                // 原路径所在的文件夹的所有文件、文件夹
                List<String> list = Arrays.asList(assetManager.list(String.join("/", newSplit)));
                // LOG.e(TAG, String.valueOf(list));
                if (list.contains(split[split.length - 1])) {
                    is = assetManager.open("www/" + path, AssetManager.ACCESS_STREAMING);
                } else {
                    File file = new File(
                            context.getExternalFilesDir(null).getParentFile(),
                            path
                    );
                    // LOG.e(TAG, file.getAbsolutePath());
                    is = new FileInputStream(file);
                }
                // LOG.e(TAG, "-----------------------");

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
                LOG.e(TAG, e.getMessage());
            }
            return null;
        });

        final WebViewAssetLoader assetLoader = assetLoaderBuilder.build();

        this.assetLoader = assetLoader;

        ServiceWorkerController swController = ServiceWorkerController.getInstance();
        swController.setServiceWorkerClient(new ServiceWorkerClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                // Capture request here and generate response or allow pass-through
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });
        ServiceWorkerWebSettings serviceWorkerWebSettings = swController.getServiceWorkerWebSettings();
        serviceWorkerWebSettings.setAllowContentAccess(true);
        serviceWorkerWebSettings.setAllowFileAccess(true);
    }

    /**
     * Give the host application a chance to take over the control when a new url
     * is about to be loaded in the current WebView.
     *
     * @param view          The WebView that is initiating the callback.
     * @param url           The url to be loaded.
     * @return              true to override, false for default behavior
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return parentEngine.client.onNavigationAttempt(url);
    }

    /**
     * On received http auth request.
     * The method reacts on all registered authentication tokens. There is one and only one authentication token for any host + realm combination
     */
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

        // Get the authentication token (if specified)
        AuthenticationToken token = this.getAuthenticationToken(host, realm);
        if (token != null) {
            handler.proceed(token.getUserName(), token.getPassword());
            return;
        }

        // Check if there is some plugin which can resolve this auth challenge
        PluginManager pluginManager = this.parentEngine.pluginManager;
        if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(null, new CordovaHttpAuthHandler(handler), host, realm)) {
            parentEngine.client.clearLoadTimeoutTimer();
            return;
        }

        // By default handle 401 like we'd normally do!
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    /**
     * On received client cert request.
     * The method forwards the request to any running plugins before using the default implementation.
     *
     * @param view
     * @param request
     */
    @Override
    public void onReceivedClientCertRequest (WebView view, ClientCertRequest request)
    {

        // Check if there is some plugin which can resolve this certificate request
        PluginManager pluginManager = this.parentEngine.pluginManager;
        if (pluginManager != null && pluginManager.onReceivedClientCertRequest(null, new CordovaClientCertRequest(request))) {
            parentEngine.client.clearLoadTimeoutTimer();
            return;
        }

        // By default pass to WebViewClient
        super.onReceivedClientCertRequest(view, request);
    }

    /**
     * Notify the host application that a page has started loading.
     * This method is called once for each main frame load so a page with iframes or framesets will call onPageStarted
     * one time for the main frame. This also means that onPageStarted will not be called when the contents of an
     * embedded frame changes, i.e. clicking a link whose target is an iframe.
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        isCurrentlyLoading = true;
        // Flush stale messages & reset plugins.
        parentEngine.bridge.reset();
        parentEngine.client.onPageStarted(url);
    }

    /**
     * Notify the host application that a page has finished loading.
     * This method is called only for main frame. When onPageFinished() is called, the rendering picture may not be updated yet.
     *
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        // Ignore excessive calls, if url is not about:blank (CB-8317).
        if (!isCurrentlyLoading && !url.startsWith("about:")) {
            return;
        }
        isCurrentlyLoading = false;

        /**
         * Because of a timing issue we need to clear this history in onPageFinished as well as
         * onPageStarted. However we only want to do this if the doClearHistory boolean is set to
         * true. You see when you load a url with a # in it which is common in jQuery applications
         * onPageStared is not called. Clearing the history at that point would break jQuery apps.
         */
        if (this.doClearHistory) {
            view.clearHistory();
            this.doClearHistory = false;
        }
        parentEngine.client.onPageFinishedLoading(url);

    }

    /**
     * Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable).
     * The errorCode parameter corresponds to one of the ERROR_* constants.
     *
     * @param view          The WebView that is initiating the callback.
     * @param errorCode     The error code corresponding to an ERROR_* value.
     * @param description   A String describing the error.
     * @param failingUrl    The url that failed to load.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // Ignore error due to stopLoading().
        if (!isCurrentlyLoading) {
            return;
        }
        Log.e(TAG, "errorCode = [" + errorCode + "], description = [" + description + "]");
        LOG.d(TAG, "CordovaWebViewClient.onReceivedError: Error code=%s Description=%s URL=%s", errorCode, description, failingUrl);

        // If this is a "Protocol Not Supported" error, then revert to the previous
        // page. If there was no previous page, then punt. The application's config
        // is likely incorrect (start page set to sms: or something like that)
        if (errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
            parentEngine.client.clearLoadTimeoutTimer();

            if (view.canGoBack()) {
                view.goBack();
                return;
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }
        parentEngine.client.onReceivedError(errorCode, description, failingUrl);
    }

    /**
     * Notify the host application that an SSL error occurred while loading a resource.
     * The host application must call either handler.cancel() or handler.proceed().
     * Note that the decision may be retained for use in response to future SSL errors.
     * The default behavior is to cancel the load.
     *
     * @param view          The WebView that is initiating the callback.
     * @param handler       An SslErrorHandler object that will handle the user's response.
     * @param error         The SSL error object.
     */
    @SuppressLint("WebViewClientOnReceivedSslError")
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        Log.e(TAG, "SslErrorHandler = [" + handler + "], SslError = [" + error + "]");
        final String packageName = parentEngine.cordova.getActivity().getPackageName();
        final PackageManager pm = parentEngine.cordova.getActivity().getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // debug = true
                handler.proceed();
            } else {
                // debug = false
                super.onReceivedSslError(view, handler, error);
            }
        } catch (NameNotFoundException e) {
            // When it doubt, lock it out!
            super.onReceivedSslError(view, handler, error);
        }
    }


    /**
     * Sets the authentication token.
     *
     * @param authenticationToken
     * @param host
     * @param realm
     */
    public void setAuthenticationToken(AuthenticationToken authenticationToken, String host, String realm) {
        if (host == null) {
            host = "";
        }
        if (realm == null) {
            realm = "";
        }
        this.authenticationTokens.put(host.concat(realm), authenticationToken);
    }

    /**
     * Removes the authentication token.
     *
     * @param host
     * @param realm
     *
     * @return the authentication token or null if did not exist
     */
    public AuthenticationToken removeAuthenticationToken(String host, String realm) {
        return this.authenticationTokens.remove(host.concat(realm));
    }

    /**
     * Gets the authentication token.
     *
     * In order it tries:
     * 1- host + realm
     * 2- host
     * 3- realm
     * 4- no host, no realm
     *
     * @param host
     * @param realm
     *
     * @return the authentication token
     */
    public AuthenticationToken getAuthenticationToken(String host, String realm) {
        AuthenticationToken token = null;
        token = this.authenticationTokens.get(host.concat(realm));

        if (token == null) {
            // try with just the host
            token = this.authenticationTokens.get(host);

            // Try the realm
            if (token == null) {
                token = this.authenticationTokens.get(realm);
            }

            // if no host found, just query for default
            if (token == null) {
                token = this.authenticationTokens.get("");
            }
        }

        return token;
    }

    /**
     * Clear all authentication tokens.
     */
    public void clearAuthenticationTokens() {
        this.authenticationTokens.clear();
    }

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        try {
            // Check the against the allow list and lock out access to the WebView directory
            // Changing this will cause problems for your application
            if (!parentEngine.pluginManager.shouldAllowRequest(url)) {
                LOG.w(TAG, "URL blocked by allow list: " + url);
                // Results in a 404.
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }

            CordovaResourceApi resourceApi = parentEngine.resourceApi;
            Uri origUri = Uri.parse(url);
            // Allow plugins to intercept WebView requests.
            Uri remappedUri = resourceApi.remapUri(origUri);

            if (!origUri.equals(remappedUri) || needsSpecialsInAssetUrlFix(origUri) || needsContentUrlFix(origUri)) {
                CordovaResourceApi.OpenForReadResult result = resourceApi.openForRead(remappedUri, true);
                return new WebResourceResponse(result.mimeType, "UTF-8", result.inputStream);
            }
            // If we don't need to special-case the request, let the browser load it.
            // return null;
            return null;
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                LOG.e(TAG, "Error occurred while loading a file (returning a 404).", e);
            }
            // Results in a 404.
            return new WebResourceResponse("text/plain", "UTF-8", null);
        }
    }

    private static boolean needsContentUrlFix(Uri uri) {
        return "content".equals(uri.getScheme());
    }

    private static boolean needsSpecialsInAssetUrlFix(Uri uri) {
        if (CordovaResourceApi.getUriType(uri) != CordovaResourceApi.URI_TYPE_ASSET) {
            return false;
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            return true;
        }

        if (!uri.toString().contains("%")) {
            return false;
        }

        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        Map<String, String> headers = request.getRequestHeaders();
        if (url.startsWith("file://")) {
            if (!url.contains("/app_webview/") && !url.contains("/app_xwalkcore/") && url.endsWith(".js")) {
                // 是否是模块请求
                if (headers != null
                        && headers.containsKey("Origin")
                        && Objects.equals(headers.get("Origin"), "file://")
                        // 非兼容版可能没有这个属性
                        // 但是华为webview可能会失败
                        && (!headers.containsKey("Sec-Fetch-Mode") || Objects.equals(headers.get("Sec-Fetch-Mode"), "cors"))
                ) {
                    try {
                        URL Url = new URL(url);
                        URLConnection connection = Url.openConnection();
                        String mimeType = connection.getContentType();
                        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                        if (extension != null) {
                            if (url.endsWith(".js") || url.endsWith(".mjs")) {
                                // Make sure JS files get the proper mimetype to support ES modules
                                mimeType = "application/javascript";
                            } else if (url.endsWith(".wasm")) {
                                mimeType = "application/wasm";
                            } else {
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                            }
                        }
                        InputStream data = Url.openStream();
                        return new WebResourceResponse(mimeType, "utf-8", data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return shouldInterceptRequest(view, url);
        }
        Log.e(TAG, "url = [" + url + "], method = [" + method + "]");

        CordovaPreferences prefs = parentEngine.preferences;
        String scheme = prefs.getString("scheme", "https").toLowerCase();
        String hostname = prefs.getString("hostname", "localhost").toLowerCase();
        // 不是以本网址显示的，一律由java请求
        if (!url.startsWith(scheme + "://" + hostname + '/')) {
            WebResourceResponse newRequest = request(request);
            if (newRequest != null) return newRequest;
        }
        return this.assetLoader.shouldInterceptRequest(request.getUrl());
    }

    public WebResourceResponse request(WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        if (!url.startsWith("http")) return null;
        try {
            HttpURLConnection httpConnect;
            HttpsURLConnection httpsConnect;
            if (url.startsWith("https")) {
                httpsConnect = (HttpsURLConnection) new URL(url).openConnection();
                httpsConnect.setReadTimeout(5000);
                httpsConnect.setConnectTimeout(5000);
                httpsConnect.setRequestMethod(method);
                httpsConnect.setUseCaches(false);
                if (request.getRequestHeaders() != null) for (Map.Entry<String, String> item : request.getRequestHeaders().entrySet()) {
                    //设置header
                    Log.e(TAG, "request添加header: " + item.getKey() + " : " + item.getValue());
                    httpsConnect.setRequestProperty(item.getKey(), item.getValue());
                }
                if (httpsConnect.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = httpsConnect.getInputStream();
                    String mimeType = httpsConnect.getContentType();
                    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                    if (extension != null) {
                        if (url.endsWith(".js") || url.endsWith(".mjs")) {
                            // Make sure JS files get the proper mimetype to support ES modules
                            mimeType = "application/javascript";
                        } else if (url.endsWith(".wasm")) {
                            mimeType = "application/wasm";
                        } else {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                        }
                    }
                    WebResourceResponse newRequest = new WebResourceResponse(mimeType, "utf-8", inputStream);
                    Log.e(TAG, "newRequest返回StatusCode: " + newRequest.getStatusCode());
                    Log.e(TAG, "newRequest返回header: " + newRequest.getResponseHeaders());
                    Map<String, List<String>> allHeaders = httpsConnect.getHeaderFields();
                    Map<String, String> singleValueHeaders = new HashMap<>();
                    for (Map.Entry<String, List<String>> entry : allHeaders.entrySet()) {
                        String headerName = entry.getKey();
                        StringBuilder headerValueBuilder = new StringBuilder();

                        if (!entry.getValue().isEmpty()) {
                            Log.e(TAG, "httpsConnect返回header: " + entry.getKey() + " : " + entry.getValue());
                            boolean isCookieHeader = "Cookie".equalsIgnoreCase(headerName);

                            for (String value : entry.getValue()) {
                                if (isCookieHeader) {
                                    // 对于Cookie特殊处理，用分号和空格隔开各个cookie值
                                    if (headerValueBuilder.length() > 0) {
                                        headerValueBuilder.append("; ");
                                    }
                                    headerValueBuilder.append(value);
                                } else {
                                    // 其他头字段直接用逗号分隔
                                    if (headerValueBuilder.length() > 0) {
                                        headerValueBuilder.append(",");
                                    }
                                    headerValueBuilder.append(value);
                                }
                            }

                            singleValueHeaders.put(headerName, headerValueBuilder.toString());
                        }
                    }
                    singleValueHeaders.put("Access-Control-Allow-Origin", "*");
                    singleValueHeaders.put("Access-Control-Allow-Headers","X-Requested-With");
                    singleValueHeaders.put("Access-Control-Allow-Methods","POST, GET, OPTIONS, DELETE");
                    singleValueHeaders.put("Access-Control-Allow-Credentials", "true");
                    newRequest.setResponseHeaders(singleValueHeaders);
                    return newRequest;
                } else {
                    httpsConnect.disconnect();
                }
            }
            else {
                httpConnect = (HttpURLConnection) new URL(url).openConnection();
                httpConnect.setReadTimeout(5000);
                httpConnect.setConnectTimeout(5000);
                httpConnect.setRequestMethod(method);
                httpConnect.setUseCaches(false);
                if (request.getRequestHeaders() != null) for (Map.Entry<String, String> item : request.getRequestHeaders().entrySet()) {
                    //设置header
                    Log.e(TAG, "request添加header: " + item.getKey() + " : " + item.getValue());
                    httpConnect.setRequestProperty(item.getKey(), item.getValue());
                }
                if (httpConnect.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = httpConnect.getInputStream();
                    String mimeType = httpConnect.getContentType();
                    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                    if (extension != null) {
                        if (url.endsWith(".js") || url.endsWith(".mjs")) {
                            // Make sure JS files get the proper mimetype to support ES modules
                            mimeType = "application/javascript";
                        } else if (url.endsWith(".wasm")) {
                            mimeType = "application/wasm";
                        } else {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                        }
                    }
                    WebResourceResponse newRequest = new WebResourceResponse(mimeType, "utf-8", inputStream);
                    Map<String, List<String>> allHeaders = httpConnect.getHeaderFields();
                    Map<String, String> singleValueHeaders = new HashMap<>();
                    for (Map.Entry<String, List<String>> entry : allHeaders.entrySet()) {
                        String headerName = entry.getKey();
                        StringBuilder headerValueBuilder = new StringBuilder();

                        if (!entry.getValue().isEmpty()) {
                            Log.e(TAG, "httpsConnect返回header: " + entry.getKey() + " : " + entry.getValue());
                            boolean isCookieHeader = "Cookie".equalsIgnoreCase(headerName);

                            for (String value : entry.getValue()) {
                                if (isCookieHeader) {
                                    // 对于Cookie特殊处理，用分号和空格隔开各个cookie值
                                    if (headerValueBuilder.length() > 0) {
                                        headerValueBuilder.append("; ");
                                    }
                                    headerValueBuilder.append(value);
                                } else {
                                    // 其他头字段直接用逗号分隔
                                    if (headerValueBuilder.length() > 0) {
                                        headerValueBuilder.append(",");
                                    }
                                    headerValueBuilder.append(value);
                                }
                            }

                            singleValueHeaders.put(headerName, headerValueBuilder.toString());
                        }
                    }
                    singleValueHeaders.put("Access-Control-Allow-Origin", "*");
                    singleValueHeaders.put("Access-Control-Allow-Headers","X-Requested-With");
                    singleValueHeaders.put("Access-Control-Allow-Methods","POST, GET, OPTIONS, DELETE");
                    singleValueHeaders.put("Access-Control-Allow-Credentials", "true");
                    newRequest.setResponseHeaders(singleValueHeaders);
                    return newRequest;
                } else {
                    httpConnect.disconnect();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "出现异常，路径为：" + url);
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
        return null;
    }
}
