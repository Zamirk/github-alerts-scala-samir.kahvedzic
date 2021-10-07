package service

import cats.effect.Async
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.SelfAwareStructuredLogger
import cats.implicits._
import io.circe.Encoder
import service.models.ResponseModels.SubscriptionResponse
import io.circe.generic.auto._

case class SubscriberServiceRoute[F[_]](
    service: SubscribeService[F],
    logger: SelfAwareStructuredLogger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] { case _ @GET -> path =>
    (for {
      userData <- service.getSubscription(path.toString())
      response <-
        userData match {
          case None => NotFound("Repositories empty.")
          case Some(value) =>
            logger.debug("Found data: " + value.toString)
            Ok(Encoder[SubscriptionResponse].apply(value).toString())
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
