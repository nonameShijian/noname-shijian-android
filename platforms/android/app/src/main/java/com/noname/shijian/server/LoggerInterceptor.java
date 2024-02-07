package com.noname.shijian.server;

import android.util.Log;

import androidx.annotation.NonNull;

import com.noname.shijian.JsonUtils;
import com.yanzhenjie.andserver.annotation.Interceptor;
import com.yanzhenjie.andserver.framework.HandlerInterceptor;
import com.yanzhenjie.andserver.framework.handler.RequestHandler;
import com.yanzhenjie.andserver.http.HttpMethod;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.util.MultiValueMap;

@Interceptor
public class LoggerInterceptor implements HandlerInterceptor {

    @Override
    public boolean onIntercept(@NonNull HttpRequest request, @NonNull HttpResponse response,
                               @NonNull RequestHandler handler) {
        String path = request.getPath();
        HttpMethod method = request.getMethod();
        MultiValueMap<String, String> valueMap = request.getParameter();
        Log.i("LoggerInterceptor","Path: " + path);
        Log.i("LoggerInterceptor","Method: " + method.value());
        Log.i("LoggerInterceptor","Param: " + JsonUtils.toJsonString(valueMap));
        return false;
    }
}