package service.domain

import java.time.OffsetDateTime
import java.util.UUID

object DatabaseModels {

  final case class DBRepository(
      repository_id: UUID,
      owner: String,
      repository: String
  )

  final case class DBSubscription(
      user_id: UUID,
      repository_id: UUID,
      subscribed_at: OffsetDateTime,
      subscription_type: RequestType
  )

  sealed trait RequestType extends Product with Serializable {
    def dbString: String
  }

  object RequestType {
    case object All extends RequestType {
      override val dbString: String = "All Events"
    }
    case object OpenPR extends RequestType {
      override val dbString: String = "Open Pull Request"
    }

    case object MergePR extends RequestType {
      override val dbString: String = "Merge Pull Request"
    }

    case object ApprovePR extends RequestType {
      override val dbString: String = "Approve Pull Request"
    }

    case object DeclinePR extends RequestType {
      override val dbString: String = "Decline Pull Request"
    }

  }

}
