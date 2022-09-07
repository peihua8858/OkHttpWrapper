package com.fz.okhttp;

import androidx.annotation.Nullable;

public abstract class OkHttpCallbackC<T> implements OkCallback<T> {
    @Override
    public void onSuccess(@Nullable T response) {

    }

    @Override
    public void onFailure(@Nullable Throwable e) {

    }
//
//    @Override
//    public void onFailure(@NonNull Call call, @NonNull IOException e) {
//
//    }
//
//    @Override
//    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//
//    }
}