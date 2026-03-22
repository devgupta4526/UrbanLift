package com.example.Uber_DriverService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequestDto {
    private Double latitude;
    private Double longitude;
}
