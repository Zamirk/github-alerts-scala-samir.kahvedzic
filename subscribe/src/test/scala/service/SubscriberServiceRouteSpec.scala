package service

import cats.effect.{Async, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.{HttpRoutes, Request, Uri}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.SelfAwareStructuredLogger
import service.models.ResponseModels.{Subscription, SubscriptionResponse}

import scala.concurrent.ExecutionContext

class SubscriberServiceRouteSpec
    extends AsyncFreeSpec
    with AsyncMockFactory
    with AsyncIOSpec
    with Matchers {

  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  private def buildRoutes[F[_]: Async](
      logger: SelfAwareStructuredLogger[F],
      subscribeService: SubscribeService[F],
      githubApi: GithubApi[F]
  ): HttpRoutes[F] = new SubscriberServiceRoute[F](subscribeService, githubApi, logger).routes

  private def buildRequest(username: String): Request[IO] = Request[IO](GET, buildUri(username))

  def buildUri(s: String) = Uri.fromString(s).getOrElse(uri"/")

  val successfulResult = IO(
    Some(
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
  )

  "Calling /subscriber/1234, a GET request should return a 200 status code" in {
    val logger           = stub[SelfAwareStructuredLogger[IO]]
    val subscribeService = stub[SubscribeService[IO]]
    val githubApi        = stub[GithubApi[IO]]

    (subscribeService.getSubscription _)
      .when("1234")
      .returns(successfulResult)

    buildRoutes(logger, subscribeService, githubApi)
      .orNotFound(
        buildRequest("1234")
      )
      .asserting(_.status shouldBe Ok)
  }

  "Calling /subscriber/1234, should call a service level method" in {
    val logger           = stub[SelfAwareStructuredLogger[IO]]
    val subscribeService = stub[SubscribeService[IO]]
    val githubApi        = stub[GithubApi[IO]]

    (subscribeService.getSubscription _)
      .when("1234")
      .returns(successfulResult)

    new SubscriberServiceRoute[IO](subscribeService, githubApi, logger).routes
      .orNotFound(buildRequest("1234"))
      .map(_ => {

        (subscribeService.getSubscription _)
          .verify("1234")
          .atLeastOnce()

        assert(true)
      })
  }

//  "On /subscriber/1234, should return a json result" in {
//    val logger           = stub[SelfAwareStructuredLogger[IO]]
//    val subscribeService = stub[SubscribeService[IO]]
//
//    (subscribeService.getSubscription _)
//      .when("1234")
//      .returns(successfulResult)
//
//
//    val aa: IO[fs2.Stream[IO, String]] = buildRoutes(logger, subscribeService)
//      .orNotFound(
//        buildRequest("1234")
//      )
//      .map(_.bodyText)
//      .asserting(b => b.compile.foldMonoid shouldBe decodedjson)
//  }

  "Calling /subscriber/notFoundId with a GET request should return an internal server error" in {
    val logger           = stub[SelfAwareStructuredLogger[IO]]
    val subscribeService = stub[SubscribeService[IO]]
    val githubApi        = stub[GithubApi[IO]]

    (subscribeService.getSubscription _)
      .when("notFoundId")
      .returns(IO.raiseError(UserNameNotFound("notFoundId")))

    new SubscriberServiceRoute[IO](subscribeService, githubApi, logger).routes
      .orNotFound(Request[IO](GET, buildUri(s"notFoundId")))
      .asserting(_.status shouldBe NotFound)
  }

  "Calling /subscriber/unex, a GET request should return an InternalServerError" in {
    val logger           = stub[SelfAwareStructuredLogger[IO]]
    val subscribeService = stub[SubscribeService[IO]]
    val githubApi        = stub[GithubApi[IO]]

    (subscribeService.getSubscription _)
      .when("unex")
      .returns(IO.raiseError(new Exception("I bet you didn't expect this.")))

    new SubscriberServiceRoute[IO](subscribeService, githubApi, logger).routes
      .orNotFound(Request[IO](GET, buildUri(s"unex")))
      .asserting(_.status shouldBe InternalServerError)
  }

}
