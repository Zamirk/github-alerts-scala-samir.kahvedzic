import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._
import service.database.DatabaseConnection
import service.interpreters.{GithubApi, SubscribeService}
import service.route.HTTPServer
import service.domain.ConfigModels.Config
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      logger     <- Resource.liftK(Slf4jLogger.create[IO])
      _          <- Resource.liftK(logger.info(s"Application start."))
      config     <- Resource.liftK(ConfigSource.default.loadF[IO, Config]())
      transactor <- DatabaseConnection.dbConnection[IO](config, logger)
      _          <- Resource.liftK(logger.info(s"Creating DB Tables."))
      _          <- DatabaseConnection.createDatabaseTables(transactor)
      githubApi  <- BlazeClientBuilder[IO](global).resource.map(GithubApi.make[IO])
      server <- HTTPServer.createBlazeServer[IO](
        config.server,
        SubscribeService.make[IO](),
        logger,
        githubApi
      )
    } yield server).use(_ => IO.never).as(ExitCode.Success)
}
