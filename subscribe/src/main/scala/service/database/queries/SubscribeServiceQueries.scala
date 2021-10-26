package service.database.queries

import doobie.util.update.Update
import service.domain.DatabaseModels._
import doobie.postgres.implicits._
import service.domain.DatabaseModels.RequestType._
import doobie.implicits._
import service.database.converters.RequestType._
import service.database.converters.DBSubscription._
import service.database.converters.DBRepository._
import cats._
import cats.data._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.jawn._
import io.circe.syntax._

import java.awt.Point
import org.postgresql.util.PGobject
import service.domain.ApiModels.{RepoId, SlackUserId, Subscription}
import service.database.converters.Subscription.subscriptionRead

import java.util.UUID

object SubscribeServiceQueries {

  def existsUser(s: SlackUserId): Query0[Boolean] = {
    sql"""SELECT exists(SELECT 1 FROM users where user_id=${s.uuid}) AS exists""".query[Boolean]
  }
  def existsRepo(s: RepoId): Query0[Boolean] = {
    sql"""SELECT exists(SELECT 1 FROM repositories where repository_id=${s.uuid}) AS exists"""
      .query[Boolean]
  }

  def getSubscriptions(s: SlackUserId): Query0[Subscription] =
    sql"""
        SELECT owner, repository, subscribed_at
        FROM subscriptions, repositories
        WHERE subscriptions.user_id=${s.uuid}
          and repositories.repository_id = subscriptions.repository_id
      """.query[Subscription]

  def deleteSubscription(s: SlackUserId, r: RepoId): doobie.Update0 =
    sql"""
        DELETE
        FROM subscriptions
        WHERE subscriptions.user_id=${s.uuid} AND subscriptions.repository_id=${r.uuid}
    """.update

  def insertUser(
      user_id: SlackUserId,
      slack_user_id: String,
      slack_channel_id: String
  ): Update0 =
    sql"""INSERT INTO users (user_id, slack_user_id, slack_channel_id) VALUES ($user_id, $slack_user_id, $slack_channel_id)""".update

  def withColumns(c: List[String]): String =
    s""" ( ${c.mkString(",")}  ) VALUES ( ${List.fill(c.length)("?").mkString(",")}  ) """

  def insertRepository(): Update[DBRepository] =
    Update[DBRepository](
      s"""INSERT INTO repositories """ + withColumns(
        List(
          "repository_id",
          "owner",
          "repository"
        )
      )
    )

  def insertSubscription(): Update[DBSubscription] =
    Update[DBSubscription](
      s"""INSERT INTO subscriptions """ + withColumns(
        List(
          "user_id",
          "repository_id",
          "subscribed_at",
          "subscription_type"
        )
      )
    )

}
