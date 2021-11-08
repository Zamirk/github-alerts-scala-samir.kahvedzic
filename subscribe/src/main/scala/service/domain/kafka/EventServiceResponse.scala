package service.domain.kafka
import cats.effect.Async
import cats.implicits.{catsSyntaxMonadError, catsSyntaxTuple3Semigroupal}
import service.domain.errors.ApiErrors.ApiRouteError.UsernameInvalidFormat
import vulcan.Codec

import java.util.UUID
object EventServiceResponse {

  final case class EventServiceResponse(
      message: String,
      organization: String,
      repository: String
  )

  implicit val responseCodec: Codec[EventServiceResponse] =
    Codec.record(
      name = "EventServiceResponse",
      namespace = "service.domain.kafka"
    ) { f =>
      (
        f("message", _.message),
        f("organization", _.organization),
        f("repository", _.repository)
      ).mapN(EventServiceResponse)
    }

}
