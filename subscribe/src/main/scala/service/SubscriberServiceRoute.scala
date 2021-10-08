package service

import cats.effect.Async
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.SelfAwareStructuredLogger
import cats.implicits._
import io.circe.Encoder
import service.models.ResponseModels.SubscriptionResponse
import io.circe.generic.auto._

class SubscriberServiceRoute[F[_]](
    service: SubscribeService[F],
    logger: SelfAwareStructuredLogger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] { case _ @GET -> path =>
    (for {
      userData <- service.getSubscription(path.toString())
      response <- {
        logger.debug("Found data: " + userData.toString)
        Ok(Encoder[SubscriptionResponse].apply(userData).toString())
      }
    } yield response).handleErrorWith {
      case UserNameNotFound(userName) => NotFound(s"Not found username $userName")
      case _                          => InternalServerError()
    }
  }

}

case class HealthCheck[F[_]](
    logger: SelfAwareStructuredLogger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] { case _ @GET -> path =>
    Ok("Healthy: " + path)
  }
}
