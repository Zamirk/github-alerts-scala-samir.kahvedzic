package service.database.queries

import service.domain.DatabaseModels._
import doobie.postgres.implicits._
import service.domain.DatabaseModels.RequestType._
import doobie.implicits._
import service.database.converters.RequestType._
import service.database.converters.DBSubscription._
import service.database.converters.DBRepository._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.syntax._
import service.domain.ApiModels.{RepoId, SlackUserId, Subscription}
import service.database.converters.Subscription.subscriptionRead
import java.util.UUID

object SubscribeServiceQueries {

  def existsUser(s: SlackUserId): Query0[Boolean] = {
    sql"""
         SELECT exists(
             SELECT 1 FROM users
             WHERE user_id=${s.uuid}
        ) AS exists
        """.query[Boolean]
  }

  def existsRepo(sId: SlackUserId, rId: RepoId): Query0[Boolean] = {
    sql"""
    SELECT exists(
        SELECT 1 FROM repositories r, subscriptions s
        WHERE r.repository_id = ${rId.uuid}
        AND s.repository_id = ${rId.uuid}
        AND s.user_id = ${sId.uuid}
    ) AS exists
    """.query[Boolean]
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

  def upsertUser(
      user_id: SlackUserId,
      slack_user_id: String,
      slack_channel_id: String
  ): Update0 =
    sql"""INSERT INTO users (user_id, slack_user_id, slack_channel_id)
                            VALUES ($user_id, $slack_user_id, $slack_channel_id)
         ON CONFLICT (user_id)
         DO UPDATE SET slack_channel_id = $slack_channel_id 
         WHERE users.user_id = $user_id
             AND
        users.slack_user_id = $slack_user_id""".update

  def upsertRepository(db: DBRepository): Update0 =
    sql"""INSERT INTO repositories (repository_id, owner, repository) VALUES (${db.repository_id}, ${db.owner}, ${db.repository})
         ON CONFLICT (repository_id)
         DO UPDATE SET owner = ${db.owner}, repository = ${db.repository}
         WHERE repositories.repository_id = ${db.repository_id}
         """.update

  def insertSubscription(s: DBSubscription): Update0 =
    sql"""INSERT INTO subscriptions (user_id, repository_id, subscribed_at, subscription_type)
            VALUES (${s.user_id}, ${s.repository_id}, ${s.subscribed_at}, ${s.subscription_type})
         ON CONFLICT (user_id, repository_id)
         DO UPDATE
             SET subscription_type = ${s.subscription_type}, subscribed_at = ${s.subscribed_at}
         WHERE subscriptions.user_id = ${s.user_id}
             AND
        subscriptions.repository_id = ${s.repository_id}
         """.update

}
