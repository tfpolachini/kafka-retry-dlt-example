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

  @KafkaListener(topics = "${kafka.consumer.topic}", containerFactory = "kafkaListenerContainerFactory", groupId = "${kafka.consumer.groupId}")
  public void kafkaListener(User message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            Acknowledgment acknowledgment) {
    log.info("Receive message [" + message + "] from topic : [" + topic + "]");

    try {
      throw new RuntimeException("Exception");
    } catch (Exception e) {
      kafkaProducer.retry(message);
    } finally {
      acknowledgment.acknowledge();
    }
  }

  @KafkaListener(topics = "${kafka.retryTopic}", containerFactory = "kafkaRetryListenerContainerFactory", groupId = "${kafka.consumer.groupId}")
  public void kafkaRetryListener(User message,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.info("Receive message [" + message + "] from topic : [" + topic + "]");
    throw new RuntimeException("Exception on Retry");
  }

  @KafkaListener(topics = "${kafka.dltTopic}", groupId = "${kafka.consumer.groupId}")
  public void dlt(User message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.info("Receive message [" + message + "] from topic : [" + topic + "]");
  }
}
