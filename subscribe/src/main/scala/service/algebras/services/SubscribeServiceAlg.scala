package service.algebras.services

import service.domain.ApiModels._
import service.domain.DatabaseModels.RequestType

trait SubscribeServiceAlg[F[_]] {

  def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse]

  def insertSubscription(
      id: SlackUserId,
      user: String,
      channel: String,
      r: Repository,
      e: RequestType = RequestType.OpenPR
  ): F[Unit]

  def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit]

  def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody]

}
