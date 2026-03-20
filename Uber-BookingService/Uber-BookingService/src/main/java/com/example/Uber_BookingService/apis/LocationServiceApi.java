package com.example.Uber_BookingService.apis;

import com.example.Uber_BookingService.dtos.DriverLocationDto;
import com.example.Uber_BookingService.dtos.NearbyDriversRequestDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LocationServiceApi {

    @POST("/api/location/nearby/drivers")
    Call<DriverLocationDto[]> getNearbyDrivers(@Body NearbyDriversRequestDto requestDto);
}
