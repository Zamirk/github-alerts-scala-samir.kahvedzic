package service.database

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.must.Matchers.not
import doobie.scalatest.{AnalysisMatchers, IOChecker}
import doobie.util.transactor.Transactor.Aux
import org.scalatest.freespec.AnyFreeSpec
import service.database.connection.DBConnection.db
import service.database.queries.SubscribeServiceQueries
import service.domain.ApiModels.{RepoId, SlackUserId}

import java.util.UUID

class SubscribeServiceQueriesSpec extends AnyFreeSpec with AnalysisMatchers[IO] with IOChecker {

  def transactor: Transactor[IO] = db

  val id: SlackUserId = SlackUserId(UUID.randomUUID())
  val id2: RepoId     = RepoId(UUID.randomUUID())
  /*
  "Subscription insert query should pass type-check" in {
    SubscribeServiceQueries.subscriptionsInsertionQuery must typecheck
  }

  "Users insert query should pass type-check" in {
    SubscribeServiceQueries.usersInsertionQuery(id, "123", "123") must typecheck
  }

  "Repository query should pass type-check" in {
    SubscribeServiceQueries.repositoryInsertionQuery must typecheck
  }

  "ExistsUser query should pass type-check" in {
    SubscribeServiceQueries.existsUser(id) must typecheck
  }

  "ExistsRepo query should pass type-check" in {
    SubscribeServiceQueries.existsRepo(id2) must typecheck
  }

  "DeleteSubscription query should pass type-check" in {
    SubscribeServiceQueries.deleteSubscription(id, id2) must typecheck
  }]*/

}
