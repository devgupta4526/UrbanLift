package com.example.Uber_BookingService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Uber_EntityService.Models.Passenger;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}
