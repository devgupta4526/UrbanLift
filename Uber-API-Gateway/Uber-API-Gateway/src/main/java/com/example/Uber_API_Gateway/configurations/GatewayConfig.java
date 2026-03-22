package com.example.Uber_API_Gateway.configurations;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .filters(f -> f.rewritePath("/auth/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-AuthService"))
                .route("booking-service", r -> r.path("/booking/**")
                        .filters(f -> f.rewritePath("/booking/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-BookingService"))
                .route("driver-service", r -> r.path("/driver/**")
                        .filters(f -> f.rewritePath("/driver/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-DriverService"))
                .route("location-service", r -> r.path("/location/**")
                        .filters(f -> f.rewritePath("/location/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-LocationService"))
                .route("socket-service", r -> r.path("/socket/**")
                        .filters(f -> f.rewritePath("/socket/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-SocketKafkaService"))
                .route("payment-service", r -> r.path("/payment/**")
                        .filters(f -> f.rewritePath("/payment/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-PaymentService"))
                .route("notification-service", r -> r.path("/notification/**")
                        .filters(f -> f.rewritePath("/notification/(?<path>.*)", "/${path}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://Uber-NotificationService"))
                .build();
    }
}
