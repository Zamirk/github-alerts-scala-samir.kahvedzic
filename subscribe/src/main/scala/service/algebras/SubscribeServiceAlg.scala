package service.algebras

import service.domain.ApiModels._

trait SubscribeServiceAlg[F[_]] {

  def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse]

  def insertSubscription(
      slackId: SlackUserId,
      slackUser: String,
      slackChannel: String,
      sub: RepoCarrier
  ): F[Unit]

  def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit]

  def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody]

}
