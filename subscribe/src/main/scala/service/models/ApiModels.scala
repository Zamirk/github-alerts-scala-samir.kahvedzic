package service.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object ApiModels {

  final case class SubscriptionResponse(subscriptions: Subscription*)

  object SubscriptionResponse {
    implicit val sd: Decoder[SubscriptionResponse] = deriveDecoder[SubscriptionResponse]
    implicit val se: Encoder[SubscriptionResponse] = deriveEncoder[SubscriptionResponse]
  }

  final case class Subscription(organization: String, repository: String, subscribedAt: String)

  object Subscription {
    implicit val sd: Decoder[Subscription] = deriveDecoder[Subscription]
    implicit val se: Encoder[Subscription] = deriveEncoder[Subscription]
  }

  final case class InsertRepository(subscription: Repository)

  object InsertRepository {
    implicit val ri: Decoder[InsertRepository]  = deriveDecoder[InsertRepository]
    implicit val ri2: Encoder[InsertRepository] = deriveEncoder[InsertRepository]
  }

  final case class Repository(organization: String, repository: String)

  object Repository {
    implicit val rD1: Decoder[Repository] = deriveDecoder[Repository]
    implicit val rE2: Encoder[Repository] = deriveEncoder[Repository]
  }

}
