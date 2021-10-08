package service

import cats.data.Kleisli
import cats.effect.Resource
import cats.effect.kernel.Async
import org.http4s.{Request, Response}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import service.models.ConfigModels.Config

import scala.concurrent.ExecutionContext.global

object HTTPServer {

  def createBlazeServer[F[_]: Async](
      config: Config,
      service: SubscribeService[F],
      logger: SelfAwareStructuredLogger[F]
  ): Resource[F, Server] = {
    BlazeServerBuilder[F](global)
      .bindHttp(config.port, config.host)
      .withHttpApp(
        generateRouter(service, logger)
      )
      .resource
  }

  def generateRouter[F[_]: Async](
      service: SubscribeService[F],
      logger: SelfAwareStructuredLogger[F]
  ): Kleisli[F, Request[F], Response[F]] = {
    Router(
      "/health"     -> HealthCheck[F](logger).routes,
      "/subscriber" -> new SubscriberServiceRoute[F](service, logger).routes
    ).orNotFound
  }
}
