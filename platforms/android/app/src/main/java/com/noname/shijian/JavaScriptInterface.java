package com.noname.shijian;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 在js中添加window.noname_shijianInterfaces
 * */
public class JavaScriptInterface {

    private Context context;
    private MainActivity activity;
    private WebView webView;

    public JavaScriptInterface(Context context, MainActivity activity, WebView webview) {
        this.context = context;
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public long getApkVersion() {
        return FinishImport.getAppVersion(context);
    }

    @JavascriptInterface
    public void setApkVersion(long version) {
        context.getSharedPreferences("nonameshijian", MODE_PRIVATE)
                .edit()
                .putLong("version", version)
                .apply();
    }

    /*
    @JavascriptInterface
    public void unzipFromBase64(String fileName, String base64) {
        if (fileName == null) fileName = "import.zip";
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        File file = new File(context.getExternalCacheDir(), fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();

            Uri fileUri = Uri.fromFile(file);
            // 处理文件Uri对象
            Intent intent = new Intent(context, NonameImportActivity.class);
            intent.setData(fileUri);
            intent.setAction(Intent.ACTION_VIEW);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("unzipFromBase64", String.valueOf(e));
        }
    }
    */
}
