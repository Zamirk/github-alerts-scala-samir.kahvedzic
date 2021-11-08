package service.route

import cats.effect.Resource
import cats.effect.kernel.Async
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{Router, Server}
import org.http4s.HttpRoutes
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import service.algebras.kafka.ProducerAlg
import service.algebras.services.{GithubApiAlg, SubscribeServiceAlg}
import service.domain.ConfigModels.{KeyValueMessageHolder, ServerConfig}
import service.domain.kafka.EventServiceEvent
import service.route.endpoints.{SlackRoute, SubscriberServiceRoute}

import scala.concurrent.ExecutionContext.global

object HTTPServer {

  type MProducer[F[_]] =
    ProducerAlg[F, KeyValueMessageHolder[String, EventServiceEvent.KafkaMessage]]

  def createBlazeServer[F[_]: Async](
      config: ServerConfig,
      service: SubscribeServiceAlg[F],
      logger: SelfAwareStructuredLogger[F],
      producer: ProducerAlg[F, KeyValueMessageHolder[String, EventServiceEvent.KafkaMessage]],
      githubApi: GithubApiAlg[F]
  ): Resource[F, Server] = {
    BlazeServerBuilder[F]
      .bindHttp(config.port, config.host)
      .withHttpApp(
        generateRouter(service, githubApi, producer, logger).orNotFound
      )
      .resource
  }

  def generateRouter[F[_]: Async](
      service: SubscribeServiceAlg[F],
      githubApi: GithubApiAlg[F],
      producer: MProducer[F],
      logger: Logger[F]
  ): HttpRoutes[F] =
    Router(
      "/health"                     -> HealthCheck[F](logger).routes,
      "/subscription"               -> SubscriberServiceRoute[F](service, githubApi, producer, logger).routes,
      "/subscription/slack/command" -> SlackRoute[F](service, githubApi, logger).routes
    )

  case class HealthCheck[F[_]](
      logger: Logger[F]
  )(implicit
      F: Async[F]
  ) extends Http4sDsl[F] {
    def routes: HttpRoutes[F] = HttpRoutes.of[F] { case _ @GET -> path => Ok("Healthy: " + path) }
  }
}
