import cats.data.Kleisli
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._
import service.algebras.kafka.{ConsumerAlg, ProducerAlg}
import service.database.connection.DBConnection
import service.interpreters.{Consumer, GithubApi, Producer, SubscribeService}
import service.route.HTTPServer
import service.domain.ConfigModels.{Config, KafkaConfig, KeyValueMessageHolder}

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import service.domain.kafka.EventServiceEvent
import service.domain.ConfigModels
import service.domain.kafka.EventServiceEvent.Message
import service.domain.kafka.EventServiceResponse
import vulcan.Codec

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      logger         <- Resource.liftK(Slf4jLogger.create[IO])
      _              <- Resource.liftK(logger.info(s"Application start."))
      config         <- Resource.liftK(ConfigSource.default.loadF[IO, Config]())
      _              <- Resource.liftK(logger.info(s"Loaded config: " + config))
      transactor     <- DBConnection.dbConnection[IO](config, logger)
      _              <- Resource.liftK(logger.info(s"Creating DB Tables."))
      _              <- DBConnection.createDatabaseTables(transactor)
      githubApi      <- BlazeClientBuilder[IO].resource.map(GithubApi.make[IO])
      schemaRegistry <- Resource.liftK(createSchemaRegistry[IO](config.kafka))
      producer       <- createProducer[IO](config.kafka, schemaRegistry)
      _ <-
        Consumer.make[IO](
          config.kafka.server,
          config.kafka.clientId,
          groupId = "1",
          config.kafka.eventTopic,
          schemaRegistry,
          processMessage[IO](logger)
        )
      server <- HTTPServer.createBlazeServer[IO](
        config.server,
        SubscribeService.make[IO](transactor),
        logger,
        producer,
        githubApi
      )
    } yield server).use(_ => IO.never).as(ExitCode.Success)

  def processMessage[F[_]: Async](
      logger: Logger[F]
  ): Kleisli[F, Message[
    String,
    EventServiceResponse.EventServiceResponse
  ], Message[String, EventServiceResponse.EventServiceResponse]] =
    Kleisli(m => logger.info(s"Receiving $m").as(Message(m.key, m.value)))

  def createProducer[F[_]: Async](kc: KafkaConfig, avroSettings: AvroSettings[F])(implicit
      c: Codec[EventServiceEvent.KafkaMessage]
  ): Resource[F, ProducerAlg[
    F,
    ConfigModels.KeyValueMessageHolder[String, EventServiceEvent.KafkaMessage]
  ]] = {
    Producer.make[F, String, EventServiceEvent.KafkaMessage](
      kc.server,
      avroSettings,
      kc.clientId,
      kc.subscriptionTopic
    )
  }

  def createSchemaRegistry[F[_]: Async](
      c: KafkaConfig
  )(implicit s: Sync[F]): F[AvroSettings[F]] =
    SchemaRegistryClientSettings[F](c.schemaRegistry.uri)
      .withMaxCacheSize(c.schemaRegistry.cachedSchemasPerSubject)
      .createSchemaRegistryClient
      .map(AvroSettings(_)(s))
}
