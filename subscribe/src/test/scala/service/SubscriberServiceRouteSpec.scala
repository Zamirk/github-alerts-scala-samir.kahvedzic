package service

import cats.MonadError
import cats.effect.{Async, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Encoder
import org.http4s.{Method, Request, Response, Uri}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.compatible.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.typelevel.log4cats.noop.NoOpLogger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import service.interpreters.{GithubApiAlg, SubscribeServiceAlg}
import service.route.HTTPServer
import service.helpers.DefaultAssertions._
import io.circe.parser._
import service.helpers.SampleData
import service.models.ApiModels.{InsertRepository, SubscriptionsResponse}
import service.models.SlackModels.{
  ResponseField,
  SlackMessageBlock1,
  SlackMessageBlock2,
  SlackResponseBody
}

import scala.concurrent.ExecutionContext

class SubscriberServiceRouteSpec extends AsyncFreeSpec with AsyncIOSpec {
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  val malformedJson = Some("{Malformed Json")

  private def decodeString: Response[IO] => String = { (r: Response[IO]) =>
    r.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString
  }

  private def assertRequest[F[_]: Async](
      request: Request[F],
      subscribeService: Option[SubscribeServiceAlg[F]] = None,
      githubApi: Option[GithubApiAlg[F]] = None,
      assertion: Response[F] => Assertion
//      assertions: List[Response[F] => Assertion] = Nil
  ): F[Assertion] = {
    HTTPServer
      .generateRouter[F](
        subscribeService.getOrElse(SubscribeServiceAlg.stub[F]),
        githubApi.getOrElse(GithubApiAlg.stub[F]),
        NoOpLogger[F]
      )
      .orNotFound(
        request
      )
      .asserting(assertion) //todo fix
  }

  def buildRequest[F[_]](
      path: String,
      method: Method = GET,
      body: Option[String] = None
  ): Request[F] = {
    body match {
      case Some(body) =>
        Request[F](method, Uri.fromString(path).getOrElse(uri"/")).withEntity(body)
      case None => Request[F](method, Uri.fromString(path).getOrElse(uri"/"))
    }
  }

  private def brokensubscriptionService[F[_]: MonadError[*[_], Throwable]]
      : Option[SubscribeServiceAlg[F]] =
    Some(SubscribeServiceAlg.stub2[F])

  private def brokenGithubService[F[_]: MonadError[*[_], Throwable]]: Option[GithubApiAlg[F]] =
    Some(GithubApiAlg.stub2[F])

  "Calling /subscription/{xxx}" - {

    "with a GET request " - {

      "should return an OK status code" in {

        assertRequest[IO](
          request = buildRequest("/subscription/1234"),
          assertion = isOk
        )
      }

      "should return an InternalServerError" in {

        assertRequest[IO](
          request = buildRequest("/subscription/1234"),
          subscribeService = brokensubscriptionService,
          assertion = isInternalServerError
        )
      }

      "should return an NotFound status code" in {

        assertRequest[IO](
          request = buildRequest("/subscription/7979"),
          assertion = isNotFound
        )
      }

      "should return a valid Json String" in {

        assertRequest[IO](
          request = buildRequest("/subscription/1234"),
          assertion = (r: Response[IO]) =>
            parse(decodeString(r)) match {
              case Left(_)  => assert(false)
              case Right(_) => assert(true)
            }
        )
      }

      "should return the correct decoded result" in {

        assertRequest[IO](
          request = buildRequest("/subscription/1234"),
          assertion = (r: Response[IO]) =>
            decode[SubscriptionsResponse](decodeString(r)) match {
              case Left(_)     => assert(false)
              case Right(data) => assert(data === SampleData.sampleData)
            }
        )
      }

    }

    "with a PUT request " - {

      "should return a CREATED status code when valid data is sent" in {

        assertRequest[IO](
          request = buildRequest(
            path = "/subscription/8000",
            method = PUT,
            body = Some(Encoder[InsertRepository].apply(SampleData.sampleDataOne).toString())
          ),
          assertion = isCreated
        )
      }

      "should return an InternalServerError status code when there is a service level error" in {
        assertRequest[IO](
          request = buildRequest(
            path = "/subscription/8000",
            method = PUT,
            body = Some(Encoder[InsertRepository].apply(SampleData.sampleDataOne).toString())
          ),
          githubApi = brokenGithubService[IO],
          assertion = isInternalServerError
        )
      }

      "should return a BadRequest when " - {

        "malformed data is sent" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscription/8000",
              method = PUT,
              body = malformedJson
            ),
            githubApi = brokenGithubService[IO],
            assertion = isBadRequest
          )
        }

        "an organisation does not exist" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscription/1234",
              method = PUT,
              body =
                Some(Encoder[InsertRepository].apply(SampleData.nonExistantOrganisation).toString())
            ),
            assertion = isBadRequest
          )
        }

        "a repo does not exist on a valid organisation" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscription/1234",
              method = PUT,
              body = Some(Encoder[InsertRepository].apply(SampleData.dataToInsert2).toString())
            ),
            assertion = isBadRequest
          )
        }
      }
    }

    "with a DELETE request " - {

      "should return a NoContent status code when valid data is sent" in {

        assertRequest[IO](
          request = buildRequest(
            path = "/subscription/1234",
            method = DELETE,
            body = Some(Encoder[InsertRepository].apply(SampleData.sampleDataOne).toString())
          ),
          assertion = isNoContent
        )
      }

      "should return an InternalServerError status code when there is a service level error" in {
        assertRequest[IO](
          request = buildRequest(
            path = "/subscription/1234",
            method = PUT,
            body = Some(Encoder[InsertRepository].apply(SampleData.sampleDataOne).toString())
          ),
          githubApi = brokenGithubService[IO],
          assertion = isInternalServerError
        )
      }

      "should return a BadRequest when malformed data is sent" in {

        assertRequest[IO](
          request = buildRequest(
            path = "/subscription/8000",
            method = DELETE,
            body = malformedJson
          ),
          githubApi = brokenGithubService[IO],
          assertion = isBadRequest
        )

      }

      "should return a Not Found if " - {

        "the provided user id is not found" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscription/121212",
              method = DELETE,
              body = Some(Encoder[InsertRepository].apply(SampleData.sampleDataOne).toString())
            ),
            assertion = isNotFound
          )
        }

        "the repository is not found" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscription/1234",
              method = DELETE,
              body =
                Some(Encoder[InsertRepository].apply(SampleData.nonExistantOrganisation).toString())
            ),
            assertion = isNotFound
          )
        }
      }
    }

  }

  "Calling /subscription/slack/command with a POST request " - {

    "should return a NoContent status code" in {

      assertRequest[IO](
        request = Request[IO](
          method = Method.POST,
          uri = Uri.fromString("/subscription/slack/command").getOrElse(uri"/")
        ).withEntity(SampleData.validUrlEncodedData),
        assertion = isAccepted
      )
    }

    "should return an InternalServerError status code when there is a service level error" in {
      assertRequest[IO](
        request = Request[IO](
          method = Method.POST,
          uri = Uri.fromString("/subscription/slack/command").getOrElse(uri"/")
        ).withEntity(SampleData.validUrlEncodedData),
        subscribeService = brokensubscriptionService[IO],
        assertion = isInternalServerError
      )
    }

    "should return an BadRequest status code when the data sent is invalid" in {
      assertRequest[IO](
        request = Request[IO](
          method = Method.POST,
          uri = Uri.fromString("/subscription/slack/command").getOrElse(uri"/")
        ).withEntity(SampleData.invalidUrlEncodedData),
        assertion = isBadRequest
      )
    }

    "should return a valid json response" in {
      assertRequest[IO](
        request = Request[IO](
          method = Method.POST,
          uri = Uri.fromString("/subscription/slack/command").getOrElse(uri"/")
        ).withEntity(SampleData.validUrlEncodedData),
        assertion = (r: Response[IO]) =>
          decode[SlackResponseBody](decodeString(r)) match {
            case Left(_) => assert(false)
            case Right(data) =>
              assert(
                data === SlackResponseBody(
                  ResponseField.In_channel,
                  "It's 80 degrees right now.",
                  List("*It's 80 degrees right now.*", "Partly cloudy today and tomorrow").map(m =>
                    SlackMessageBlock1(
                      "section",
                      SlackMessageBlock2(
                        "mrkdwn",
                        m
                      )
                    )
                  )
                )
              )
          }
      )
    }

  }

}
