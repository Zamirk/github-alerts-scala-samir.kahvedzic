import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import service.{GithubApi, HTTPServer, SubscribeService}
import service.models.ConfigModels.Config
import pureconfig.generic.auto._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      logger <- Resource.liftK(Slf4jLogger.create[IO])
      _      <- Resource.liftK(logger.info(s"Application start."))
      config <- Resource.liftK(ConfigSource.default.loadF[IO, Config]())
      server <- HTTPServer.createBlazeServer[IO](
        config,
        SubscribeService.make,
        logger,
        GithubApi.make
      )
    } yield server).use(_ => IO.never).as(ExitCode.Success)
  }
}
