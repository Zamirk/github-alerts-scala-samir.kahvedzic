package service.interpreters

import cats.effect.IO
import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testing.scalatest.CatsResourceIO
import org.flywaydb.core.Flyway
import org.scalatest.Assertion
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}
import service.domain.ApiModels._
import service.domain.errors.ApiErrors.SubscriberServiceException._

import java.util.UUID
import service.algebras.SubscribeServiceAlg
import service.database.connection.DBConnection

class SubscribeServiceImpSpec
    extends FixtureAsyncFreeSpec
    with CatsResourceIO[SubscribeServiceAlg[IO]]
    with Matchers
    with AsyncIOSpec {

  override val resource: Resource[IO, SubscribeServiceAlg[IO]] = {
    for {
      db <- DBConnection.buildTransactor()
      _ <- Resource.make(
        db.configure { dataSource =>
          Async[IO].delay {
            Flyway.configure().dataSource(dataSource).load().migrate()
          } *> Async[IO].unit
        }
      )(_ => {
        db.configure { dataSource =>
          IO.delay {
            Flyway.configure().dataSource(dataSource).load().clean()
          } *> Async[IO].unit
        }
      })
    } yield SubscribeService.make[IO](db)
  }

  def mkSlackId(s: Option[String] = None): SlackUserId = SlackUserId(
    s.map(v => UUID.nameUUIDFromBytes(v.getBytes)).getOrElse(UUID.randomUUID())
  )

  private def pass: Assertion = assert(true)
  private def fail: Assertion = assert(false)

  private val gOwner = "Zamirk"
  private val user   = "Samir_Kah"

  private val repoOne = Repository(gOwner, "Huffman-tree-encode-decode")
  private val repoTwo = Repository(gOwner, "Snowplow-schema-validation")

  private val channel1 = "/random"
  private val channel2 = "/general"

  "InsertSubscription " - {

    "should return Unit when successfully inserting" in { ss =>
      ss.insertSubscription(mkSlackId(), "", "", Repository("", ""))
        .asserting(_ should ===())
    }

    "should return Unit when successfully updating the same repo twice" in { ss =>
      val user2   = user + "_2"
      val slackId = mkSlackId(Some(user2))

      (for {
        _ <- ss.insertSubscription(slackId, user2, channel1, repoOne)
        b <- ss.insertSubscription(slackId, user2, channel2, repoOne)
      } yield b).asserting(_ should ===())

    }

  }

  "GetSubscription " - {
    " should return a UserNameNotFound error when no records exist in the DB" in { ss =>
      ss.getSubscriptions(SlackUserId(UUID.randomUUID())).assertThrows[UserNameNotFound]
    }

    " should return a SubscriptionsResponse with a single record that was inserted" in { ss =>
      val user3   = user + "_3"
      val slackId = mkSlackId(Some(user3))

      (for {
        _   <- ss.insertSubscription(slackId, user3, channel1, repoOne)
        res <- ss.getSubscriptions(slackId)
      } yield res).asserting {
        case SubscriptionsResponse(list) if list.size == 1 => pass
        case _                                             => fail
      }
    }

    " should return an updated record when a repo is subscribed again" in { ss =>
      val user4   = user + "_4"
      val slackId = mkSlackId(Some(user4))

      (for {
        _    <- ss.insertSubscription(slackId, user4, channel1, repoOne)
        res1 <- ss.getSubscriptions(slackId)
        _    <- ss.insertSubscription(slackId, user4, channel2, repoOne)
        res2 <- ss.getSubscriptions(slackId)
      } yield res1 -> res2).asserting {
        case (
              SubscriptionsResponse(List(Subscription(_, _, time1))),
              SubscriptionsResponse(List(Subscription(_, _, time2)))
            ) if time2.toInstant.toEpochMilli > time1.toInstant.toEpochMilli =>
          pass
        case _ => fail
      }
    }

    "should return multiple records for a single user" in { ss =>
      val user5   = user + "_5"
      val slackId = mkSlackId(Some(user5))

      (for {
        _   <- ss.insertSubscription(slackId, user5, channel1, repoOne)
        _   <- ss.insertSubscription(slackId, user5, channel1, repoTwo)
        res <- ss.getSubscriptions(slackId)
      } yield res).asserting {
        case SubscriptionsResponse(
              data @ List(Subscription(_, _, time1), Subscription(_, _, time2))
            )
            if data === List(
              Subscription(
                Organisation(repoOne.organization),
                Repositoryy(repoOne.repository),
                time1
              ),
              Subscription(
                Organisation(repoTwo.organization),
                Repositoryy(repoTwo.repository),
                time2
              )
            ) =>
          pass
        case _ => fail
      }
    }
  }

  "DeleteSubscription " - {
    "should return a UserNameNotFound error when a user is not found" in { ss =>
      ss.deleteSubscription(mkSlackId(), RepoCarrier(repoOne)).assertThrows[UserNameNotFound]
    }

    "should return a Unit response when data is deleted" in { ss =>
      val user6   = user + "_6"
      val slackId = mkSlackId(Some(user6))

      (for {
        _   <- ss.insertSubscription(slackId, user6, channel1, repoOne)
        res <- ss.deleteSubscription(slackId, RepoCarrier(repoOne))
      } yield res).asserting(_ should ===())
    }

    "should return a RepositoryNotFound error when a repository is not found on your subscription" in {
      ss =>
        val user7   = user + "_7"
        val slackId = mkSlackId(Some(user7))

        (for {
          _   <- ss.insertSubscription(slackId, user7, channel1, repoOne)
          res <- ss.deleteSubscription(slackId, RepoCarrier(repoTwo))
        } yield res).assertThrows[RepositoryNotFound]
    }

    "should remove a single subscription for a user" in { ss =>
      val user8   = user + "_8"
      val slackId = mkSlackId(Some(user8))

      (for {
        _   <- ss.insertSubscription(slackId, user8, channel1, repoOne)
        _   <- ss.insertSubscription(slackId, user8, channel1, repoTwo)
        _   <- ss.deleteSubscription(slackId, RepoCarrier(repoOne))
        res <- ss.getSubscriptions(slackId)
      } yield res).asserting(_.subscriptions.length shouldBe 1)
    }
  }
}
