package com.noname.core.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.noname.core.R;

import java.util.HashMap;
import java.util.Map;

@SuppressLint("WebViewApiAvailability")
public class WebViewSelectionActivity extends Activity {

    private static final String TAG = "WebViewSelectionActivity";

    public static final String[] WEBVIEW_PACKAGES = {
            "com.android.chrome",
            "com.android.webview",
            "com.google.android.webview",
            "com.google.android.webview.beta",
            "com.google.android.webview.canary",
            "com.google.android.webview.dev",
            "com.huawei.webview",
            "org.bromite.webview"
    };

    public static PackageInfo SYSTEM_WEB_VIEW_PACKAGE_INFO;

    public static String SELECTED_WEBVIEW_PACKAGE;

    private final Map<String, LinearLayout> map = new HashMap<>();

    private static final String NOT_INSTALL = "未安装该应用";

    private static final String UNSUPPORTED = "不支持此版本";

    private static final String NOT_ENABLED = "未启用此应用";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_selector);

        getSystemWebviewPackageInfo();

        if (SELECTED_WEBVIEW_PACKAGE == null) {
            SELECTED_WEBVIEW_PACKAGE = getSharedPreferences("nonameyuri", MODE_PRIVATE)
                    .getString("selectedWebviewPackage", WEBVIEW_PACKAGES[0]);
        }

        LinearLayout webViewContainer = findViewById(R.id.web_view_container);

        for (String packageName : WEBVIEW_PACKAGES) {
            LinearLayout webViewLayout = addWebViewOption(webViewContainer, packageName);
            map.put(packageName, webViewLayout);
            updateWebViewOption(webViewLayout, packageName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSystemWebviewPackageInfo();

        for (String packageName : WEBVIEW_PACKAGES) {
            updateWebViewOption(map.get(packageName), packageName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (SYSTEM_WEB_VIEW_PACKAGE_INFO != null &&
                    !SYSTEM_WEB_VIEW_PACKAGE_INFO.packageName.equals(SELECTED_WEBVIEW_PACKAGE)) {
                restartApp();
            }
        }
        else {
            restartApp();
        }
    }

    private void getSystemWebviewPackageInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                SYSTEM_WEB_VIEW_PACKAGE_INFO = WebView.getCurrentWebViewPackage();
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (SYSTEM_WEB_VIEW_PACKAGE_INFO != null) {
            Log.e(TAG, SYSTEM_WEB_VIEW_PACKAGE_INFO.packageName);
        }
    }

    private LinearLayout addWebViewOption(LinearLayout container, String packageName) {
        LinearLayout webViewLayout = new LinearLayout(this);
        webViewLayout.setClickable(true);
        webViewLayout.setFocusable(true);
        webViewLayout.setOrientation(LinearLayout.HORIZONTAL);
        webViewLayout.setPadding(16, 16, 16, 30);
        webViewLayout.setBackgroundResource(R.drawable.item_background);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = 25; // 下边距
        webViewLayout.setLayoutParams(layoutParams);

        // 设置 LayoutParams 以便于控制视图间的宽度分配
        LinearLayout.LayoutParams paramsIcon = new LinearLayout.LayoutParams(0, 150, 1); // 图标
        LinearLayout.LayoutParams paramsTexts = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3); // 文本占3份
        LinearLayout.LayoutParams paramsRadioButton = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); // RadioButton 占1份


        ImageView iconView = new ImageView(this);
        iconView.setTag("packageIcon");
        iconView.setLayoutParams(paramsIcon);

        // 创建一个垂直方向的 LinearLayout 来包含两个 TextView
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(paramsTexts);

        TextView titleView = new TextView(this);
        titleView.setTag("packageName");
        titleView.setTextSize(18);
        titleView.setTextColor(getResources().getColor(R.color.primary_text_color));

        TextView subtitleView = new TextView(this);
        subtitleView.setTag("packageVersion");
        subtitleView.setTextSize(16);
        subtitleView.setTextColor(getResources().getColor(R.color.secondary_text_color));

        int defaultTextColor = titleView.getCurrentTextColor();

        textContainer.addView(titleView);
        textContainer.addView(subtitleView);

        RadioButton radioButton = new RadioButton(this);
        radioButton.setLayoutParams(paramsRadioButton);
        radioButton.setTag("choose");
        radioButton.setId(View.generateViewId());
        radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 取消其他选项的选择状态
                for (String packageName2 : WEBVIEW_PACKAGES) {
                    if (packageName.equals(packageName2)) continue;
                    LinearLayout webViewLayout2 = map.get(packageName2);
                    if (webViewLayout2 == null) continue;
                    RadioButton checkBox2 = webViewLayout2.findViewWithTag("choose");
                    checkBox2.setChecked(false);
                }

                titleView.setTextColor(0xFF55A7C3);
                subtitleView.setTextColor(0xFF55A7C3);

                SELECTED_WEBVIEW_PACKAGE = packageName;
                getSharedPreferences("nonameyuri", MODE_PRIVATE)
                        .edit()
                        .putString("selectedWebviewPackage", SELECTED_WEBVIEW_PACKAGE)
                        .apply();
            }
            else {
                titleView.setTextColor(defaultTextColor);
                subtitleView.setTextColor(defaultTextColor);
            }
        });

        // 当LinearLayout被点击时，模拟RadioButton被点击
        webViewLayout.setOnClickListener(v -> {
            if (radioButton.isEnabled()) {
                radioButton.performClick();
            }
        });

        if (SELECTED_WEBVIEW_PACKAGE.equals(packageName)) {
            radioButton.setChecked(true);
        }

        webViewLayout.addView(iconView);
        webViewLayout.addView(textContainer);
        webViewLayout.addView(radioButton);

        container.addView(webViewLayout);

        return webViewLayout;
    }

    @SuppressLint("SetTextI18n")
    private void updateWebViewOption(LinearLayout webViewLayout, String packageName) {
        if (webViewLayout == null) return;

        ImageView iconView = webViewLayout.findViewWithTag("packageIcon");
        if (iconView != null) {
            Drawable drawable = getDrawableForPackage(packageName);
            if (drawable != null) {
                iconView.setImageDrawable(drawable);
            } else {
                int resId = getResources().getIdentifier(packageName.toLowerCase().replace(".", "_"), "drawable", getPackageName());
                Drawable defaultDrawable;
                if (resId != 0) {
                    // 如果找到了对应包名的图片资源
                    defaultDrawable = getResources().getDrawable(resId, null);
                } else {
                    // 如果没有找到，则使用默认图标
                    defaultDrawable = getResources().getDrawable(R.drawable.com_google_android_webview, null);
                }
                iconView.setImageDrawable(defaultDrawable);
            }
        }

        TextView titleView = webViewLayout.findViewWithTag("packageName");
        if (titleView != null) {
            String appName = getAppName(packageName);
            if (appName != null) {
                titleView.setText(appName);
            }
            else {
                titleView.setText(packageName);
            }
        }

        TextView subtitleView = webViewLayout.findViewWithTag("packageVersion");
        String versionName = getVersionName(packageName);

        if (subtitleView != null) {
            if (SYSTEM_WEB_VIEW_PACKAGE_INFO != null && SYSTEM_WEB_VIEW_PACKAGE_INFO.packageName.equals(packageName)) {
                subtitleView.setText(versionName + "(正在使用)");
            }
            else {
                subtitleView.setText(versionName);
            }
        }

        RadioButton radioButton = webViewLayout.findViewWithTag("choose");
        boolean enable = !versionName.equals(NOT_INSTALL) &&
                !versionName.equals(UNSUPPORTED) &&
                !versionName.equals(NOT_ENABLED);
        radioButton.setEnabled(enable);

        // 调整滤镜
        if (iconView != null) {
            if (enable) {
                iconView.setColorFilter(null);
            }
            else {
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0); // 设置饱和度
                ColorMatrixColorFilter grayColorFilter = new ColorMatrixColorFilter(cm);
                iconView.setColorFilter(grayColorFilter);
            }
        }
    }

    private Drawable getDrawableForPackage(String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return appInfo.loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getVersionName(String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            if (packageInfo.applicationInfo != null) {
                if (!packageInfo.applicationInfo.enabled) {
                    return NOT_ENABLED;
                }
                boolean hasWebViewLibrary = hasWebViewLibrary(packageInfo.applicationInfo);
                if (hasWebViewLibrary) {
                    return packageInfo.versionName;
                }
                else {
                    return UNSUPPORTED;
                }
            }
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return NOT_INSTALL;
        }
    }

    private String getAppName(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            CharSequence appName = packageManager.getApplicationLabel(applicationInfo);
            return appName != null ? appName.toString() : null;
        } catch (PackageManager.NameNotFoundException ignored) {

        }
        return null;
    }

    private boolean hasWebViewLibrary(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData != null) {
            return applicationInfo.metaData.getString("com.android.webview.WebViewLibrary") != null;
        }
        return false;
    }

    public void restartApp() {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getPackageName());
        if (launchIntent != null) {
            // 设置Intent标志，确保新的Activity作为新的任务栈顶部运行，并清除当前任务栈
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // 启动主Activity
            startActivity(launchIntent);
        }

        try {
            // 杀掉当前进程
            android.os.Process.killProcess(android.os.Process.myPid());
            // 退出Java虚拟机
            System.exit(0);
        } catch (Exception e) {
            // 处理启动Activity时可能出现的异常
            e.printStackTrace();
        }
    }
}
