package service.interpreters

import net.manub.embeddedkafka.schemaregistry.EmbeddedKafka
import cats.effect.IO
import net.manub.embeddedkafka.schemaregistry._
import fs2.kafka.vulcan.{Auth, AvroSettings, SchemaRegistryClientSettings, avroDeserializer}
import fs2.kafka._
import cats.effect.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.Stream
import io.github.embeddedkafka.schemaregistry.EmbeddedKafkaConfigImpl
import org.http4s.Uri
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import service.domain.ConfigModels.KeyValueMessageHolder
import service.domain.Kafka.KafkaMessage
import service.domain.Kafka.Operation.{DELETE_SUBSCRIPTION, NEW_SUBSCRIPTION}

class ProducerSpec extends AsyncFreeSpec with Matchers with AsyncIOSpec with EmbeddedKafka {

  val (topic, clientId) = ("subscription-topic", "client")
  val (kp, zkp, srp)    = (12345, 12346, 12347)

  private def createConsumer(
      s: AvroSettings[IO],
      a: EmbeddedKafkaConfig
  ): Stream[IO, KafkaConsumer[IO, String, KafkaMessage]] = {
    KafkaConsumer
      .stream(
        genConsumerSettings(s).withBootstrapServers(
          "localhost:" + a.kafkaPort
        )
      )
  }

  val message: KeyValueMessageHolder[String, KafkaMessage] = KeyValueMessageHolder(
    "123",
    KafkaMessage(
      NEW_SUBSCRIPTION,
      "47Deg",
      "Repo"
    )
  )

  val message2: KeyValueMessageHolder[String, KafkaMessage] = KeyValueMessageHolder(
    "123",
    KafkaMessage(
      DELETE_SUBSCRIPTION,
      "47Deg",
      "Repo"
    )
  )

  def genConsumerSettings(s: AvroSettings[IO]): ConsumerSettings[IO, String, KafkaMessage] =
    ConsumerSettings[IO, String, KafkaMessage](
      avroDeserializer[String].using(s),
      avroDeserializer[KafkaMessage].using(s)
    )
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withGroupId("groupId")
      .withClientId(clientId)

  def genAvroSettings(port: Int): AvroSettings[IO] =
    AvroSettings {
      SchemaRegistryClientSettings[IO](
        s"http://localhost:$port"
      ).withAuth(Auth.Basic("user", "password"))
    }

  "Consumer successfully receives messages sent my Producer" in {

    implicit val config: EmbeddedKafkaConfigImpl =
      EmbeddedKafkaConfigImpl(
        kafkaPort = kp,
        zooKeeperPort = zkp,
        schemaRegistryPort = srp,
        Map(),
        Map(),
        Map(),
        Map()
      )
    val embeddedKafka: Resource[IO, io.github.embeddedkafka.schemaregistry.EmbeddedKWithSR] =
      Resource.make(IO(io.github.embeddedkafka.schemaregistry.EmbeddedKafka.start()))(k =>
        IO(k.stop(true))
      )

    for {
      kafka <- embeddedKafka
      a = kafka.config
      s = genAvroSettings(a.schemaRegistryPort)
      producer <-
        Producer
          .make[IO, String, KafkaMessage](
            Uri.unsafeFromString("localhost:" + a.kafkaPort).renderString,
            genAvroSettings(a.schemaRegistryPort),
            clientId,
            topic
          )
      listOfMessages =
        for {
          _ <- IO(createCustomTopic(topic))
          _ <- producer.sendMessage(message)
          _ <- producer.sendMessage(message2)

          message <- createConsumer(s, a)
            .evalTap(_.subscribeTo(topic))
            .evalTap(_.seekToBeginning)
            .flatMap(_.stream)
            .map(_.record.value)
            .take(1)
            .compile
            .toList
        } yield message
    } yield listOfMessages
  }.use(_.asserting(_ shouldBe List(message.value)))

}
