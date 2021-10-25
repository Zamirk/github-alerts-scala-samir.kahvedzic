package service.interpreters

import cats.effect.IO
import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testing.scalatest.CatsResourceIO
import cats.syntax.all._
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.flywaydb.core.Flyway
import service.domain.ApiModels.{RepoCarrier, Repository, SlackUserId}
import service.domain.errors.ApiErrors.SubscriberServiceException.UserNameNotFound

import java.util.UUID

class SubscribeServiceImpSpec
    extends FixtureAsyncFreeSpec
    with Matchers
    with CatsResourceIO[Transactor[IO]]
    with AsyncIOSpec {

  override val resource: Resource[IO, Transactor[IO]] = {
    for {
      db <- for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32)
        xa <- HikariTransactor.newHikariTransactor[IO](
          "org.postgresql.Driver",
          "jdbc:postgresql://localhost:5436/test",
          "postgres",
          "mysecretpassword",
          ce
        )
      } yield xa
      _ <- Resource.make(createTables(db))(_ => dropTables(db))
    } yield db
  }

  private def createTables(db: HikariTransactor[IO]): IO[Unit] = {
    db.configure { dataSource =>
      Async[IO].delay {
        Flyway.configure().dataSource(dataSource).load().migrate()
      } *> Async[IO].unit
    }
  }

  private def dropTables(db: HikariTransactor[IO]): IO[Unit] = {
    db.configure { dataSource =>
      IO.delay {
        Flyway.configure().dataSource(dataSource).load().clean()
      } *> Async[IO].unit
    }
  }

  //TODO
  //
  // IT: Tests are being refactored and will come in a future PR

  "GetSubscription should return a UserNameNotFound error when no records exist in the DB" in {
    db =>
      SubscribeService
        .make[IO](db)
        .getSubscriptions(SlackUserId(UUID.randomUUID()))
        .assertThrows[UserNameNotFound]
  }

  "InsertSubscription should return a Unit response when successfully inserting into the DB" in {
    db =>
      SubscribeService
        .make[IO](db)
        .insertSubscription(
          SlackUserId(UUID.randomUUID()),
          "Samir_Kah",
          "/random",
          RepoCarrier(
            Repository(
              "Zamirk",
              "Huffman-tree-encode-decode"
            )
          )
        )
        .asserting(_ should be())
  }

  "GetSubscription should return a result when finding data in the DB" in { db =>
    val s = SubscribeService
      .make[IO](db)

    val userId = SlackUserId(UUID.randomUUID())

    (for {
      _ <- s.insertSubscription(
        userId,
        "Samir_Kah2",
        "/random",
        RepoCarrier(
          Repository(
            "Zamirk",
            "Huffman-tree-encode-decode"
          )
        )
      )
      sub <- s.getSubscriptions(userId)
    } yield sub).asserting(_.subscriptions.length shouldBe 1)
  }

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
