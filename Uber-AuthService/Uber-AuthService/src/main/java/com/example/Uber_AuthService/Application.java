package com.example.Uber_AuthService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan("com.example.Uber_EntityService.Models")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
