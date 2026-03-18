package com.example.Uber_EntityService.Models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "passenger_review")
public class PassengerReview extends Review {
    @Column(nullable = false)
    private String passengerReviewContent;

    @Column(nullable = false)
    private String passengerRating;

}
