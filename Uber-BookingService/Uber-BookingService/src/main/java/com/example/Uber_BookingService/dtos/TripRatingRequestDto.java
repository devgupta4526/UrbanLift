package com.example.Uber_BookingService.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripRatingRequestDto {
    @NotNull
    private Long actorId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    private String comment;
}
