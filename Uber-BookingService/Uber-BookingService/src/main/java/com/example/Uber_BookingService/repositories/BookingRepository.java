package com.example.Uber_BookingService.repositories;


import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.Booking;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.bookingStatus = :status , b.driver = :driver  WHERE b.id = :id ")
    void updateBookingStatusAndDriverById(@Param("id") Long id, @Param("status") BookingStatus status, @Param("driver") Driver driver);

    @Query("SELECT b FROM Booking b WHERE b.passenger.id = :passengerId ORDER BY b.createdAt DESC")
    List<Booking> findByPassengerId(@Param("passengerId") Long passengerId);

    @Query("SELECT b FROM Booking b WHERE b.driver.id = :driverId ORDER BY b.createdAt DESC")
    List<Booking> findByDriverId(@Param("driverId") Long driverId);

    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.bookingStatus = :status WHERE b.id = :id ")
    void updateBookingStatusById(@Param("id") Long id, @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.passenger.id = :passengerId AND b.bookingStatus IN :statuses")
    long countByPassengerIdAndBookingStatusIn(@Param("passengerId") Long passengerId,
                                              @Param("statuses") Collection<BookingStatus> statuses);
}
