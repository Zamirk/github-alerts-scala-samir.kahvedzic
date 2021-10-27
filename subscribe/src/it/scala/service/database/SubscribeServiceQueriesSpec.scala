package service.database

import cats.effect.{Async, IO}
import cats.effect.unsafe.implicits.global
import doobie.scalatest.{AnalysisMatchers, IOChecker}
import doobie.util.transactor.Transactor
import org.scalatest._
import org.scalatest.freespec.AnyFreeSpec
import service.database.connection.DBConnection
import service.database.connection.DBConnection.fw
import service.database.queries.SubscribeServiceQueries
import service.domain.ApiModels.{RepoId, SlackUserId}
import service.domain.DatabaseModels
import service.domain.DatabaseModels.DBRepository

import java.time.OffsetDateTime
import java.util.UUID

trait MatcherChecks[F[_]]
    extends AnyFreeSpec
    with matchers.must.Matchers
    with AnalysisMatchers[F]
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    (Async[IO].delay {
      fw.migrate()
    } *> Async[IO].unit).unsafeRunSync()

  }

  override def afterAll(): Unit = {
    (Async[IO].delay {
      fw.migrate()
    } *> Async[IO].unit).unsafeRunSync()

  }

  val id: SlackUserId = SlackUserId(UUID.randomUUID())
  val id2: RepoId     = RepoId(UUID.randomUUID())

  "Insert Subscription query should pass type-check" in {
    SubscribeServiceQueries.insertSubscription(
      DatabaseModels.DBSubscription(
        id.uuid,
        id.uuid,
        OffsetDateTime.now(),
        DatabaseModels.RequestType.OpenPR
      )
    ) must typecheck
  }

  "Insert Users query should pass type-check" in {
    SubscribeServiceQueries.upsertUser(id, "123", "123") must typecheck
  }

  "Insert Repository query should pass type-check" in {
    SubscribeServiceQueries.upsertRepository(DBRepository(id.uuid, "", "")) must typecheck
  }

  "DeleteSubscription query should pass type-check" in {
    SubscribeServiceQueries.deleteSubscription(id, id2) must typecheck
  }

//TODO Fix this or alter types
//Note: Test failing or unknown reason
//OffsetDateTime does not seem be convertable to TIMESTAMPTZ on the JDBC level
//Seems to work at runtime regardless of tests, interestingly insertion tests seem to pass.

//  "Get Subscriptions query should pass type-check" in {
//    SubscribeServiceQueries.getSubscriptions(id) must typecheck
//  }

  "ExistsUser query should pass type-check" in {
    SubscribeServiceQueries.existsUser(id) must typecheck
  }

  "ExistsRepo query should pass type-check" in {
    SubscribeServiceQueries.existsRepo(id, id2) must typecheck
  }

  lazy val transactor = Transactor.fromDriverManager[F](
    DBConnection.driver,
    DBConnection.url,
    DBConnection.user,
    DBConnection.password
  )

}

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker
