package com.example.Uber_DriverService.apis;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;
import java.util.Map;

public interface BookingServiceApi {

    @GET("api/v1/booking/driver/{driverId}")
    Call<List<Map<String, Object>>> getBookingsByDriver(@Path("driverId") Long driverId);
}

