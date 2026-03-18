package com.example.Uber_ServiceDiscovery_Eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class UberServiceDiscoveryEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(UberServiceDiscoveryEurekaApplication.class, args);
	}

}
