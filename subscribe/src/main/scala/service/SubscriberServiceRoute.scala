package service

import cats.effect.Async
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import cats.implicits._
import io.circe.Encoder
import service.models.ApiModels.{InsertRepository, SubscriptionResponse}
import io.circe.generic.auto._
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import service.BadRepositoryException.{InvalidRepository, MalformedRepository}
import service.models.ApiModels.InsertRepository.rD2

sealed trait BadRepositoryException extends Exception

object BadRepositoryException {
  final case class InvalidRepository(repo: InsertRepository) extends BadRepositoryException

  final case class MalformedRepository(repo: InsertRepository) extends BadRepositoryException
}

final case class SubscriberServiceRoute[F[_]](
    service: SubscribeService[F],
    githubApiValidation: GithubApi[F],
    logger: Logger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case _ @GET -> path =>
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
    case req @ PUT -> userName => {
      (for {
        decodeData <- req.as[InsertRepository]
        _          <- githubApiValidation.validateRepo(decodeData)
        res <- service.insertSubscription(userName.toString(), decodeData).flatMap {
          case 1 => Created()
          case _ => InternalServerError()
        }
      } yield res).flatMap(_ => Created()).handleErrorWith {
        case InvalidRepository(_)   => BadRequest()
        case MalformedRepository(_) => BadRequest()
        case _                      => InternalServerError()
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
