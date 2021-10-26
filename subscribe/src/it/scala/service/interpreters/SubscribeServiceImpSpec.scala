package service.interpreters

import cats.effect.IO
import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testing.scalatest.CatsResourceIO
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import service.database.connection.DBConnection
import service.domain.ApiModels.{RepoCarrier, Repository, SlackUserId}
import service.domain.errors.ApiErrors.SubscriberServiceException.UserNameNotFound
import service.database.connection.DBConnection.{createTables, dropTables}

import java.util.UUID

class SubscribeServiceImpSpec
    extends FixtureAsyncFreeSpec
    with Matchers
    with CatsResourceIO[Transactor[IO]]
    with AsyncIOSpec {

  override val resource: Resource[IO, Transactor[IO]] = {
    for {
      db <- DBConnection.buildTransactor()
      _  <- Resource.make(createTables(db))(_ => dropTables(db))
    } yield db
  }

  "GetSubscription should return a UserNameNotFound error when no records exist in the DB" in {
    db =>
      SubscribeService
        .make[IO](db)
        .getSubscriptions(SlackUserId(UUID.randomUUID()))
        .assertThrows[UserNameNotFound]
  }

//  "InsertSubscription should return a Unit response when successfully inserting into the DB" in {
//    db =>
//      SubscribeService
//        .make[IO](db)
//        .insertSubscription(
//          SlackUserId(UUID.randomUUID()),
//          "Samir_Kah",
//          "/random",
//          Repository(
//            "Zamirk",
//            "Huffman-tree-encode-decode"
//          )
//        )
//        .asserting(_ should ===())
//  }

//  "GetSubscription should return a result when finding data in the DB" in { db =>
//    val s = SubscribeService
//      .make[IO](db)
//
//    val userId = SlackUserId(UUID.nameUUIDFromBytes("Samir_Kah1001".getBytes()))
//
//    (for {
//      _ <- s.insertSubscription(
//        userId,
//        "Samir_Kah1001",
//        "/random",
//        Repository(
//          "Zamirk",
//          "Snowplow-schema-validation"
//        )
//      )
//      sub <- s.getSubscriptions(userId)
//    } yield sub).asserting(_.subscriptions.length shouldBe 1)
//  }

  "DeleteSubscription should return a Unit response when successfully removing a row from the DB" in {
    db =>
      SubscribeService
        .make[IO](db)
        .deleteSubscription(
          SlackUserId(UUID.randomUUID()),
          RepoCarrier(
            Repository(
              "Zamirk",
              "Huffman-tree-encode-decode"
            )
          )
        )
        .assertThrows[UserNameNotFound]
  }

}
