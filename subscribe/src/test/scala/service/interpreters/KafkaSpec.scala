package service.interpreters

import cats.effect.IO
import fs2.kafka.vulcan._
import org.apache.kafka.clients.admin.NewTopic
import cats.implicits.catsSyntaxApplicativeError
import fs2.Stream
import fs2.kafka.{
  AdminClientSettings,
  AutoOffsetReset,
  ConsumerSettings,
  KafkaAdminClient,
  KafkaConsumer,
  RecordDeserializer
}
import org.apache.kafka.common.errors.TopicExistsException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import service.domain.ConfigModels.KeyValueMessageHolder
import service.domain.Kafka.{KafkaMessage, Operation}
import service.domain.Kafka.Operation.{DeleteSubscription, NewSubscription}

import scala.concurrent.duration.DurationInt

class KafkaSpec extends KafkaBaseSuite {

  val (topic, clientId, groupId) = ("subscription-topic", "client", "groupId")
  val (org, repo, slackId)       = ("47Deg", "Repo", "123")

  val auth = Auth.Basic("user", "password")

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def createTopic: IO[Unit] = {
    val adminClientSettings: AdminClientSettings = AdminClientSettings(getKafkaAddress)

    KafkaAdminClient.resource[IO](adminClientSettings).use {
      _.createTopic(new NewTopic(topic, 1, 1.toShort)).recoverWith { case _: TopicExistsException =>
        logger.info(s"Topic $topic already exists")
      }
    }
  }

  def generateMessage(o: Operation): KeyValueMessageHolder[String, KafkaMessage] =
    KeyValueMessageHolder(slackId, KafkaMessage(o, org, repo))

  val newSubscriptionMessage: KeyValueMessageHolder[String, KafkaMessage] = generateMessage(
    NewSubscription
  )

  val deleteSubscriptionMessage: KeyValueMessageHolder[String, KafkaMessage] = generateMessage(
    DeleteSubscription
  )

  test("Producer and Consumer worked using kafka and schema registry from containers") {

    val avroSettings: AvroSettings[IO] =
      AvroSettings {
        SchemaRegistryClientSettings[IO](getSchemaRegistryAddress)
          .withAuth(Auth.Basic("user", "password"))
      }

    implicit val d: RecordDeserializer[IO, KafkaMessage] =
      AvroDeserializer[KafkaMessage].using(avroSettings)

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
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(groupId)

    val res = for {
      _ <- createTopic
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

    assertIO(res, List(KafkaMessage(NewSubscription, org, repo)))
  }

}
