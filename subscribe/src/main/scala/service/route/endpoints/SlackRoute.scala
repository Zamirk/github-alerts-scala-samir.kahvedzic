package service.route.endpoints

import cats.effect.Async
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.typelevel.log4cats.Logger
import service.algebras.services.{GithubApiAlg, SubscribeServiceAlg}
import service.domain.ApiModels.SlackCommand._
import service.domain.ApiModels.{RepoCarrier, SlackCommand, SlackUserId}

import java.util.UUID

final case class SlackRoute[F[_]](
    service: SubscribeServiceAlg[F],
    githubApiValidation: GithubApiAlg[F],
    logger: Logger[F]
)(implicit F: Async[F])
    extends DefaultRoute[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root =>
    req
      .decode[UrlForm] { d =>
        extractUrlformData(d) match {
          case Some(List(c, t, r, tr, u)) =>
            for {
              uuid <- validateUUID(u)
              response <- service.postSlackCommand(
                SlackUserId(uuid),
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

  private def extractUrlformData(d: UrlForm): Option[List[String]] =
    formData.traverse(d.values.get(_).flatMap(_.headOption))

}
