package com.example.Uber_SocketKafkaService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan("com.example.uberentityservice.models")
public class UberSocketKafkaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UberSocketKafkaServiceApplication.class, args);
	}

}
