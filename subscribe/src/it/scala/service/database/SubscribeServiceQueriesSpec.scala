package service.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.scalatest.{AnalysisMatchers, IOChecker}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest._
import org.scalatest.freespec.AnyFreeSpec
import service.database.connection.DBConnection
import service.database.connection.DBConnection.{createTables, dropTables}
import service.database.queries.SubscribeServiceQueries
import service.domain.ApiModels.{RepoId, SlackUserId}

import java.util.UUID

trait MatcherChecks[F[_]]
    extends AnyFreeSpec
    with matchers.must.Matchers
    with AnalysisMatchers[F]
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    DBConnection.buildTransactor().map(createTables).use(_ => IO.unit).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    DBConnection.buildTransactor().map(dropTables).use(_ => IO.unit).unsafeRunSync()
  }

  val id: SlackUserId = SlackUserId(UUID.randomUUID())
  val id2: RepoId     = RepoId(UUID.randomUUID())

  "Subscription insert query should pass type-check" in {
    SubscribeServiceQueries.insertSubscription must typecheck
  }

  "Users insert query should pass type-check" in {
    SubscribeServiceQueries.insertUser(id, "123", "123") must typecheck
  }

  "Repository query should pass type-check" in {
    SubscribeServiceQueries.insertRepository must typecheck
  }

  "ExistsUser query should pass type-check" in {
    SubscribeServiceQueries.existsUser(id) must typecheck
  }

  "ExistsRepo query should pass type-check" in {
    SubscribeServiceQueries.existsRepo(id2) must typecheck
  }

  "DeleteSubscription query should pass type-check" in {
    SubscribeServiceQueries.deleteSubscription(id, id2) must typecheck
  }

  lazy val transactor = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5436/test",
    "postgres",
    "mysecretpassword"
  )

}

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker
