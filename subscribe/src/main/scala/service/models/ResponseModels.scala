package service.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object ResponseModels {

  case class SubscriptionResponse(subscriptions: Subscription*)

  case class Subscription(organization: String, repository: String, subscribedAt: String)

  case class Repository(organization: String, repository: String)

  object Repository {
    implicit val rD1: Decoder[Repository] = deriveDecoder[Repository]
    implicit val rE2: Encoder[Repository] = deriveEncoder[Repository]
  }

  case class InsertRepository(subscription: Repository)

  object InsertRepository {
    implicit val rD2: Decoder[InsertRepository] = deriveDecoder[InsertRepository]
    implicit val rE3: Encoder[InsertRepository] = deriveEncoder[InsertRepository]
  }

}
