package com.example.Uber_DriverService.repositories;
import com.example.Uber_EntityService.Models.Car;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
}
