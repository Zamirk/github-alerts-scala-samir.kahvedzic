package service.route.endpoints

import cats.effect.Async
import org.http4s.{HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import cats.implicits._
import io.circe.Encoder
import service.models.ApiErrors.BadRepositoryException.InvalidRepository
import service.models.ApiModels.{InsertRepository, SubscriptionResponse}
import service.models.ApiErrors.ApiException.UserNameNotFound
import org.http4s.circe.CirceEntityDecoder._

final case class SubscriberServiceRoute[F[_]](
    service: SubscribeServiceAlg[F],
    githubApiValidation: GithubApiAlg[F],
    logger: Logger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case _ @GET -> path =>
      (for {
        userData <- service.getSubscription(path.toRelative.toString())
        response <- {
          logger.debug("Found data: " + userData.toString)
          Ok(Encoder[SubscriptionResponse].apply(userData).toString())
        }
      } yield response).handleErrorWith {
        case UserNameNotFound(userName) => NotFound(s"Not found username $userName")
        case _                          => InternalServerError()
      }
    case req @ PUT -> userName =>
      req
        .attemptAs[InsertRepository]
        .value
        .flatMap {
          case Left(_) => BadRequest()
          case Right(decodedData) =>
            for {
              _   <- githubApiValidation.validateRepo(decodedData)
              _   <- service.insertSubscription(userName.toString(), decodedData)
              res <- Created()
            } yield res
        }
        .handleErrorWith {
          case InvalidRepository(_) => BadRequest()
          case _                    => InternalServerError()
        }
  }
}
