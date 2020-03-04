package com.welcome.gpsmocktest.net;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface Api {


    @GET("geoconv/v1/?from=5&to=3")
    public Observable<Object> transformCoordinate(@Query("coords") String coords, @Query("ak") String ak, @Query("mcode") String mcode);
}
