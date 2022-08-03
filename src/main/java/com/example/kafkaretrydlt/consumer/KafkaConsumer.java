package com.example.kafkaretrydlt.consumer;

import com.example.kafkaretrydlt.entities.User;
import com.example.kafkaretrydlt.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

  Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  private final KafkaProducer kafkaProducer;

  public KafkaConsumer(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  @KafkaListener(topics = "${kafka.consumer.topic}", containerFactory = "kafkaListenerContainerFactory", groupId = "main-group")
  public void kafkaListener(User message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            Acknowledgment acknowledgment) {
    log.info("Receive message [" + message + "] from topic : [" + topic + "]");

    try {
      if (true) throw new RuntimeException("Exception");
      acknowledgment.acknowledge();
    } catch (Exception e) {
      kafkaProducer.retry(message);
      acknowledgment.acknowledge();
    }
  }

  @KafkaListener(topics = "${kafka.retryTopic}", containerFactory = "kafkaRetryListenerContainerFactory", groupId = "retry-group")
  public void kafkaRetryListener(User message,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {

    log.info("Receive message [" + message + "] from topic : [" + topic + "]");

    if (true) throw new RuntimeException("Exception on Retry");

    acknowledgment.acknowledge();
  }

  @KafkaListener(topics = "${kafka.dltTopic}", containerFactory = "kafkaListenerContainerFactory", groupId = "dlt-group")
  public void dlt(User message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.info("Receive message [" + message + "] from topic : [" + topic + "]");
  }
}
