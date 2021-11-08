package service.algebras.kafka

import service.domain.kafka.EventServiceEvent.Message
import service.domain.kafka.EventServiceResponse.EventServiceResponse

case object InvalidBrokerAddress extends Throwable

trait ConsumerAlg[F[_], A] {
  def consumeAndSendMessage(topic: String): fs2.Stream[F, Message[String, EventServiceResponse]]
}
