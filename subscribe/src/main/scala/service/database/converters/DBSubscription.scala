package service.database.converters

import doobie.{Read, Write}
import service.database.converters.DoobieGenericConverters.{From, To}
import service.domain.DatabaseModels.{DBSubscription, RequestType}
import cats._
import cats.data._
import java.time.{LocalDateTime, OffsetDateTime}
import cats._
import doobie.implicits._
import doobie.h2.implicits.UuidType
import service.database.converters.RequestType.{requestGet, requestPut}

import java.util.UUID
object DBSubscription {
  import doobie.implicits.javatimedrivernative._

  val subscriptionsRead: Read[DBSubscription]   = To[DBSubscription].genericRead
  val subscriptionsWrite: Write[DBSubscription] = From[DBSubscription].genericWrite

}
