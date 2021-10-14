package service.route.endpoints

import cats.effect.Async
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import cats.implicits._
import service.models.ApiErrors.BadRepositoryException.InvalidRepository
import service.models.ApiModels.{InsertRepository, SlackUserId, SubscriptionsResponse}
import service.models.ApiErrors.SubscriberServiceException.{RepositoryNotFound, UserNameNotFound}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s._
import org.http4s.circe._
import service.models.SlackModels
import service.models.SlackModels.SlackCommand.{Command, ResponseUrl, Text, TriggerId, UserId}
import service.models.SlackModels.{SlackCommand, SlackResponseBody}

final case class SubscriberServiceRoute[F[_]](
    service: SubscribeServiceAlg[F],
    githubApiValidation: GithubApiAlg[F],
    logger: Logger[F]
)(implicit
    F: Async[F]
) extends Http4sDsl[F] {

  implicit def subscriptionResponseEd: EntityDecoder[F, SubscriptionsResponse] = jsonOf
  implicit def subscriptionResponseEe: EntityEncoder[F, SubscriptionsResponse] = jsonEncoderOf

  implicit def slackResponseEd: EntityDecoder[F, SlackResponseBody] = jsonOf
  implicit def slackResponseEe: EntityEncoder[F, SlackResponseBody] = jsonEncoderOf

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / username =>
      (for {
        userData <- service.getSubscriptions(SlackUserId(username))
        response <- Ok(userData)
      } yield response).handleErrorWith {
        userNameNotFound.orElse(internalServerError)
      }
    case req @ PUT -> Root / userName =>
      processInput(
        req,
        (d: InsertRepository) => {
          for {
            _   <- githubApiValidation.validateRepo(d)
            _   <- service.insertSubscription(SlackUserId(userName), d)
            res <- Created()
          } yield res
        }
      )
        .handleErrorWith {
          githubValidation.orElse(internalServerError)
        }
    case req @ DELETE -> Root / userName =>
      processInput(
        req,
        (d: InsertRepository) =>
          for {
            _   <- service.deleteSubscription(SlackUserId(userName), d)
            res <- NoContent()
          } yield res
      ).handleErrorWith {
        userNameNotFound.orElse(repoNotFound).orElse(internalServerError)
      }

    case req @ POST -> Root / "slack" / "command" =>
      req
        .decode[UrlForm] { d =>
          SlackModels.formData.traverse(d.values.get(_).flatMap(_.headOption)) match {
            case Some(List(c, t, r, tr, u)) =>
              for {
                response <- service.postSlackCommand(
                  SlackUserId(u),
                  SlackCommand(Command(c), Text(t), ResponseUrl(r), TriggerId(tr), UserId(u))
                )
                res <- Accepted(response)
              } yield res
            case _ => BadRequest(s"Bad data: ${d.values}")
          }
        }
        .handleErrorWith {
          internalServerError
        }
  }

  private def processInput(
      req: Request[F],
      resultProcess: InsertRepository => F[Response[F]]
  ): F[Response[F]] =
    req
      .attemptAs[InsertRepository]
      .value
      .flatMap {
        case Left(_)            => BadRequest()
        case Right(decodedData) => resultProcess(decodedData)
      }

  type ThrowToResponseF = PartialFunction[Throwable, F[Response[F]]]

  val userNameNotFound: ThrowToResponseF = { case UserNameNotFound(u) =>
    NotFound(s"Not found username $u")
  }

  val repoNotFound: ThrowToResponseF = { case RepositoryNotFound(r) =>
    NotFound(s"Repository not found in subscription ${r.subscription.repository}")
  }

  val githubValidation: ThrowToResponseF = { case InvalidRepository(_) =>
    BadRequest()
  }

  val internalServerError: ThrowToResponseF = { case _ =>
    InternalServerError()
  }
}
