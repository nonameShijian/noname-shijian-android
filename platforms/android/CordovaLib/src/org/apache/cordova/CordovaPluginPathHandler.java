package org.apache.cordova;

import androidx.webkit.WebViewAssetLoader;

// cordova 10的配置
public class CordovaPluginPathHandler {
    private final WebViewAssetLoader.PathHandler handler;

    public  CordovaPluginPathHandler(WebViewAssetLoader.PathHandler handler) {
        this.handler = handler;
    }

    public WebViewAssetLoader.PathHandler getPathHandler() {
        return handler;
    }
}
