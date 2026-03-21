package com.example.Uber_BookingService.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationEventDto {
    private String eventType;
    private Long userId;
    private String userType;
    private Map<String, Object> payload;
}
