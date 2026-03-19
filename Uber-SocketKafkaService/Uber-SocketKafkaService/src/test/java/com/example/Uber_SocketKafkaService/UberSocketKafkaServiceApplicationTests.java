package com.example.Uber_SocketKafkaService;

import com.example.Uber_SocketKafkaService.consumers.RideRequestTestConsumer;
import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UberSocketKafkaServiceApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RideRequestTestConsumer consumer;

	@Autowired
	private ObjectMapper objectMapper;

	private String baseUrl;

	@BeforeEach
	void setup() throws Exception {
		baseUrl = "http://localhost:" + port;
		consumer.reset();
		// Wait for consumer to be fully assigned and positioned at 'latest'
		// before we produce the test message
		Thread.sleep(8000);
	}

	@Test
	void testRideRequestFlow_endToEnd() throws Exception {

		RideRequestDto request = RideRequestDto.builder()
				.passengerId(1L)
				.driverIds(List.of(101L, 102L))
				.bookingId(999L)
				.build();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity =
				new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

		// Call the API — this produces the Kafka message
		ResponseEntity<String> response = restTemplate.postForEntity(
				baseUrl + "/api/socket/newride",
				entity,
				String.class
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		// Wait for Kafka consumption (up to 15 seconds)
		boolean messageConsumed = consumer.getLatch().await(15, TimeUnit.SECONDS);

		assertTrue(messageConsumed, "Kafka message was NOT consumed");

		RideRequestDto received = consumer.getReceivedMessage().get();

		assertNotNull(received);
		assertEquals(1L, received.getPassengerId());
		assertEquals(999L, received.getBookingId());
		assertEquals(2, received.getDriverIds().size());
	}
}
