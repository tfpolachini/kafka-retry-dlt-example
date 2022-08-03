package com.example.kafkaretrydlt.config;

import com.example.kafkaretrydlt.entities.User;
import com.example.kafkaretrydlt.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_ACKNOWLEDGMENT;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_RECORD;

@Configuration
public class KafkaConsumerConfig {

  @Value("${kafka.bootstrapAddress}")
  private String bootstrapAddress;

  @Value("${kafka.consumer.groupId}")
  private String groupId;

  @Value("${kafka.retry.maxAttempts}")
  private Integer maxAttempts;

  @Value("${kafka.retry.delay}")
  private Long retryDelay;

  private final KafkaProducer kafkaProducer;

  public KafkaConsumerConfig(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  public Map<String, Object> consumerProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, User.class);
    return props;
  }

  public ConsumerFactory<String, User> consumerFactory() {
    Map<String, Object> props = consumerProps();
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(User.class));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, User> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, User> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, User> kafkaRetryListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, User> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
    factory.setRetryTemplate(retryTemplate());
    factory.setRecoveryCallback(retryContext -> {
      var record = (ConsumerRecord<String, User>) retryContext.getAttribute(
          CONTEXT_RECORD);
      var acknowledgment = (Acknowledgment) retryContext.getAttribute(
              CONTEXT_ACKNOWLEDGMENT);

      kafkaProducer.dlt(record.value());
      acknowledgment.acknowledge();

      return Optional.empty();
    });
    return factory;
  }

  public RetryTemplate retryTemplate() {
    return RetryTemplate.builder()
        .fixedBackoff(this.retryDelay)
        .maxAttempts(this.maxAttempts)
        .build();
  }
}
