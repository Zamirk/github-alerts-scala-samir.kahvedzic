package service.algebras

import service.models.ApiModels.{InsertRepository, SubscriptionResponse}

trait SubscribeServiceAlg[F[_]] {

  /** Returns a list of subscriptions
    * @param name               The id of the slack user
    * @return
    */
  def getSubscription(name: String): F[SubscriptionResponse]

  /** @param userName
    * @param sub
    * @return
    */
  def insertSubscription(userName: String, sub: InsertRepository): F[Unit]

}
