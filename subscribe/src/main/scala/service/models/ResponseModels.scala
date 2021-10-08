package service.models

object ResponseModels {

  final case class SubscriptionResponse(subscriptions: Subscription*)

  final case class Subscription(organization: String, repository: String, subscribedAt: String)

}
