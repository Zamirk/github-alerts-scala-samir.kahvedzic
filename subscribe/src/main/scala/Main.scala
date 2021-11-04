import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings}
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._
import service.algebras.kafka.ProducerAlg
import service.database.connection.DBConnection
import service.interpreters.{GithubApi, Producer, SubscribeService}
import service.route.HTTPServer
import service.domain.ConfigModels.{Config, KafkaConfig, KeyValueMessageHolder}

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect._
import cats.syntax.all._
import service.domain.{ConfigModels, Kafka}
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
      githubApi      <- BlazeClientBuilder[IO](global).resource.map(GithubApi.make[IO])
      schemaRegistry <- Resource.liftK(createSchemaRegistry[IO](config.kafka))
      producer       <- createProducer[IO](config.kafka, schemaRegistry)(implicitly, Kafka.msgCodec)
      server <- HTTPServer.createBlazeServer[IO](
        config.server,
        SubscribeService.make[IO](transactor),
        logger,
        producer,
        githubApi
      )
    } yield server).use(_ => IO.never).as(ExitCode.Success)

  def createProducer[F[_]: Async](kc: KafkaConfig, avroSettings: AvroSettings[F])(implicit
      c: Codec[Kafka.KafkaMessage]
  ): Resource[F, ProducerAlg[F, ConfigModels.KeyValueMessageHolder[String, Kafka.KafkaMessage]]] = {
    Producer.make[F, String, Kafka.KafkaMessage](
      kc.server,
      avroSettings,
      kc.clientId,
      kc.topic
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
