package service.database.converters

import doobie.Read
import service.domain.ApiModels
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}

import java.time.OffsetDateTime

import doobie.implicits.javatimedrivernative._
object Subscription {

  implicit val subscriptionRead: Read[ApiModels.Subscription] =
    Read[(String, String, OffsetDateTime)].map { case (x, y, z) =>
      ApiModels.Subscription(Organisation(x), Repositoryy(y), z)
    }

}
