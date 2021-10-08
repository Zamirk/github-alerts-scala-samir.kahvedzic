package service

import cats.effect.{Async, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Encoder
import org.http4s.{HttpRoutes, Method, Request, Uri}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import service.models.ApiModels.{InsertRepository, Repository, Subscription, SubscriptionResponse}
import org.typelevel.log4cats.noop.NoOpLogger

import scala.concurrent.ExecutionContext

class SubscriberServiceRouteSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  val logger: Logger[IO] = NoOpLogger[IO]

  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  private def buildRoutes[F[_]: Async](
      logger: Logger[F],
      subscribeService: SubscribeService[F],
      githubApi: GithubApi[F]
  ): HttpRoutes[F] = new SubscriberServiceRoute[F](subscribeService, githubApi, logger).routes

  private def buildRequest(username: String, method: Method = GET): Request[IO] =
    Request[IO](method, buildUri(username))

  def buildUri(s: String) = Uri.fromString(s).getOrElse(uri"/")

  val dataToInsert = {
    InsertRepository(
      Repository(
        "48deg",
        "thool"
      )
    )
  }

  val nonExistantRepo = {
    InsertRepository(
      Repository(
        "werwerwer",
        "thool"
      )
    )
  }
  val successfulResult = IO(
    SubscriptionResponse(
      Subscription(
        "47deg",
        "thool",
        "2021-04-15T12:30:15"
      ),
      Subscription(
        "higherkindness",
        "skeuomorph",
        "2021-04-26T12:45:25"
      ),
      Subscription(
        "47degrees",
        "github4s",
        "2021-05-01T09:15:05"
      )
    )
  )

  "Calling /subscriber/1234, a GET request should return a 200 status code" in {
    val subscribeService = SubscribeService.stub[IO]
    val githubApi        = GithubApi.stub[IO]

    buildRoutes(logger, subscribeService, githubApi)
      .orNotFound(
        buildRequest("1234")
      )
      .asserting(_.status shouldBe Ok)
  }

  "Calling /subscriber/notFoundId with a GET request should return an NotFound status" in {
    val subscribeService = SubscribeService.stub[IO]
    val githubApi        = GithubApi.stub[IO]

    new SubscriberServiceRoute[IO](subscribeService, githubApi, logger).routes
      .orNotFound(Request[IO](GET, buildUri(s"notFoundId")))
      .asserting(_.status shouldBe NotFound)
  }

//  "Calling /subscriber/unex, a GET request should return an InternalServerError" in {
//    val subscribeService = SubscribeService.stub[IO]
//    val githubApi        = GithubApi.stub[IO]
//
//    new SubscriberServiceRoute[IO](subscribeService, githubApi, logger).routes
//      .orNotFound(Request[IO](GET, buildUri(s"unex")))
//      .asserting(_.status shouldBe InternalServerError)
//  }

  "Calling /subscriber/1234, a PUT request should return a 201 status code" in {
    val subscribeService = SubscribeService.stub[IO]
    val githubApi        = GithubApi.stub[IO]

    buildRoutes(logger, subscribeService, githubApi)
      .orNotFound(
        Request[IO](PUT, buildUri("1234"))
          .withEntity(Encoder[InsertRepository].apply(dataToInsert).toString())
      )
      .asserting(_.status shouldBe Created)
  }

  "Calling /subscriber/1234, a PUT request should return a 404" in {
    val subscribeService = SubscribeService.stub[IO]
    val githubApi        = GithubApi.stub[IO]

    buildRoutes(logger, subscribeService, githubApi)
      .orNotFound(
        Request[IO](PUT, buildUri("1234"))
          .withEntity(Encoder[InsertRepository].apply(nonExistantRepo).toString())
      )
      .asserting(_.status shouldBe BadRequest)
  }

}
