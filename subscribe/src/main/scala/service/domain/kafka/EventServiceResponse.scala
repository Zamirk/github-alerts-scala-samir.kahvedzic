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
  ) {

    def genUUID[F[_]: Async](o: String, r: String): F[UUID] =
      Async[F]
        .delay {
          UUID.nameUUIDFromBytes((o + r).getBytes())
        }
        .adaptError({ _ =>
          UsernameInvalidFormat(o + r): Throwable
        }: PartialFunction[Throwable, Throwable])
  }

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
