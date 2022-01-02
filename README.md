# kafka-retry-dlt-example

Exemplo de uma aplicação simples utilizando Kafka e implementando tópicos de retry e dead letter. O objetivo é não perder as mensagens que não puderam ser processadas, encaminhando-as para o tópico de _retry_, não prejudicando o desempenho e a ordem das demais mensagens que chegarão no tópico principal (_main_). Depois de um certo número de retentativas para processar essas mensagens, elas serão encaminhadas para um tópico de "mensagens mortas" (_dlt_), onde poderão ser analisadas manualmente por uma pessoa.

## Instruções

1. Abra um **primeiro terminal** e inicie os containers _Zookeeper_ e _Kafka_, executando o comando `docker-compose up -d` de dentro da pasta _docker_ deste projeto.
2. Ainda no **primeiro terminal**, Utilizando o comando`docker exec broker kafka-topics --bootstrap-server broker:9092 --create --topic <nome_topico>` crie os tópicos _main_ (tópico principal), _retry_ (tópico para retentativas) e _dlt_ (tópico de mensagens mortas)
3. A partir de um **segundo terminal**, inicie a aplicação com o comando `mvn spring-boot:run`
4. No **primeiro terminal**, execute o comando `curl --location --request POST 'localhost:8080/user/create' \
   --header 'Content-Type: application/json' \
   --data-raw '{ "firstName": "José", "lastName": "da Silva" }'` para colocar uma mensagem no tópico do Kafka e veja o resultado através do _log_ no **segundo terminal**.

## Log da execução

```
Sent message=[User{id=0, firstName='José', lastName='da Silva'}] with offset=[1] to topic=[main]
Receive message [User{id=0, firstName='José', lastName='da Silva'}] from topic : [main]
Sent message=[User{id=0, firstName='José', lastName='da Silva'}] with offset=[2] to topic=[retry]
Receive message [User{id=0, firstName='José', lastName='da Silva'}] from topic : [retry]
Receive message [User{id=0, firstName='José', lastName='da Silva'}] from topic : [retry]
Receive message [User{id=0, firstName='José', lastName='da Silva'}] from topic : [retry]
Sent message=[User{id=0, firstName='José', lastName='da Silva'}] with offset=[2] to topic=[dlt]
Receive message [User{id=0, firstName='José', lastName='da Silva'}] from topic : [dlt]
```

## Código comentado

### KafkaConsumerConfig.java

Para o tópico _main_ particularmente prefiro aceitar as mensagens manualmente, através do _Acknowledgment_. Por isso, vamos deixar `ENABLE_AUTO_COMMIT_CONFIG` como `false` e `AckMode` como `MANUAL_IMMEDIATE`.

```
@Bean
public ConsumerFactory<String, User> consumerFactory() {
 Map<String, Object> props = consumerProps();
 props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
 return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(User.class));
}
```

```
@Bean
public ConcurrentKafkaListenerContainerFactory<String, User> kafkaListenerContainerFactory() {
 ConcurrentKafkaListenerContainerFactory<String, User> factory = new ConcurrentKafkaListenerContainerFactory<>();
 factory.setConsumerFactory(consumerFactory());
 factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
 return factory;
}
```

As propriedades `retryDelay` e `maxAttempts` indicam respectivamente o tempo de atraso que a mensagem será processada pelo tópico de _retry_ e a quantidade de retentativas que serão executadas, até ela seguir para o tópico _dlt_.

```
@Bean
public RetryTemplate retryTemplate() {
 return RetryTemplate.builder()
     .fixedBackoff(this.retryDelay)
     .maxAttempts(this.maxAttempts)
     .build();
}
```

O uso do método `setRecoveryCallback` especifica a ação a ser executada quando o número máximo de retentativas for alcançado para o tópico de _retry_. Nesse caso, encaminhamos a mensagem para o tópico _dlt_.

```
@Bean
public ConcurrentKafkaListenerContainerFactory<String, User> kafkaRetryListenerContainerFactory() {
 ConcurrentKafkaListenerContainerFactory<String, User> factory = new ConcurrentKafkaListenerContainerFactory<>();
 factory.setConsumerFactory(retryConsumerFactory());
 factory.setRetryTemplate(retryTemplate());
 factory.setRecoveryCallback(retryContext -> {
   ConsumerRecord<String, User> record = (ConsumerRecord<String, User>) retryContext.getAttribute(
       CONTEXT_RECORD);
   kafkaProducer.dlt(record.value());
   return Optional.empty();
 });
 return factory;
}
```