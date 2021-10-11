import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import service.models.ConfigModels.Config
import pureconfig.generic.auto._
import service.interpreters.{GithubApiAlg, SubscribeServiceAlg}
import service.route.HTTPServer
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      logger    <- Resource.liftK(Slf4jLogger.create[IO])
      _         <- Resource.liftK(logger.info(s"Application start."))
      config    <- Resource.liftK(ConfigSource.default.loadF[IO, Config]())
      githubApi <- BlazeClientBuilder[IO](global).resource.map(GithubApiAlg.make[IO])
      server <- HTTPServer.createBlazeServer[IO](
        config,
        SubscribeServiceAlg.make,
        logger,
        githubApi
      )
    } yield server).use(_ => IO.never).as(ExitCode.Success)
  }
}
