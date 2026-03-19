package com.example.Uber_SocketKafkaService.controllers;


import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import com.example.Uber_SocketKafkaService.dtos.RideResponseDto;
import com.example.Uber_SocketKafkaService.producers.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/socket")
public class DriverRequestController {


    private final KafkaProducerService kafkaProducerService;
    private final RestTemplate restTemplate =  new RestTemplate();
    public DriverRequestController(KafkaProducerService kafkaProducerService){
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/newride")
    public ResponseEntity<Boolean> raiseRideRequest(@RequestBody RideRequestDto requestDto) {

        System.out.println("Sending ride request to Kafka...");
        kafkaProducerService.sendRideRequest(requestDto);

        return ResponseEntity.ok(true);
    }


    @MessageMapping("/rideResponse/{userId}")
    public void rideResponseHandler(
            @DestinationVariable String userId,
            RideResponseDto responseDto) {

        System.out.println("Driver response: " + responseDto);

        // Send event to Kafka
        kafkaProducerService.sendRideResponse(responseDto);

        // Call booking service
        restTemplate.postForEntity(
                "http://localhost:8001/api/v1/booking/" + responseDto.getBookingId(),
                responseDto,
                String.class
        );
    }
}
