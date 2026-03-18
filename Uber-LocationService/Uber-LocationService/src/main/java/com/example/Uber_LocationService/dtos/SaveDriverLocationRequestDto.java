package com.example.Uber_LocationService.dtos;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDriverLocationRequestDto {
    String driverId;
    Double latitude;
    Double longitude;
}