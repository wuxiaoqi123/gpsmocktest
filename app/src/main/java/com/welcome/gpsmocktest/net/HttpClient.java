package com.welcome.gpsmocktest.net;


import android.util.Log;

import com.welcome.gpsmocktest.BuildConfig;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpClient {

    private static final String BAIDU_API = "https://api.map.baidu.com/";

    private OkHttpClient mOkHttpClient;
    private static final int TIMEOUT = 30;

    private HttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.e("OKHTTP", message);
            }
        });
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        mOkHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new EncryptionInterceptor())
                .addInterceptor(logging) // 添加拦截器
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS) // 设置超时时间
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS) // 写入超时
                .readTimeout(TIMEOUT, TimeUnit.SECONDS) // 读取超时
//                .sslSocketFactory(createTrustAllSSLFactory())
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .retryOnConnectionFailure(true) // 遇到问题启动重复连接
                .build();

    }

//    public RetrofitClient getRetrofitClient() {
//        return getRetrofitClient(Config.BASE_URL);
//    }

    public RetrofitClient getRetrofitClient(String baseUrl) {
        return new RetrofitClient(baseUrl, mOkHttpClient);
    }

    public RetrofitClient getRetrofitClient() {
        return new RetrofitClient(BAIDU_API, mOkHttpClient);
    }

    private static class SingletonHolder {
        private static HttpClient httpClient = new HttpClient();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public static HttpClient getInstance() {
        return SingletonHolder.httpClient;
    }

    protected SSLSocketFactory createTrustAllSSLFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return ssfFactory;
    }

}