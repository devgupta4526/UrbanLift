package com.example.Uber_AuthService.dto;


import com.example.Uber_EntityService.Models.Passenger;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDto {
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String address;

    private Double rating;

    private Date createdAt;

    public static PassengerDto from(Passenger passenger) {
        if (passenger == null) {
            return null;
        }

        return PassengerDto.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phoneNumber(passenger.getPhoneNumber())
                .address(passenger.getAddress())
                .rating(passenger.getRating())
                .createdAt(passenger.getCreatedAt())
                .build();
    }
}
