package service.algebras

import service.models.ApiModels.{InsertRepository, SlackUserId, SubscriptionsResponse}

trait SubscribeServiceAlg[F[_]] {

  def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse]

  def insertSubscription(s: SlackUserId, sub: InsertRepository): F[Unit]

  def deleteSubscription(s: SlackUserId, sub: InsertRepository): F[Unit]

}
