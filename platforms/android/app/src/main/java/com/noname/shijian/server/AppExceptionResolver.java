package com.noname.shijian.server;

import androidx.annotation.NonNull;
import com.noname.shijian.JsonUtils;
import com.yanzhenjie.andserver.annotation.Resolver;
import com.yanzhenjie.andserver.error.HttpException;
import com.yanzhenjie.andserver.framework.ExceptionResolver;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.StatusCode;

@Resolver
public class AppExceptionResolver implements ExceptionResolver {

    @Override
    public void onResolve(@NonNull HttpRequest request, @NonNull HttpResponse response, @NonNull Throwable e) {
        e.printStackTrace();
        if (e instanceof HttpException) {
            HttpException exception = (HttpException) e;
            response.setStatus(exception.getStatusCode());
        } else {
            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
        }
        String body = JsonUtils.failedJson(response.getStatus(), e.getMessage());
        response.setBody(new JsonBody(body));
    }
}