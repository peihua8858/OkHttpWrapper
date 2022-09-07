package com.fz.okhttp.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.fz.okhttp.OkCallback;
import com.fz.okhttp.OkHttpCallback;
import com.fz.okhttp.OkHttpCallbackC;
import com.fz.okhttp.OkHttpProxy;
import com.fz.okhttp.params.OkRequestParams;

import java.io.IOException;
import java.text.MessageFormat;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String LOG_HOST = "http://{0}:8090/";
    private String POST_EVENT_LOG = "analysis/appsflyer_post/{0}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private String getUrl(String serverIp, String eventName) {
        String host = MessageFormat.format(LOG_HOST, serverIp);
        String path = MessageFormat.format(POST_EVENT_LOG, eventName);
        return host + path;
    }

    public void onClick(View view) {
        OkRequestParams param = new OkRequestParams();
        param.put("eventName", "logTag-eventName");
        param.put("platform", "Android-logTag");
        param.put("domain", "");
        param.put("content", "123333333333333");
        param.put("appType", "");
        param.put("versionName", "");
        param.put("sdkName", "");
        param.put("sdkVersion", "");
        OkHttpProxy.getInstance().post(getUrl("10.8.31.5", "test_event"), param,new OkHttpCallbackC<String>() {
            @Override
            public void onSuccess(@Nullable String response) {

            }

            @Override
            public void onFailure(@Nullable Throwable e) {

            }
//
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//
//            }
        }); /*{

//            @Override
//            public void onFailure(@Nullable Throwable e) {
//                KLog.d("logTag", "onFailure>>>>request:" + param + ",'\nerror:" + e.getMessage());
//            }
//
//            @Override
//            public void onSuccess(@Nullable String response) {
//                KLog.d("logTag", "onSuccess>>>>request:" + param + ",'\nresponse:" + response);
//            }
        });*/
    }
}