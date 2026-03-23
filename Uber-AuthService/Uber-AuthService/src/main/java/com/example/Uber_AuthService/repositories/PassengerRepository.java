package com.example.Uber_AuthService.repositories;

import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    @Query("SELECT p FROM Passenger p WHERE LOWER(TRIM(p.email)) = LOWER(TRIM(:email))")
    Optional<Passenger> findPassengerByEmail(@Param("email") String email);
}
