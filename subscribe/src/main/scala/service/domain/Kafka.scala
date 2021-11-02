package service.domain
import vulcan.{AvroError, Codec}
import cats.implicits._
import service.domain.Kafka.Operation.{DELETE_SUBSCRIPTION, EXISTING_SUBSCRIPTION, NEW_SUBSCRIPTION}

object Kafka {

  final case class SubscriptionRecord(userName: String, message: KafkaMessage)

  final case class KafkaMessage(
      operationType: Operation,
      organization: String,
      repoName: String
  )

  sealed trait Operation { val s: String }

  object Operation {
    case object NEW_SUBSCRIPTION extends Operation {
      override val s: String = "NEW_SUBSCRIPTION"
    }

    case object EXISTING_SUBSCRIPTION extends Operation {
      override val s: String = "EXISTING_SUBSCRIPTION"
    }

    case object DELETE_SUBSCRIPTION extends Operation {
      override val s: String = "DELETE_SUBSCRIPTION"
    }

  }

  object SubscriptionRecord {

    implicit val opCodec: Codec[Operation] =
      Codec.enumeration[Operation](
        name = "SubscriptionRecord.Operation",
        namespace = "service.domain.Kafka",
        doc = Some("Subscription Service"),
        symbols = List(NEW_SUBSCRIPTION.s, DELETE_SUBSCRIPTION.s, EXISTING_SUBSCRIPTION.s),
        encode = {
          case DELETE_SUBSCRIPTION   => DELETE_SUBSCRIPTION.s
          case NEW_SUBSCRIPTION      => NEW_SUBSCRIPTION.s
          case EXISTING_SUBSCRIPTION => EXISTING_SUBSCRIPTION.s
        },
        decode = {
          case DELETE_SUBSCRIPTION.s   => Right(Operation.DELETE_SUBSCRIPTION)
          case NEW_SUBSCRIPTION.s      => Right(Operation.NEW_SUBSCRIPTION)
          case EXISTING_SUBSCRIPTION.s => Right(Operation.EXISTING_SUBSCRIPTION)
          case e                       => Left(AvroError(s"$e"))
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
