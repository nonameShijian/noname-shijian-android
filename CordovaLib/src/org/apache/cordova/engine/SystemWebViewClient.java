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
import android.util.Base64;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
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
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                // InputStream is = parentEngine.webView.getContext().getAssets().open("www/" + path, AssetManager.ACCESS_STREAMING);
                // 使其在Asset文件夹中找不到文件时自动读取一次外部存储文件
                InputStream is;
                String[] split = ("www/" + path).split("/");
                String[] newSplit = Arrays.copyOfRange(split, 0, split.length - 1);
                List<String> list = Arrays.asList(assetManager.list(String.join("/", newSplit)));
                Long lastModified = null;
                if (list.contains(split[split.length - 1])) {
                    is = assetManager.open("www/" + path, AssetManager.ACCESS_STREAMING);
                } else {
                    File file = new File(
                            parentEngine.webView.getContext().getExternalFilesDir(null).getParentFile(),
                            path
                    );
                    lastModified = file.lastModified();
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

                WebResourceResponse response = new WebResourceResponse(mimeType, null, is);
                if (lastModified != null) {
                    Locale aLocale = Locale.US;
                    @SuppressLint("SimpleDateFormat")
                    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new DateFormatSymbols(aLocale));
                    Map<String, String> headers = new HashMap<>();
                    headers.put("last-modified", fmt.format(new Date(lastModified)));
                    if (response.getResponseHeaders() != null) {
                        headers.putAll(response.getResponseHeaders());
                    }
                    response.setResponseHeaders(headers);
                }
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                LOG.e(TAG, e.getMessage());
            }
            return null;
        });

        this.assetLoader = assetLoaderBuilder.build();
        boolean setAsServiceWorkerClient = parentEngine.preferences.getBoolean("ResolveServiceWorkerRequests", true);
        ServiceWorkerController controller = null;

        if (setAsServiceWorkerClient) {
            controller = ServiceWorkerController.getInstance();
            controller.setServiceWorkerClient(new ServiceWorkerClient(){
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }
            });
        }
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
     * @param view          The WebView initiating the callback.
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
     * @param view          The WebView initiating the callback.
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

        /*
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
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

        final String packageName = parentEngine.cordova.getActivity().getPackageName();
        final PackageManager pm = parentEngine.cordova.getActivity().getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // debug = true
                handler.proceed();
                return;
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
     * @return the authentication token or null if did not exist
     */
    public AuthenticationToken removeAuthenticationToken(String host, String realm) {
        return this.authenticationTokens.remove(host.concat(realm));
    }

    /**
     * Gets the authentication token.
     *
     * <p>In order it tries:</p>
     * <ol>
     *  <li>host + realm</li>
     *  <li>host</li>
     *  <li>realm</li>
     *  <li>no host, no realm</li>
     * </ol>
     *
     * @param host
     * @param realm
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
        LOG.e("Request", method + "  " + url + "  " + headers);
        if (url.startsWith("http://localhost:9222/remote/debug/image_base64")){
            try {
                Map<String, String> query_pairs = new LinkedHashMap<>();
                String query = new URL(url).getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length > 1) {
                            query_pairs.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
                        } else {
                            query_pairs.put(pair[0], "");
                        }
                    }
                }
                String imgUrl = query_pairs.get("url");
                if (imgUrl != null) {
                    if (imgUrl.startsWith("http://localhost/") || imgUrl.startsWith("https://localhost/")) {
                        File imageFile = new File(
                                this.parentEngine.webView.getContext().getExternalFilesDir(null).getParentFile(),
                                imgUrl.substring(17)
                        );
                        LOG.e("Request", imageFile.getAbsolutePath());
                        InputStream inputStream = new FileInputStream(imageFile);
                        byte[] buffer = new byte[(int) imageFile.length()];
                        inputStream.read(buffer);
                        inputStream.close();
                        String base64 =  Base64.encodeToString(buffer, Base64.DEFAULT);
                        LOG.e("Request", base64);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("base64", base64);
                        String jsonString = jsonObject.toString();
                        ByteArrayInputStream bais = new ByteArrayInputStream(jsonString.getBytes());
                        WebResourceResponse response = new WebResourceResponse(
                                "application/json",
                                "UTF-8",
                                bais
                        );
                        Map<String, String> responseHeaders = new HashMap<>();
                        if (response.getResponseHeaders() != null) {
                            responseHeaders.putAll(response.getResponseHeaders());
                        }
                        responseHeaders.put("Access-Control-Allow-Origin", "*");
                        response.setResponseHeaders(responseHeaders);
                        return response;
                    }
                }
            } catch (Exception e) {}
        }
        CordovaPreferences prefs = parentEngine.preferences;
        String scheme = prefs.getString("scheme", "https").toLowerCase();
        String hostname = prefs.getString("hostname", "localhost").toLowerCase();
        // 不是以本网址显示的，一律由java请求
        if (!url.startsWith(scheme + "://" + hostname + '/')) {
            WebResourceResponse newRequest = hookResponse(request);
            if (newRequest != null) return newRequest;
        }
        return this.assetLoader.shouldInterceptRequest(request.getUrl());
    }

    @Override
    public boolean onRenderProcessGone(final WebView view, RenderProcessGoneDetail detail) {
        // Check if there is some plugin which can handle this event
        PluginManager pluginManager = this.parentEngine.pluginManager;
        if (pluginManager != null && pluginManager.onRenderProcessGone(view, detail)) {
            return true;
        }

        return super.onRenderProcessGone(view, detail);
    }

    private WebResourceResponse hookResponse(WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        try {
            HttpURLConnection httpConnect;
            HttpsURLConnection httpsConnect;
            if (url.startsWith("https")) {
                httpsConnect = (HttpsURLConnection) new URL(url).openConnection();
                return request(request, httpsConnect);
            }
            else {
                httpConnect = (HttpURLConnection) new URL(url).openConnection();
                return request(request, httpConnect);
            }
        } catch (Exception e) {
            LOG.e(TAG, "出现异常，路径为：" + url);
            LOG.e(TAG, e.getMessage());
        }
        return null;
    }

    private WebResourceResponse request(WebResourceRequest request, HttpURLConnection httpConnect) throws Exception {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        httpConnect.setReadTimeout(5000);
        httpConnect.setConnectTimeout(5000);
        httpConnect.setRequestMethod(method);
        httpConnect.setUseCaches(false);
        if (request.getRequestHeaders() != null) for (Map.Entry<String, String> item : request.getRequestHeaders().entrySet()) {
            //设置header
            LOG.e(TAG, "request添加header: " + item.getKey() + " : " + item.getValue());
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
                    LOG.e(TAG, "httpsConnect返回header: " + entry.getKey() + " : " + entry.getValue());
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
            singleValueHeaders.put("Access-Control-Allow-Headers","X-Requested-With,Content-Type");
            singleValueHeaders.put("Access-Control-Allow-Methods","POST, GET, OPTIONS, DELETE");
            singleValueHeaders.put("Access-Control-Allow-Credentials", "true");
            newRequest.setResponseHeaders(singleValueHeaders);
            return newRequest;
        } else {
            httpConnect.disconnect();
            return null;
        }
    }

    private WebResourceResponse request(WebResourceRequest request, HttpsURLConnection httpConnect) throws Exception {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        httpConnect.setReadTimeout(5000);
        httpConnect.setConnectTimeout(5000);
        httpConnect.setRequestMethod(method);
        httpConnect.setUseCaches(false);
        if (request.getRequestHeaders() != null) for (Map.Entry<String, String> item : request.getRequestHeaders().entrySet()) {
            //设置header
            LOG.e(TAG, "request添加header: " + item.getKey() + " : " + item.getValue());
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
                    LOG.e(TAG, "httpsConnect返回header: " + entry.getKey() + " : " + entry.getValue());
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
            singleValueHeaders.put("Access-Control-Allow-Headers","X-Requested-With,Content-Type");
            singleValueHeaders.put("Access-Control-Allow-Methods","POST, GET, OPTIONS, DELETE");
            singleValueHeaders.put("Access-Control-Allow-Credentials", "true");
            newRequest.setResponseHeaders(singleValueHeaders);
            return newRequest;
        } else {
            httpConnect.disconnect();
            return null;
        }
    }
}
