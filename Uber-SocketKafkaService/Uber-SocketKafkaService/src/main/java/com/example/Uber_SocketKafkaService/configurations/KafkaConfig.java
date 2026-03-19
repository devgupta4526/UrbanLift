package com.example.Uber_SocketKafkaService.configurations;


import com.fasterxml.jackson.databind.JsonSerializer;
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

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic rideRequestTopic(){
        return new NewTopic("ride-request-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic rideResponseTopic(){
        return new NewTopic("ride-response-topic", 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
     public ConsumerFactory<String, Object> consumerFactory(){
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages("*");
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "socket-group");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }


}
