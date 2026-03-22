package com.example.Uber_DriverService.apis;

import com.example.Uber_DriverService.dtos.SaveDriverLocationRequestDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LocationServiceApi {

    @POST("api/location/drivers")
    Call<Boolean> saveDriverLocation(@Body SaveDriverLocationRequestDto request);
}
