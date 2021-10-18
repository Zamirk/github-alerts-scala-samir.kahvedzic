package service.interpreters

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import service.domain.ApiErrors.SubscriberServiceException.{RepositoryNotFound, UserNameNotFound}
import service.domain.ApiModels._
import service.domain.SlackModels
import service.domain.SlackModels.{SlackCommand, SlackResponseBody}
import service.domain.SlackModels.SlackCommand.{Command, ResponseUrl, TriggerId, UserId}

import scala.concurrent.ExecutionContext

class SubscribeServiceStubSpec extends AsyncFreeSpec with Matchers with AsyncIOSpec {
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  def runGetSubscription(s: SlackUserId): IO[SubscriptionsResponse] =
    SubscribeService.stub[IO].getSubscriptions(s)

  def runDeleteSubscription(s: SlackUserId, i: InsertRepository): IO[Unit] =
    SubscribeService.stub[IO].deleteSubscription(s, i)

  def runPostSlackCommand(s: SlackUserId, sc: SlackCommand): IO[SlackModels.SlackResponseBody] =
    SubscribeService.stub[IO].postSlackCommand(s, sc)

  private lazy val validUser: SlackUserId = SlackUserId("1234")

  private lazy val invalidUser: SlackUserId = SlackUserId("1235")

  private lazy val validOrgRepo = InsertRepository(Repository("47deg", "thool"))

  private lazy val invalidRepo = InsertRepository(Repository("47deg", "NotFound"))

  private lazy val invalidOrg = InsertRepository(Repository("48deg", "thool"))

  private lazy val successfulResult: SubscriptionsResponse = SubscriptionsResponse(
    List(
      Subscription("47deg", "thool", "2021-04-15T12:30:15"),
      Subscription("higherkindness", "skeuomorph", "2021-04-26T12:45:25"),
      Subscription("47degrees", "github4s", "2021-05-01T09:15:05")
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
