package com.noname.shijian;

import static android.content.Context.MODE_PRIVATE;

import static com.noname.shijian.MainActivity.FILE_CHOOSER_RESULT_CODE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileOutputStream;

import io.github.pwlin.cordova.plugins.fileopener2.FileProvider;

/**
 * 在js中添加window.noname_shijianInterfaces
 * */
public class JavaScriptInterface {

    private final Context context;
    private final MainActivity activity;
    private final WebView webview;

    public JavaScriptInterface(Context context, MainActivity activity, WebView webview) {
        this.context = context;
        this.activity = activity;
        this.webview = webview;
        Log.e("new-JavaScriptInterface", String.valueOf(webview));
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public long getApkVersion() {
        return FinishImport.getAppVersion(context);
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void setApkVersion(long version) {
        context.getSharedPreferences("nonameshijian", MODE_PRIVATE)
                .edit()
                .putLong("version", version)
                .apply();
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void unzipFromBase64(@NonNull String fileName, @NonNull String base64) throws Exception {
        try {
            Log.e("unzipFromBase64", fileName + ".zip");
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            File tempFile = File.createTempFile(fileName, ".zip", context.getExternalCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            outputStream.write(data);
            outputStream.close();
            tempFile.deleteOnExit();
            Uri fileUri = Uri.fromFile(tempFile);
            // 处理文件Uri对象
            Intent intent = new Intent(context, NonameImportActivity.class);
            intent.setData(fileUri);
            intent.setAction(Intent.ACTION_VIEW);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("unzipFromBase64Error", String.valueOf(e));
            throw e;
        }
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void showToast(@NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public boolean shareFile(@NonNull String documentPath) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath();
        Log.e("shareImage", rootPath);
        Log.e("shareImage", documentPath);
        if (!documentPath.startsWith(rootPath)) {
            documentPath = rootPath + "/" + documentPath;
        }
        Log.e("shareImage", documentPath);
        File shareFile =  new File(documentPath);
        if (!shareFile.exists()) {
            Log.e("shareDocument", "文件不存在: " + documentPath);
            return false;
        } else if (shareFile.isDirectory()) {
            Log.e("shareDocument", "不能分享文件夹: " + documentPath);
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                    context.getApplicationContext(),
                    context.getPackageName() + ".fileProvider",
                    shareFile
            )
        );
        intent.setType("*/*");
        context.startActivity(Intent.createChooser(intent, "分享文件"));
        return true;
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public boolean shareExtension(@NonNull String extName) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath() + "/extension/";
        Log.e("shareExtension", rootPath);
        Log.e("shareExtension", extName);
        String extPath = extName;
        if (!extPath.startsWith(rootPath)) {
            extPath = rootPath + extPath;
        }
        Log.e("shareExtension", extPath);
        File shareExtDir =  new File(extPath);
        File[] files = shareExtDir.listFiles();
        if (!shareExtDir.exists()) {
            Log.e("shareExtension", "文件不存在: " + extPath);
            return false;
        } else if (shareExtDir.isFile()) {
            Log.e("shareExtension", "不能分享文件: " + extPath);
            return false;
        } else if (files == null) {
            Log.e("shareExtension", "shareExtDir.listFiles()为空: " + extPath);
            return false;
        }
        String zipName = extName + ".zip";
        File shareFile = new File(context.getExternalCacheDir(), zipName);
        if (shareFile.exists()) shareFile.delete();
        try {
            // webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStart('正在压缩扩展', '请稍候...');"));

            Log.e("shareExtension", "shareFile.getPath(): " + shareFile.getPath());
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
            // webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();"));

            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileProvider",
                    shareFile
                )
            );
            share.setType("application/zip");
            context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            return true;
        } catch (Exception e) {
            Log.e("shareExtension", String.valueOf(e));
            webview.post(() -> webview.loadUrl("javascript:alert('" + e + "')"));
            return false;
        }
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public boolean shareExtensionWithPassWord(@NonNull String extName, @NonNull String pwd)  {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath() + "/extension/";
        Log.e("shareExtension", rootPath);
        Log.e("shareExtension", extName);
        String extPath = extName;
        if (!extPath.startsWith(rootPath)) {
            extPath = rootPath + extPath;
        }
        Log.e("shareExtension", extPath);
        File shareExtDir =  new File(extPath);
        File[] files = shareExtDir.listFiles();
        if (!shareExtDir.exists()) {
            Log.e("shareExtension", "文件不存在: " + extPath);
            return false;
        } else if (shareExtDir.isFile()) {
            Log.e("shareExtension", "不能分享文件: " + extPath);
            return false;
        } else if (files == null) {
            Log.e("shareExtension", "shareExtDir.listFiles()为空: " + extPath);
            return false;
        }
        String zipName = extName + "(密码: " + pwd + ")" + ".zip";
        File shareFile = new File(context.getExternalCacheDir(), zipName);
        if (shareFile.exists()) shareFile.delete();
        try {
            Log.e("shareExtension", "pwd: " + pwd);
            Log.e("shareExtension", "pwd.toCharArray(): " + String.valueOf(pwd.toCharArray()));
            // 压缩文件
            ZipFile zipFile = new ZipFile(shareFile);
            zipFile.setPassword(pwd.toCharArray());
            Log.e("shareExtension", "zipFile.isEncrypted(): " + zipFile.isEncrypted());
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

            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileProvider",
                    shareFile
                )
            );
            share.setType("application/zip");
            context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            return true;
        } catch (Exception e) {
            Log.e("shareExtension", String.valueOf(e));
            webview.post(() -> webview.loadUrl("javascript:alert('" + e + "')"));
            return false;
        }
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void shareExtensionAsync(@NonNull String extName) {
        String rootPath = context.getExternalCacheDir().getParentFile().getAbsolutePath() + "/extension/";
        Log.e("shareExtension", rootPath);
        Log.e("shareExtension", extName);
        String extPath = extName;
        if (!extPath.startsWith(rootPath)) {
            extPath = rootPath + extPath;
        }
        Log.e("shareExtension", extPath);
        File shareExtDir =  new File(extPath);
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
        if (shareFile.exists()) shareFile.delete();

        webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStart('正在压缩扩展', '请稍候...');"));
        Log.e("shareExtension", "shareFile.getPath(): " + shareFile.getPath());
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
                webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();"));

                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileProvider",
                        shareFile
                    )
                );
                share.setType("application/zip");
                context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            } catch (Exception e) {
                webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();alert('" + e + "')"));
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
        File shareExtDir =  new File(extPath);
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
        if (shareFile.exists()) shareFile.delete();

        webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStart('正在压缩扩展', '请稍候...');"));
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
                webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();"));

                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileProvider",
                        shareFile
                    )
                );
                share.setType("application/zip");
                context.startActivity(Intent.createChooser(share, "分享扩展压缩包"));
            } catch (Exception e) {
                webview.post(() -> webview.loadUrl("javascript:navigator.notification.activityStop();alert('" + e + "')"));
                Log.e("shareExtension", String.valueOf(e));
            }
        }).start();
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void selectZipToExtract() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        activity.startActivityForResult(Intent.createChooser(intent, "选择zip文件并解压"), FILE_CHOOSER_RESULT_CODE);
    }
}
