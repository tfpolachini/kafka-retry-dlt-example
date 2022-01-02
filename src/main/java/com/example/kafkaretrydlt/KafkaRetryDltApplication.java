package com.example.kafkaretrydlt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class KafkaRetryDltApplication {

  public static void main(String[] args) {
    SpringApplication.run(KafkaRetryDltApplication.class, args);
  }

}
