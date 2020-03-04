package com.welcome.gpsmocktest.net;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private OkHttpClient mOkHttpClient;

    private String baseUrl;

    public RetrofitClient(String baseUrl, @NonNull OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.mOkHttpClient = client;
    }

    public RetrofitClient setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public <T> T build(Class<T> service) {
        return build(service, true);
    }

    public <T> T build(Class<T> service, boolean useFast) {
        if (TextUtils.isEmpty(baseUrl)) {
            throw new RuntimeException("baseUrl is null!!!");
        }
        if (service == null) {
            throw new RuntimeException("api Service is null!!!");
        }
        if (mOkHttpClient == null) {
            throw new RuntimeException("OkHttpClient is null!!!");
        }
        Retrofit retrofit = new Retrofit.Builder()
                .client(mOkHttpClient)
                .baseUrl(baseUrl)
                .addConverterFactory(useFast ? FastJsonConverterFactory.create() : GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        return retrofit.create(service);
    }
}
