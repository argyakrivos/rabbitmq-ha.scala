# Rabbit-MQ HA Library

High availability RabbitMQ client for Scala

### Configuration

Connection
```scala
val factory = new ConnectionFactory()
  factory.setHost(host)
  factory.setPort(port)
  factory.setUsername(username)
  factory.setPassword(password)
```
Lyra Config

 ```scala
 val lyraConfig = new Config()
    .withConnectRetryPolicy(RetryPolicies.retryNever()) // do not retry on an initial error
    .withRecoveryPolicy(new RecoveryPolicy()
    .withBackoff(lyra.util.Duration.seconds(retryInterval), lyra.util.Duration.seconds(retryMaxInterval)))
    .withRetryPolicy(new RetryPolicy()
    .withBackoff(lyra.util.Duration.seconds(retryInterval), lyra.util.Duration.seconds(retryMaxInterval)))
```

Connection

```scala
val connection = Connections.create(factory, lyraConfig)
```

### Consumer
Use AmqpConsumerActor to retrieve messages from RabbitMQ

```scala
AmqpConsumerActor(connection.createChannel, receivingActor, queueName, None, amqpTimeout, None, "consumer-tag", prefetchCount))
```

### Publisher
Use AmqpPublisherActor to send message to RabbitMQ

```scala
AmqpPublisherActor(connection.createChannel, queueName, amqpTimeout)
```

