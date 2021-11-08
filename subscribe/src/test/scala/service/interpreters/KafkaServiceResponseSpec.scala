package service.interpreters

import cats.data.Kleisli
import cats.effect.{Async, IO}
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import fs2.kafka._
import fs2.kafka.vulcan._
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import service.domain.ConfigModels.KeyValueMessageHolder
import service.domain.kafka.EventServiceEvent.{KafkaMessage, Message, Operation}
import service.domain.kafka.EventServiceResponse

class KafkaServiceResponseSpec extends KafkaBaseSuite {

  val (topic, clientId, groupId) = ("event-topic", "client", "groupId")
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

  val m: Message[String, EventServiceResponse.EventServiceResponse] =
    Message("111", EventServiceResponse.EventServiceResponse("Message", "VTSL", "Repo"))

  test("Consumer worked using kafka and schema registry from containers") {

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
          getKafkaAddress
        )
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(groupId)

    val res: IO[List[Message[String, EventServiceResponse.EventServiceResponse]]] = createTopic
      .flatMap(_ =>
        for {
          p <- IO(
            Producer.make[IO, String, EventServiceResponse.EventServiceResponse](
              getKafkaAddress,
              avroSettings,
              clientId,
              topic
            )
          )
          _ <- p.use(_.sendMessage(KeyValueMessageHolder(m.key, m.value)))
          c <- IO(
            Consumer
              .make[IO](
                getKafkaAddress,
                clientId,
                groupId = "1",
                topic,
                avroSettings,
                processMessage[IO](logger)
              )
          )
        } yield c.use(_.consumeAndSendMessage(topic).take(1).compile.toList)
      )
      .flatten

    assertIO(res, List(m))
  }

  def processMessage[F[_]: Async](
      logger: Logger[F]
  ): Kleisli[F, Message[
    String,
    EventServiceResponse.EventServiceResponse
  ], Message[String, EventServiceResponse.EventServiceResponse]] =
    Kleisli(m => logger.info(s"Receiving $m").as(Message(m.key, m.value)))

}
