package com.example.Uber_BookingService.repositories;


import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.bookingStatus = :status , b.driver = :driver  WHERE b.id = :id ")
    void updateBookingStatusAndDriverById(@Param("id") Long id, @Param("status") BookingStatus status, @Param("driver") Driver driver);

}
