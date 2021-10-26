package service.route.endpoints

import cats.effect.Async
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import cats.implicits._
import service.domain.ApiModels._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._

final case class SubscriberServiceRoute[F[_]](
    service: SubscribeServiceAlg[F],
    githubApiValidation: GithubApiAlg[F],
    logger: Logger[F]
)(implicit F: Async[F])
    extends DefaultRoute[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / username          => getSubscriptions(username)
    case req @ PUT -> Root / username    => insertSubscription(req, username)
    case req @ DELETE -> Root / username => deleteSubscriptions(req, username)
  }

  private def insertSubscription(req: Request[F], userName: String): F[Response[F]] = {
    processInput(
      req,
      (d: RepoCarrier) => {
        for {
          uuid <- validateUUID(userName)
          _    <- githubApiValidation.validateRepo(d)
          _    <- service.insertSubscription(SlackUserId(uuid), userName, "/random", d.subscription)
          res  <- Created()
        } yield res
      }
    )
      .handleErrorWith {
        githubValidation.orElse(internalServerError)
      }
  }

  private def deleteSubscriptions(req: Request[F], userName: String): F[Response[F]] = {
    processInput(
      req,
      (d: RepoCarrier) =>
        for {
          uuid <- validateUUID(userName)
          _    <- service.deleteSubscription(SlackUserId(uuid), d)
          res  <- NoContent()
        } yield res
    ).handleErrorWith {
      userNameValidation.orElse(repoNotFound).orElse(internalServerError)
    }
  }

  private def getSubscriptions(username: String): F[Response[F]] = {
    (for {
      uuid     <- validateUUID(username)
      userData <- service.getSubscriptions(SlackUserId(uuid))
      response <- Ok(userData)
    } yield response).handleErrorWith {
      userNameValidation.orElse(internalServerError)
    }
  }

  private def processInput(
      req: Request[F],
      resultProcess: RepoCarrier => F[Response[F]]
  ): F[Response[F]] =
    req
      .attemptAs[RepoCarrier]
      .value
      .flatMap {
        case Left(_)            => BadRequest()
        case Right(decodedData) => resultProcess(decodedData)
      }

}
