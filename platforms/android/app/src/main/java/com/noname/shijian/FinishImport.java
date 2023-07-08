package com.noname.shijian;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FinishImport extends CordovaPlugin {
    public static String ext = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.e("ready", "触发ready: " + action);
        switch (action) {
            case "importReady": {
                Map<String, String> data = this.importReady();
                JSONObject r = new JSONObject(data);
                if (!"error".equals(data.get("type"))) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, r);
                    callbackContext.sendPluginResult(result);
                } else {
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, r);
                    callbackContext.sendPluginResult(result);
                }
                return true;
            }
            case "importReceived":
                ext = null;
                callbackContext.success();
                return true;
            case "environment": {
                // 是否是测试环境
                Map<String, String> data = new HashMap<String, String>();
                data.put("type", "environment");
                data.put("message", "false");
                // data.put("message", "true");
                JSONObject r = new JSONObject(data);
                PluginResult result = new PluginResult(PluginResult.Status.OK, r);
                callbackContext.sendPluginResult(result);
                return true;
            }
            case "listView": {
                callbackContext.success();
                Intent intent = new Intent(cordova.getContext(), ListViewActivity.class);
                // cordova.getContext().startActivity(intent);
                intent.putExtra("type", (String) args.get(0));
                intent.putExtra("readFile", (String) args.get(1));
                // Log.e("listViewArgs", args.toString());
                // Log.e("listViewArgs1", (String) args.get(1));
                cordova.getActivity().startActivityForResult(intent, 2);
                return true;
            }
            case "checkAppUpdate": {
                Log.e("checkAppUpdate", "checkAppUpdate");
                // 储存的版本号
                long appVersion = cordova.getContext().getSharedPreferences("nonameshijian", cordova.getContext().MODE_PRIVATE).getLong("version",10000);
                // 目前的版本号
                long version = getAppVersion(cordova.getContext());

                Log.e("appVersion", String.valueOf(appVersion));
                Log.e("version", String.valueOf(version));
                // Log.e("NonameImportActivity", String.valueOf(NonameImportActivity.VERSION));

                if (appVersion < NonameImportActivity.VERSION) {
                    Intent intent = new Intent(cordova.getContext(), NonameImportActivity.class);
                    cordova.getActivity().startActivity(intent);
                }
                callbackContext.success();
                return true;
            }
            case "assetZip": {
                Intent intent = new Intent(cordova.getContext(), NonameImportActivity.class);
                intent.putExtra("unzip", "true");
                cordova.getActivity().startActivity(intent);
                callbackContext.success();
                return true;
            }
            case "resetGame": {
                cordova.getContext().getSharedPreferences("nonameshijian", /*MODE_PRIVATE*/ 0).edit().putLong("version", 10000).apply();
            }
            case "requestMediaRecord": {
                MainActivity activity = (MainActivity) cordova.getActivity();
                if (activity != null) {
                    try {
                        // 初始化
                        activity.startService(new Intent(activity, ScreenRecordingService.class));
                        MainActivity.mProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        // 执行
                        activity.startActivityForResult(MainActivity.mProjectionManager.createScreenCaptureIntent(), MainActivity.MediaRecord_REQUEST_CODE);
                        callbackContext.success();
                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }
                } else {
                    Log.e("requestMediaRecord", "未获取到activity实例");
                    callbackContext.error("未获取到activity实例");
                }
                return true;
            }
            case "stopMediaRecord": {
                MainActivity.stopRecording(true);
                callbackContext.success();
                return true;
            }
        }

        return super.execute(action, args, callbackContext);
    }

    private Map<String, String> importReady() {
        Map<String, String> data = new HashMap<String, String>();
        if (ext != null) {
            if (ext.equals("importPackage")) {
                data.put("type", "package");
                String dataPath = "file://" + cordova.getContext().getExternalFilesDir(null).getParentFile().getAbsolutePath() + '/';
                data.put("message", dataPath);
            } else {
                data.put("type", "extension");
                data.put("message", ext);
            }
        } else {
            data.put("type", "error");
            data.put("message", "ext is null");
        }
        Log.e("type", data.get("type") );
        Log.e("message", data.get("message") );
        return data;
    }

    // 获取apk版本
    // 因为低版本不兼容某些函数
    public static long getAppVersion(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            Log.e("getPackageInfo", info.toString());
            String[] v = info.versionName.split("\\.");
            long version = 0L;
            for (int i = 0; i < v.length; i++) {
                version += Integer.parseInt(v[i]) * 10000L / (int) Math.pow(10, i);
            }
            NonameImportActivity.VERSION = version;
            Log.e("getAppVersion", String.valueOf(version));
            return version;
        } catch (Exception e) {
            Log.e("getPackageInfo", e.getMessage());
            return NonameImportActivity.VERSION;
        }
    }

    /*
    // 把path转换为以供访问Android/data
    private static Uri getUri(String path) {
        String[] paths = path.replaceAll("/storage/emulated/0/Android/data", "").split("/");
        StringBuilder stringBuilder = new StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata");
        for (String p : paths) {
            if (p.length() == 0) continue;
            stringBuilder.append("%2F").append(p);
        }
        return Uri.parse(stringBuilder.toString());
    }
    */
}
