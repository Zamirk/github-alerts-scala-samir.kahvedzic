package service.route

import cats.MonadThrow
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Async, IO}
import io.circe.Encoder
import io.circe.parser._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Method, Request, Response, Uri}
import org.scalatest.compatible.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.typelevel.log4cats.noop.NoOpLogger
import service.algebras.kafka.ProducerAlg
import service.algebras.services.{GithubApiAlg, SubscribeServiceAlg}
import service.domain.ApiModels._
import service.domain.{ConfigModels, Kafka}
import service.route.helpers.DefaultAssertions._
import service.route.helpers.SampleData
import service.interpreters.{GithubApi, Producer, SubscribeService}

import scala.concurrent.ExecutionContext

class SubscriberServiceSpec extends AsyncFreeSpec with AsyncIOSpec {
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  val malformedJson: Option[String] = Some("Malformed Json")

  val uuid_1 = """Samir_Kah"""
  val uuid_2 = """Ali_Shah"""
  val uuid_3 = """Test_User1"""
  val uuid_4 = """Test_User2"""

  private def decodeString: Response[IO] => String = { (r: Response[IO]) =>
    r.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString
  }

  type M = ConfigModels.KeyValueMessageHolder[String, Kafka.KafkaMessage]

  def fail(): Assertion = assert(false)
  def pass(): Assertion = assert(true)

  private def assertRequest[F[_]: Async](
      request: Request[F],
      subscribeService: Option[SubscribeServiceAlg[F]] = None,
      githubApi: Option[GithubApiAlg[F]] = None,
      producer: Option[ProducerAlg[F, M]] = None,
      assertion: Response[F] => Assertion
  ): F[Assertion] = {
    HTTPServer
      .generateRouter[F](
        subscribeService.getOrElse(SubscribeService.stub[F]),
        githubApi.getOrElse(GithubApi.stub[F]),
        producer.getOrElse(Producer.stub1),
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
    body match {
      case Some(body) =>
        Request[F](method, Uri.fromString(path).getOrElse(uri"/")).withEntity(body)
      case None => Request[F](method, Uri.fromString(path).getOrElse(uri"/"))
    }
  }

  private def brokensubscriptionService[F[_]: MonadThrow]: Option[SubscribeServiceAlg[F]] =
    Some(SubscribeService.stub2[F])

  private def brokenGithubService[F[_]: MonadThrow]: Option[GithubApiAlg[F]] =
    Some(GithubApi.stub2[F])

  "Calling /subscription/{xxx}" - {

    "with a GET request " - {

      "should return an OK status code" in {

        assertRequest[IO](
          request = buildRequest(s"/subscription/$uuid_1"),
          assertion = isOk
        )
      }

      "should return an InternalServerError" in {

        assertRequest[IO](
          request = buildRequest(s"/subscription/$uuid_1"),
          subscribeService = brokensubscriptionService,
          assertion = isInternalServerError
        )
      }

      "should return an NotFound status code" in {

        assertRequest[IO](
          request = buildRequest(s"/subscription/$uuid_4"),
          assertion = isNotFound
        )
      }

      "should return a valid Json String" in {

        assertRequest[IO](
          request = buildRequest(s"/subscription/$uuid_1"),
          assertion = (r: Response[IO]) =>
            parse(decodeString(r)) match {
              case Left(_)  => fail()
              case Right(_) => pass()
            }
        )
      }

      "should return the correct decoded result" in {

        assertRequest[IO](
          request = buildRequest(s"/subscription/$uuid_1"),
          assertion = (r: Response[IO]) =>
            decode[SubscriptionsResponse](decodeString(r)) match {
              case Left(_)     => fail()
              case Right(data) => assert(data === SampleData.sampleData)
            }
        )
      }

    }

    "with a PUT request " - {

      "should return a CREATED status code when valid data is sent" in {

        assertRequest[IO](
          request = buildRequest(
            path = s"/subscription/$uuid_1",
            method = PUT,
            body = Some(Encoder[RepoCarrier].apply(SampleData.sampleDataOne).toString())
          ),
          assertion = isCreated
        )
      }

      "should return an InternalServerError status code when there is a service level error" in {
        assertRequest[IO](
          request = buildRequest(
            path = s"/subscription/$uuid_3",
            method = PUT,
            body = Some(Encoder[RepoCarrier].apply(SampleData.sampleDataOne).toString())
          ),
          githubApi = brokenGithubService[IO],
          assertion = isInternalServerError
        )
      }

      "should return a BadRequest when " - {

        "malformed data is sent" in {

          assertRequest[IO](
            request = buildRequest(
              path = s"/subscription/$uuid_1",
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
              path = s"/subscription/$uuid_1",
              method = PUT,
              body = Some(Encoder[RepoCarrier].apply(SampleData.nonExistantOrganisation).toString())
            ),
            assertion = isBadRequest
          )
        }

        "a repo does not exist on a valid organisation" in {

          assertRequest[IO](
            request = buildRequest(
              path = s"/subscription/$uuid_1",
              method = PUT,
              body = Some(Encoder[RepoCarrier].apply(SampleData.dataToInsert2).toString())
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
            path = s"/subscription/$uuid_1",
            method = DELETE,
            body = Some(Encoder[RepoCarrier].apply(SampleData.sampleDataOne).toString())
          ),
          assertion = isNoContent
        )
      }

      "should return an InternalServerError status code when there is a service level error" in {
        assertRequest[IO](
          request = buildRequest(
            path = s"/subscription/$uuid_1",
            method = PUT,
            body = Some(Encoder[RepoCarrier].apply(SampleData.sampleDataOne).toString())
          ),
          githubApi = brokenGithubService[IO],
          assertion = isInternalServerError
        )
      }

      "should return a BadRequest when malformed data is sent" in {

        assertRequest[IO](
          request = buildRequest(
            path = s"/subscription/$uuid_3",
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
              path = s"/subscription/$uuid_4",
              method = DELETE,
              body = Some(Encoder[RepoCarrier].apply(SampleData.sampleDataOne).toString())
            ),
            assertion = isNotFound
          )
        }

        "the repository is not found" in {

          assertRequest[IO](
            request = buildRequest(
              path = s"/subscription/$uuid_1",
              method = DELETE,
              body = Some(Encoder[RepoCarrier].apply(SampleData.nonExistantOrganisation).toString())
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
            case Left(_) => fail()
            case Right(data) =>
              assert(
                data === SlackResponseBody(
                  ResponseField.InChannel,
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
