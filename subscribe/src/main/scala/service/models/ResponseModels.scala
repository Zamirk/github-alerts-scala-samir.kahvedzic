package service.models

object ResponseModels {

  case class SubscriptionResponse(subscriptions: Subscription*)

  case class Subscription(organization: String, repository: String, subscribedAt: String)

}
