import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import service.{FakeSubscriberService, HTTPServer}
import service.models.ConfigModels.Config
import pureconfig.generic.auto._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      logger <- Resource.liftK(Slf4jLogger.create[IO])
      _      <- Resource.liftK(logger.info(s"Application start.")) //Our first log
      config <- Resource.liftK(ConfigSource.default.loadF[IO, Config]())
      server <- HTTPServer.createBlazeServer[IO](config, FakeSubscriberService[IO](), logger)
    } yield {
      server
    }).use(_ => IO.never).as(ExitCode.Success)
  }

}
