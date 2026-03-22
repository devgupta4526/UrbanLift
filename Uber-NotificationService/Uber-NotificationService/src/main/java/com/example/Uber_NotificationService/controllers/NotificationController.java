package com.example.Uber_NotificationService.controllers;

import com.example.Uber_NotificationService.dtos.NotificationEventDto;
import com.example.Uber_NotificationService.services.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationEventDto event) {
        notificationService.sendNotification(event);
        return ResponseEntity.ok("Notification sent successfully");
    }
}