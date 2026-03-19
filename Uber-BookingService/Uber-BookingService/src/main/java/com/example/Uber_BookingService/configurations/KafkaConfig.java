package com.example.Uber_BookingService.configurations;

import com.example.Uber_BookingService.dtos.RideResponseDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic rideRequestTopic() {
        return new NewTopic("ride-request-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic rideResponseTopic() {
        return new NewTopic("ride-response-topic", 1, (short) 1);
    }

    // ================= PRODUCER =================
    // Booking service PRODUCES ride requests → to SocketKafka

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // ✅ JSON, not String

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ================= CONSUMER =================
    // Booking service CONSUMES ride responses ← from SocketKafka

    @Bean
    public ConsumerFactory<String, RideResponseDto> consumerFactory() {

        JsonDeserializer<RideResponseDto> deserializer =
                new JsonDeserializer<>(RideResponseDto.class);

        deserializer.addTrustedPackages("*"); // ✅ Trust all packages (cross-service)

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "booking-group"); // ✅ Unique group ID
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RideResponseDto>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, RideResponseDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}