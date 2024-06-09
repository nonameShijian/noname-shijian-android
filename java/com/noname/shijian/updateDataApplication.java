package com.noname.shijian;

import android.app.Application;

public class updateDataApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        String protocol = getSharedPreferences("nonameshijian", MODE_PRIVATE).getString("updateProtocol", "file");
        String protocol2 = getSharedPreferences("nonameyuri", MODE_PRIVATE).getString("updateProtocol", "file");
        if (protocol.startsWith("http")) {
            getSharedPreferences("nonameyuri", MODE_PRIVATE)
                    .edit()
                    .putString("updateProtocol", protocol)
                    .apply();
        }
        else if (protocol2.startsWith("http")) {
            getSharedPreferences("nonameshijian", MODE_PRIVATE)
                    .edit()
                    .putString("updateProtocol", protocol)
                    .apply();
        }
    }
}
