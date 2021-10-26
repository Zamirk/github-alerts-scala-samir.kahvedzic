package service.interpreters

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import service.domain.ApiModels.SlackCommand.{Command, ResponseUrl, TriggerId, UserId}
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}
import service.domain.ApiModels._
import service.domain.errors.ApiErrors.SubscriberServiceException._
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.ExecutionContext
import service.interpreters._

class SubscribeServiceStubSpec extends AsyncFreeSpec with Matchers with AsyncIOSpec {
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  def runGetSubscription(s: SlackUserId): IO[SubscriptionsResponse] =
    stubs.SubscribeService.stub[IO].getSubscriptions(s)

  def runDeleteSubscription(s: SlackUserId, i: RepoCarrier): IO[Unit] =
    stubs.SubscribeService.stub[IO].deleteSubscription(s, i)

  def runPostSlackCommand(s: SlackUserId, sc: SlackCommand): IO[SlackResponseBody] =
    stubs.SubscribeService.stub[IO].postSlackCommand(s, sc)

  val uuid_1 = """Samir_Kah"""
  val uuid_2 = """Ali_Shah"""

  private lazy val validUser: SlackUserId = SlackUserId(UUID.nameUUIDFromBytes(uuid_1.getBytes))

  private lazy val invalidUser: SlackUserId = SlackUserId(UUID.nameUUIDFromBytes(uuid_2.getBytes))

  private lazy val validOrgRepo = RepoCarrier(Repository("47deg", "thool"))

  private lazy val invalidRepo = RepoCarrier(Repository("47deg", "NotFound"))

  private lazy val invalidOrg = RepoCarrier(Repository("48deg", "thool"))

  val offsetDatTime = OffsetDateTime.of(2000, 10, 10, 10, 10, 10, 10, ZoneOffset.ofHours(1))

  private lazy val successfulResult: SubscriptionsResponse = SubscriptionsResponse(
    List(
      Subscription(Organisation("47deg"), Repositoryy("thool"), offsetDatTime),
      Subscription(
        Organisation("higherkindness"),
        Repositoryy("skeuomorph"),
        offsetDatTime
      ),
      Subscription(Organisation("47degrees"), Repositoryy("github4s"), offsetDatTime)
    )
  )

  "Calling getSubscriptions" - {
    "should return a result when a result is found" in {
      runGetSubscription(validUser).asserting(_.shouldBe(successfulResult))
    }

    "should raise an UserNameNotFound error when a user name is not found" in {
      runGetSubscription(invalidUser).assertThrows[UserNameNotFound]
    }
  }

  "Calling deleteSubscription" - {
    "should return unit when the repo and user is found" in {
      runDeleteSubscription(
        validUser,
        validOrgRepo
      ).asserting(_.shouldBe(()))
    }

    "should raise an UserNameNotFound error when a user name is not found" in {
      runDeleteSubscription(
        invalidUser,
        validOrgRepo
      ).assertThrows[UserNameNotFound]
    }

    "should raise a RepositoryNotFound error when a repository is not found" in {
      runDeleteSubscription(
        validUser,
        invalidRepo
      ).assertThrows[RepositoryNotFound]
    }

    "should raise a RepositoryNotFound error when a organisation is not found" in {
      runDeleteSubscription(
        validUser,
        invalidOrg
      ).assertThrows[RepositoryNotFound]
    }

  }

  "Calling postSubscription should return valid data" in {
    runPostSlackCommand(
      validUser,
      SlackCommand(
        Command("/sub"),
        SlackCommand.Text(""),
        ResponseUrl(""),
        TriggerId(""),
        UserId("")
      )
    ).asserting(
      _ shouldBe SlackResponseBody.generateSlackResponse(
        "It's 80 degrees right now.",
        List(
          "*It's 80 degrees right now.*",
          "Partly cloudy today and tomorrow"
        )
      )
    )
  }

}
