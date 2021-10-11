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
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.noop.NoOpLogger
import service.algebras.{GithubApiAlg, SubscribeServiceAlg}
import service.interpreters.{GithubApiAlg, SubscribeServiceAlg}
import service.route.HTTPServer
import service.helpers.DefaultAssertions._
import io.circe.parser._
import service.helpers.SampleData
import service.models.ApiModels.{InsertRepository, SubscriptionResponse}

import scala.concurrent.ExecutionContext

class SubscriberServiceRouteSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  private def decodeString: Response[IO] => String = { (r: Response[IO]) =>
    r.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString
  }

  private def assertRequest[F[_]: Async](
      request: Request[F],
      subscribeService: Option[SubscribeServiceAlg[F]] = None,
      githubApi: Option[GithubApiAlg[F]] = None,
      assertion: Response[F] => Assertion
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
      .asserting(assertion)
  }

  def buildRequest[F[_]](
      path: String,
      method: Method = GET,
      body: Option[String] = None
  ): Request[F] = {
    println("Path: " + path.toString())
    println("Method: " + method.toString())

    body match {
      case Some(body) =>
        println("Body: " + body)
        Request[F](method, Uri.fromString(path).getOrElse(uri"/")).withEntity(body)
      case None => Request[F](method, Uri.fromString(path).getOrElse(uri"/"))
    }
  }

  private def brokenSubscriberService[F[_]: MonadError[*[_], Throwable]]
      : Option[SubscribeServiceAlg[F]] =
    Some(SubscribeServiceAlg.stub2[F])

  private def brokenGithubService[F[_]: MonadError[*[_], Throwable]]: Option[GithubApiAlg[F]] =
    Some(GithubApiAlg.stub2[F])

  "Calling /subscriber/{xxx}" - {

    "with a GET request " - {

      "should return an OK status code" in {

        assertRequest[IO](
          request = buildRequest("/subscriber/1234"),
          assertion = okStatus()
        )
      }

      "should return an InternalServerError" in {

        assertRequest[IO](
          request = buildRequest("/subscriber/1234"),
          subscribeService = brokenSubscriberService,
          assertion = internalServerErrorStatus()
        )
      }

      "should return an NotFound status code" in {

        assertRequest[IO](
          request = buildRequest("/subscriber/7979"),
          assertion = notFoundStatus()
        )
      }

      "should return a valid Json String" in {

        assertRequest[IO](
          request = buildRequest("/subscriber/1234"),
          assertion = (r: Response[IO]) =>
            parse(decodeString(r)) match {
              case Left(_)  => assert(false)
              case Right(_) => assert(true)
            }
        )
      }

      "should return the correct decoded result" in {

        assertRequest[IO](
          request = buildRequest("/subscriber/1234"),
          assertion = (r: Response[IO]) =>
            decode[SubscriptionResponse](decodeString(r)) match {
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
            path = "/subscriber/8000",
            method = PUT,
            body = Some(Encoder[InsertRepository].apply(SampleData.dataToInsert).toString())
          ),
          assertion = createdStatus()
        )
      }

      "should return an InternalServerError status code when there is a service level error" in {
        assertRequest[IO](
          request = buildRequest(
            path = "/subscriber/8000",
            method = PUT,
            body = Some(Encoder[InsertRepository].apply(SampleData.dataToInsert).toString())
          ),
          githubApi = brokenGithubService[IO],
          assertion = internalServerErrorStatus()
        )
      }

      "should return a BadRequest when " - {

        "malformed data is sent" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscriber/8000",
              method = PUT,
              body = Some("{Malformed Json")
            ),
            githubApi = brokenGithubService[IO],
            assertion = badRequestStatus()
          )
        }

        "an organisation does not exist" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscriber/8000",
              method = PUT,
              body =
                Some(Encoder[InsertRepository].apply(SampleData.nonExistantOrganisation).toString())
            ),
            assertion = badRequestStatus()
          )
        }

        "a repo does not exist on a valid organisation" in {

          assertRequest[IO](
            request = buildRequest(
              path = "/subscriber/8000",
              method = PUT,
              body = Some(Encoder[InsertRepository].apply(SampleData.dataToInsert2).toString())
            ),
            assertion = badRequestStatus()
          )
        }
      }
    }

  }

}
