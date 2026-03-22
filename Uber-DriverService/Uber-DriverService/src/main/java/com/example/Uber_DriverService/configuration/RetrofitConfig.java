package com.example.Uber_DriverService.configuration;

import com.example.Uber_DriverService.apis.BookingServiceApi;
import com.example.Uber_DriverService.apis.LocationServiceApi;
import com.netflix.discovery.EurekaClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.URI;

@Configuration
public class RetrofitConfig {

    @Autowired
    private EurekaClient eurekaClient;

    private String getServiceUrl(String serviceName) {
        String url = eurekaClient.getNextServerFromEureka(serviceName, false).getHomePageUrl();
        return url.endsWith("/") ? url : url + "/";
    }

    private OkHttpClient buildLazyClient(String eurekaServiceName) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String baseUrl = getServiceUrl(eurekaServiceName);
                    URI uri = URI.create(baseUrl);
                    Request original = chain.request();
                    HttpUrl newUrl = original.url().newBuilder()
                            .scheme(uri.getScheme())
                            .host(uri.getHost())
                            .port(uri.getPort() == -1 ? 80 : uri.getPort())
                            .build();
                    return chain.proceed(original.newBuilder().url(newUrl).build());
                })
                .build();
    }

    @Bean
    public LocationServiceApi locationServiceApi() {
        return new Retrofit.Builder()
                .baseUrl("http://placeholder/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildLazyClient("UBER-LOCATIONSERVICE"))  // uppercase, matches Eureka
                .build()
                .create(LocationServiceApi.class);
    }

    @Bean
    public BookingServiceApi bookingServiceApi() {
        return new Retrofit.Builder()
                .baseUrl("http://placeholder/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildLazyClient("UBER-BOOKINGSERVICE"))  // uppercase, matches Eureka
                .build()
                .create(BookingServiceApi.class);
    }
}