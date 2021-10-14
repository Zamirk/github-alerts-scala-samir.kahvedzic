package service.algebras

import service.models.ApiModels.{InsertRepository, SlackUserId, SubscriptionsResponse}
import service.models.SlackModels.{SlackCommand, SlackResponseBody}

trait SubscribeServiceAlg[F[_]] {

  def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse]

  def insertSubscription(s: SlackUserId, sub: InsertRepository): F[Unit]

  def deleteSubscription(s: SlackUserId, sub: InsertRepository): F[Unit]

  def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody]

}
