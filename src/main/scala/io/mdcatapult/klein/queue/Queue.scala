package io.mdcatapult.klein.queue

import akka.actor._
import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit.properties.{DeliveryModePersistence, MessageProperty}
import com.spingo.op_rabbit.{Exchange => OpExchange, RecoveryStrategy => OpRecoveryStrategy, _}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Format

import scala.concurrent.Future

/**
  * Queue Abstraction
  */
case class Queue[T <: Envelope](name: String,
                                consumerName: Option[String] = None,
                                topics: Option[String] = None,
                                persistent: Boolean = true,
                                errorQueue: Option[String] = None,
                                exchange: Option[String] = None)
                               (implicit actorSystem: ActorSystem, config: Config, formatter: Format[T])
  extends Subscribable with Sendable[T] with LazyLogging {

  import actorSystem.dispatcher
  import Directives.{exchange => opExchange, _}
  val rabbit: ActorRef = actorSystem.actorOf(
    Props(classOf[Rabbit], ConnectionParams.fromConfig(config.getConfig("op-rabbit.connection"))),
    name
  )

  implicit val recoveryStrategy: OpRecoveryStrategy = RecoveryStrategy.errorQueue(errorQueue, consumerName = consumerName)

  lazy val exchangeName: String = exchange.getOrElse(config.getString("op-rabbit.topic-exchange-name"))
  /**
    * subscribe to queue/topic and execute callback on receipt of message
    *
    * @param callback Function
    * @return SubscriptionRef
    */
  def subscribe(callback: (T, String) => Any, concurrent: Int = 1): SubscriptionRef = Subscription.run(rabbit) {
    val queueExchange = OpExchange.direct(exchangeName, durable = true, autoDelete = false)
    val queueBinding = Binding.direct(queue(name, durable = true, autoDelete = false), OpExchange.passive(queueExchange))
    channel(qos = concurrent) {
      consume(queueBinding) {
        (body(as[T]) & opExchange) {
          (msg, ex) =>
            callback(msg, ex) match {
              // Success
              case f: Future[Any] => ack(f)
              // Possible failure. Really should not happen
              case _ => {
                logger.error(s"Message appears to have completed without returning value. Investigate logs of consumer handling queue $name for possible reason.")
                // Delete message from queue and flag as failed
                nack()
              }
            }
        }
      }
    }
  }

  /**
    * Send message directly to configured queue. If the queue is set the persist messages
    * then add header to persist them DeliveryModePersistence(true) otherwise add header
    * DeliveryModePersistence(false). Note that op-rabbit adds DeliveryModePersistence(true)
    * header by default but we are adding it here to ensure any future changes don't come as
    * a surprise.
    *
    * @param envelope message to send
    */
  def send(envelope: T, properties: Seq[MessageProperty] = Seq[MessageProperty]()): Unit = {
    val persistedProperties = if (persistent) {
      properties :+ DeliveryModePersistence(true)
    } else {
      properties :+ DeliveryModePersistence(false)
    }
    rabbit ! Message.exchange(envelope, exchangeName, name, persistedProperties)
  }

}
