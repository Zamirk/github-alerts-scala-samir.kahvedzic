package service.domain.kafka

import cats.implicits.catsSyntaxTuple3Semigroupal
import service.domain.kafka.EventServiceEvent.Operation._
import vulcan.{AvroError, Codec}

object EventServiceEvent {

  final case class Message[K, V](key: K, value: V)

  final case class SubscriptionRecord(userName: String, message: KafkaMessage)

  final case class KafkaMessage(
      operationType: Operation,
      organization: String,
      repoName: String
  )

  sealed trait Operation { val s: String }

  object Operation {
    case object NewSubscription extends Operation {
      override val s: String = "NEW_SUBSCRIPTION"
    }

    case object ExistingSubscription extends Operation {
      override val s: String = "EXISTING_SUBSCRIPTION"
    }

    case object DeleteSubscription extends Operation {
      override val s: String = "DELETE_SUBSCRIPTION"
    }

  }

  object SubscriptionRecord {

    implicit val opCodec: Codec[Operation] =
      Codec.enumeration[Operation](
        name = "SubscriptionRecord.Operation",
        namespace = "service.domain.Kafka",
        doc = Some("Subscription Service"),
        symbols = List(NewSubscription.s, DeleteSubscription.s, ExistingSubscription.s),
        encode = {
          case DeleteSubscription   => DeleteSubscription.s
          case NewSubscription      => NewSubscription.s
          case ExistingSubscription => ExistingSubscription.s
        },
        decode = {
          case DeleteSubscription.s   => Right(Operation.DeleteSubscription)
          case NewSubscription.s      => Right(Operation.NewSubscription)
          case ExistingSubscription.s => Right(Operation.ExistingSubscription)
          case e                      => Left(AvroError(s"$e"))
        }
      )
  }

  import SubscriptionRecord.opCodec

  implicit val msgCodec: Codec[KafkaMessage] = Codec.record(
    name = "SubscriptionRecord.Message",
    namespace = "service.domain.Kafka"
  ) { f =>
    (
      f("operationType", _.operationType),
      f("organization", _.organization),
      f("repoName", _.repoName)
    ).mapN(KafkaMessage)
  }

}
