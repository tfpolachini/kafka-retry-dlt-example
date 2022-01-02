package com.example.kafkaretrydlt.controller;

import com.example.kafkaretrydlt.entities.User;
import com.example.kafkaretrydlt.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserTestController {

    private final KafkaProducer kafkaProducer;

    public UserTestController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/produce")
    public ResponseEntity<Void> produce(@RequestBody User user) {

        kafkaProducer.produce(user);

        return ResponseEntity.ok().build();
    }
}
