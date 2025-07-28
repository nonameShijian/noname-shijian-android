package com.noname.shijian;

import com.noname.core.application.NonameCoreApplication;

public class UpdateDataApplication extends NonameCoreApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (getClass().getSuperclass() != NonameCoreApplication.class) {
            throw new RuntimeException("this class is not my UpdateDataApplication");
        }
    }
}
