package com.example.Uber_EntityService.Models;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_review")
@Inheritance(strategy = InheritanceType.JOINED)
public class Review extends BaseModel {

    @Column(nullable = false)
    private String content;

    private Double rating;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Booking booking;

    @Override
    public String toString() {
        return "Review: " + this.content + " " + this.rating + " " + " booking: " + this.booking.getId() + " " + this.createdAt;
    }

}
