package service.route.endpoints

import cats.effect.Async
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import cats.implicits._
import service.models.ApiErrors.BadRepositoryException.InvalidRepository
import service.models.ApiModels.{InsertRepository, SlackUserId, SubscriptionsResponse}
import service.models.ApiErrors.ApiException.UserNameNotFound
import org.http4s.circe.CirceEntityDecoder._
import org.http4s._
import org.http4s.circe._

final case class SubscriberServiceRoute[F[_]](
    service: SubscribeServiceAlg[F],
    githubApiValidation: GithubApiAlg[F],
    logger: Logger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {

  implicit def subscriptionResponseEd: EntityDecoder[F, SubscriptionsResponse] = jsonOf
  implicit def subscriptionResponseEe: EntityEncoder[F, SubscriptionsResponse] = jsonEncoderOf

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / username =>
      (for {
        userData <- service.getSubcriptions(SlackUserId(username))
        response <- Ok(userData)
      } yield response).handleErrorWith {
        case UserNameNotFound(userName) => NotFound(s"Not found username $userName")
        case _                          => InternalServerError()
      }
    case req @ PUT -> Root / userName =>
      req
        .attemptAs[InsertRepository]
        .value
        .flatMap {
          case Left(_) => BadRequest()
          case Right(decodedData) =>
            for {
              _   <- githubApiValidation.validateRepo(decodedData)
              _   <- service.insertSubscription(SlackUserId(userName), decodedData)
              res <- Created()
            } yield res
        }
        .handleErrorWith {
          case InvalidRepository(_) => BadRequest()
          case _                    => InternalServerError()
        }
  }
}
