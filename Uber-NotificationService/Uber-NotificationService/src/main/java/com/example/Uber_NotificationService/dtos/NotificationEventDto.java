package com.example.Uber_NotificationService.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class NotificationEventDto {
    @NotBlank(message = "eventType is required")
    private String eventType;
    @NotNull(message = "userId is required")
    private Long userId;
    @NotBlank(message = "userType is required (e.g. PASSENGER)")
    private String userType;
    private Map<String, Object> payload;
}
