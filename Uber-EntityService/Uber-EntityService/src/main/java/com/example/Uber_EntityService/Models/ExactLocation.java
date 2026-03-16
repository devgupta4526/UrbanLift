package com.example.Uber_EntityService.Models;


import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExactLocation extends BaseModel{

    private Double latitude;
    private Double longitude;

}
