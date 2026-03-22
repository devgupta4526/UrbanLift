package com.example.Uber_DriverService.configuration;


import com.example.Uber_DriverService.apis.BookingServiceApi;
import com.example.Uber_DriverService.apis.LocationServiceApi;
import com.netflix.discovery.EurekaClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Configuration
public class RetrofitConfig {

    @Autowired
    private EurekaClient eurekaClient;

    private String getServiceUrl(String serviceName){
        String url = eurekaClient.getNextServerFromEureka(serviceName,false).getHomePageUrl();
        return url.endsWith("/") ? url : url + "/";
    }

    @Bean
    public LocationServiceApi  locationServiceApi(){
        return new Retrofit.Builder()
                .baseUrl(getServiceUrl("Uber-LocationService"))
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(LocationServiceApi.class);
    }

    @Bean
    public BookingServiceApi  bookingServiceApi(){
        return new Retrofit.Builder()
                .baseUrl(getServiceUrl("Uber_BookingService"))
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(BookingServiceApi.class);
    }
}
