package com.example.Uber_EntityService.Models;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.Random;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OTP extends BaseModel {

    private String code;

    private String sentToNumber;

    public static OTP make(String phoneNumber) {

        Random random = new Random();
        Integer code = random.nextInt(9000) + 1000;

        return OTP.builder()
                .code(code.toString())
                .sentToNumber(phoneNumber).build();
    }



}
