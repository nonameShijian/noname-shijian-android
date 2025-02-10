package com.noname.core;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.noname.core.activity.WebViewSelectionActivity;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.apache.cordova.CordovaPreferences;

import java.io.File;

public class NonameJavaScriptInterface {
    private final Context context;
    private final Activity activity;
    private final WebView webview;
    private final CordovaPreferences preferences;

    public NonameJavaScriptInterface(Context context, WebView webview, CordovaPreferences preferences) {
        this.context = context;
        this.activity = (Activity) context;
        this.webview = webview;
        this.preferences = preferences;
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void showToast(@NonNull String message) {
        activity.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public boolean shareFile(@NonNull String documentPath) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath();
        if (!documentPath.startsWith(rootPath)) {
            documentPath = rootPath + "/" + documentPath;
        }
        File shareFile = new File(documentPath);
        if (!shareFile.exists()) {
            Log.e("shareFile", "文件不存在: " + documentPath);
            return false;
        } else if (shareFile.isDirectory()) {
            Log.e("shareFile", "不能分享文件夹: " + documentPath);
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                        context.getApplicationContext(),
                        context.getPackageName() + ".fileProvider",
                        shareFile));
        intent.setType("*/*");
        context.startActivity(Intent.createChooser(intent, "分享文件"));
        return true;
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void shareExtensionAsync(@NonNull String extName) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath() + "/extension/";
        String extPath = extName;
        if (!extPath.startsWith(rootPath)) {
            extPath = rootPath + extPath;
        }
        File shareExtDir = new File(extPath);
        File[] files = shareExtDir.listFiles();
        if (!shareExtDir.exists()) {
            Log.e("shareExtension", "文件不存在: " + extPath);
            return;
        } else if (shareExtDir.isFile()) {
            Log.e("shareExtension", "不能分享文件: " + extPath);
            return;
        } else if (files == null) {
            Log.e("shareExtension", "shareExtDir.listFiles()为空: " + extPath);
            return;
        }
        String zipName = extName + ".zip";
        File shareFile = new File(context.getExternalCacheDir(), zipName);
        if (shareFile.exists())
            shareFile.delete();

        if (webview != null) {
            webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStart('正在压缩扩展', '请稍候...');"));
        }
        new Thread(() -> {
            try {
                // 压缩文件
                ZipFile zipFile = new ZipFile(shareFile);
                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                parameters.setCompressionLevel(CompressionLevel.NORMAL);

                for (File file : files) {
                    if (file.isDirectory()) {
                        zipFile.addFolder(file, parameters);
                    } else {
                        zipFile.addFile(file, parameters);
                    }
                }

                zipFile.close();
                if (webview != null) {
                    webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();"));
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileProvider",
                                shareFile));
                share.setType("application/zip");
                context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            } catch (Exception e) {
                if (webview != null) {
                    webview.post(() -> webview
                            .loadUrl("javascript:navigator.notification.activityStop();alert('" + e + "')"));
                }
                Log.e("shareExtension", String.valueOf(e));
            }
        }).start();
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void shareExtensionWithPassWordAsync(@NonNull String extName, @NonNull String pwd) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath() + "/extension/";
        Log.e("shareExtension", rootPath);
        Log.e("shareExtension", extName);
        String extPath = extName;
        if (!extPath.startsWith(rootPath)) {
            extPath = rootPath + extPath;
        }
        Log.e("shareExtension", extPath);
        File shareExtDir = new File(extPath);
        File[] files = shareExtDir.listFiles();
        if (!shareExtDir.exists()) {
            Log.e("shareExtension", "文件不存在: " + extPath);
            return;
        } else if (shareExtDir.isFile()) {
            Log.e("shareExtension", "不能分享文件: " + extPath);
            return;
        } else if (files == null) {
            Log.e("shareExtension", "shareExtDir.listFiles()为空: " + extPath);
            return;
        }
        String zipName = extName + "(密码: " + pwd + ")" + ".zip";
        File shareFile = new File(context.getExternalCacheDir(), zipName);
        if (shareFile.exists())
            shareFile.delete();
        if (webview != null) {
            webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStart('正在压缩扩展', '请稍候...');"));
        }
        Log.e("shareExtension", "shareFile.getPath(): " + shareFile.getPath());
        new Thread(() -> {
            try {
                // 压缩文件
                ZipFile zipFile = new ZipFile(shareFile);
                zipFile.setPassword(pwd.toCharArray());
                ZipParameters parameters = new ZipParameters();
                parameters.setEncryptFiles(true);
                // 设置加密方法
                parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
                parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                parameters.setCompressionLevel(CompressionLevel.NORMAL);

                for (File file : files) {
                    if (file.isDirectory()) {
                        zipFile.addFolder(file, parameters);
                    } else {
                        zipFile.addFile(file, parameters);
                    }
                }

                zipFile.close();
                if (webview != null) {
                    webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();"));
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileProvider",
                                shareFile));
                share.setType("application/zip");
                context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            } catch (Exception e) {
                if (webview != null) {
                    webview.post(() -> webview
                            .loadUrl("javascript:navigator.notification.activityStop();alert('" + e + "')"));
                }
                Log.e("shareExtension", String.valueOf(e));
            }
        }).start();
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public String sendUpdate() {
        final String SCHEME_HTTP = "http";
        final String SCHEME_HTTPS = "https";
        final String DEFAULT_HOSTNAME = "localhost";

        String scheme = preferences.getString("scheme", SCHEME_HTTPS).toLowerCase();
        String hostname = preferences.getString("hostname", DEFAULT_HOSTNAME).toLowerCase();

        context.getSharedPreferences("nonameyuri", MODE_PRIVATE)
                .edit()
                .putString("updateProtocol", scheme)
                .apply();

        if (!scheme.contentEquals(SCHEME_HTTP) && !scheme.contentEquals(SCHEME_HTTPS)) {
            Log.d("JavaScriptInterface", "The provided scheme \"" + scheme + "\" is not valid. " +
                    "Defaulting to \"" + SCHEME_HTTPS + "\". " +
                    "(Valid Options=" + SCHEME_HTTP + "," + SCHEME_HTTPS + ")");

            scheme = SCHEME_HTTPS;
        }

        Log.e("sendUpdate", scheme + "://" + hostname + '/');

        return scheme + "://" + hostname + '/';
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public String getPackageName() {
        return context.getPackageName();
    }

    // 可使用FileObserver实现文件监听功能

    // activity.getCallingPackage()

    @JavascriptInterface
    @SuppressWarnings("unused")
    public boolean captureScreen(String fileName) {
        return Utils.captureAndSaveScreenshot(activity, fileName);
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void changeWebviewProvider() {
        Intent newIntent = new Intent(context, WebViewSelectionActivity.class);
        newIntent.setAction(Intent.ACTION_VIEW);
        activity.startActivity(newIntent);
    }
}
