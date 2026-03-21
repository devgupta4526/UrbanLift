package com.example.Uber_BookingService.controllers;

import com.example.Uber_BookingService.apis.LocationServiceApi;
import com.example.Uber_BookingService.apis.UberSocketApi;
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
        return eurekaClient.getNextServerFromEureka(serviceName, false).getHomePageUrl();
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

                    Request newRequest = original.newBuilder()
                            .url(newUrl)
                            .build();

                    return chain.proceed(newRequest);
                })
                .build();
    }

    @Bean
    public LocationServiceApi locationServiceApi() {
        return new Retrofit.Builder()
                .baseUrl("http://placeholder/")   // replaced at call time by the interceptor
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildLazyClient("LOCATIONSERVICE"))
                .build()
                .create(LocationServiceApi.class);
    }

    @Bean
    public UberSocketApi uberSocketApi() {
        return new Retrofit.Builder()
                .baseUrl("http://placeholder/")   // replaced at call time by the interceptor
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildLazyClient("UBERSOCKETSERVER"))
                .build()
                .create(UberSocketApi.class);
    }
}