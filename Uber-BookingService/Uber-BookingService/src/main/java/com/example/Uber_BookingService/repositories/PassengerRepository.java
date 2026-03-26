package com.example.Uber_BookingService.repositories;

import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM Passenger p
            LEFT JOIN FETCH p.activeBooking
            WHERE p.id = :id
            """)
    Optional<Passenger> findWithActiveBooking(@Param("id") Long id);
}
