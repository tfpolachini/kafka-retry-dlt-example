package com.example.kafkaretrydlt.producer;

import com.example.kafkaretrydlt.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class KafkaProducer {

  Logger log = LoggerFactory.getLogger(KafkaProducer.class);

  @Value("${kafka.consumer.topic}")
  private String mainTopic;

  @Value("${kafka.retryTopic}")
  private String retryTopic;

  @Value("${kafka.dltTopic}")
  private String dltTopic;

  private final KafkaTemplate<String, User> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, User> kafkaTemplate) { this.kafkaTemplate = kafkaTemplate; }

  public void produce(User user) { sendMessageToTopic(user, mainTopic); }

  public void retry(User user) {
    sendMessageToTopic(user, retryTopic);
  }

  public void dlt(User user) {
    sendMessageToTopic(user, dltTopic);
  }

  private void sendMessageToTopic(User user, String topic) {
    ListenableFuture<SendResult<String, User>> future = kafkaTemplate.send(topic,
        user);

    future.addCallback(new ListenableFutureCallback<SendResult<String, User>>() {
      @Override
      public void onFailure(Throwable ex) {
        log.error("Unable to send message=[" + user + "] due to : " + ex.getMessage());
      }

      @Override
      public void onSuccess(SendResult<String, User> result) {
        log.info(
            "Sent message=[" + user + "] with offset=[" + result.getRecordMetadata()
                .offset() + "] to topic=[" + topic + "]");
      }
    });
  }
}
