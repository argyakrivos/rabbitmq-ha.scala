package com.blinkbox.books.rabbitmq

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Props}
import com.blinkbox.books.messaging.{Event, EventHeader}
import com.blinkbox.books.rabbitmq.RabbitMqConsumer.QueueConfiguration
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * A simple ad-hoc test/example using RabbitMQ code for confirmed publishing,
 * that runs against a local RabbitMQ.
 * It uses message headers as routing arguments
 *
 * You can for example try to kill and restart RabbitMQ while it's running to see how that's handled.
 */
object ConfirmedPublishingWithHeadersExample extends App {

  println("Starting")

  def newConnection() = RabbitMq.reliableConnection(RabbitMqConfig(new URI("amqp://guest:guest@localhost:5672"), 2.seconds, 10.seconds))

  // Set up an actor that publishes messages every few seconds.
  {
    import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher._

    val connection = newConnection()
    val system = ActorSystem("producer-system")
    val counter = new AtomicInteger()
    implicit val executionContext = system.dispatcher
    val config = ConfigFactory.load("rabbitmq-publisher-test-headers.conf").getConfig("service.test.testQueue")
    val queueConfig = QueueConfiguration(config)

    val publisher = system.actorOf(Props(
      new RabbitMqConfirmedPublisher(connection, PublisherConfiguration(config))), name ="publisher")
    val responsePrinter = system.actorOf(Props(new ResponsePrinter()), name = "response-printer")

    // Send a steady stream of numbers every few seconds.
    system.scheduler.schedule(0.seconds, 3.seconds) {
      val newValue = counter.getAndIncrement
      val event = Event.xml(s"<value>$newValue</value>", EventHeader("test-producer-app-1"))
      publisher.tell(event, responsePrinter)
      println(s"Sent request with value $newValue")
    }
  }

  // Set up an actor that consumes messages and somewhat arbitrarily makes them succeed or fail.
  {
    val connection = newConnection()
    val system = ActorSystem("consumer-system")

    val output = system.actorOf(Props(new TestConsumer()), "test-consumer")
    val config = ConfigFactory.load("rabbitmq-publisher-test-headers.conf").getConfig("service.test.testQueue")
    val queueConfig = QueueConfiguration(config)
    val consumer = system.actorOf(Props(new RabbitMqConsumer (connection.createChannel(), queueConfig, "consumer-tag", output)), name = "rabbitmq-consumer")
    consumer ! RabbitMqConsumer.Init
  }

  println("Started!")

}

