package service.interpreters

import cats.effect.IO
import com.dimafeng.testcontainers.lifecycle.and
import fs2.kafka.vulcan._
import fs2.kafka._
import org.apache.kafka.clients.admin.NewTopic
import service.domain.ConfigModels.KeyValueMessageHolder
import service.domain.Kafka.{KafkaMessage, Operation}
import service.domain.Kafka.Operation.{DeleteSubscription, NewSubscription}

class ProducerSpec extends KafkaBaseSuite {
  val (topic, clientId, groupId) = ("subscription-topic", "client", "groupId")
  val (org, repo, slackId)       = ("47Deg", "Repo", "123")

  val auth = Auth.Basic("user", "password")

  def createTopic(): IO[Unit] = {
    val adminClientSettings: AdminClientSettings = AdminClientSettings(getKafkaAddress)

    KafkaAdminClient
      .resource[IO](adminClientSettings)
      .use(
        _.createTopic(new NewTopic(topic, 1, 1.toShort))
      )
  }

  def generateMessage(o: Operation): KeyValueMessageHolder[String, KafkaMessage] =
    KeyValueMessageHolder(slackId, KafkaMessage(o, org, repo))

  val newSubscriptionMessage: KeyValueMessageHolder[String, KafkaMessage] = generateMessage(
    NewSubscription
  )

  val deleteSubscriptionMessage: KeyValueMessageHolder[String, KafkaMessage] = generateMessage(
    DeleteSubscription
  )

  "Producer should send message to topic" in {

    def getSchemaRegistryAddress: String = withContainers { case _ and schemaRegistryContainer =>
      schemaRegistryContainer.schemaUrl
    }

    val avroSettings: AvroSettings[IO] =
      AvroSettings {
        SchemaRegistryClientSettings[IO](getSchemaRegistryAddress)
          .withAuth(auth)
      }

    def consumerSettings[K, V](implicit
        keyDeserializer: RecordDeserializer[IO, K],
        valueDeserializer: RecordDeserializer[IO, V]
    ): ConsumerSettings[IO, K, V] =
      ConsumerSettings[IO, K, V]
        .withBootstrapServers(
          withContainers(
            _.head.container.getBootstrapServers
          )
        )
        .withAutoOffsetReset(AutoOffsetReset.Latest)
        .withGroupId(groupId)

    implicit val d: RecordDeserializer[IO, KafkaMessage] =
      AvroDeserializer[KafkaMessage].using(avroSettings)

    for {
      _ <- createTopic()
      p <- IO(
        Producer.make[IO, String, KafkaMessage](getKafkaAddress, avroSettings, clientId, topic)
      )
      _ <- p.use(_.sendMessage(newSubscriptionMessage))
//      r <- p.use(_.sendMessage(deleteSubscriptionMessage))
      res <- {
        KafkaConsumer.apply
          .stream(consumerSettings[String, KafkaMessage])
          .evalTap(_.subscribeTo(topic))
          .flatMap(_.stream)
          .map(_.record.value)
          .compile
          .toList
      }

    } yield res

  }.asserting(_ shouldBe List(KafkaMessage(NewSubscription, org, repo)))

}
