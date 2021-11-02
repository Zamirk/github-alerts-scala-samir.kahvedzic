package service.algebras.kafka

case object InvalidBrokerAddress extends Throwable

trait ProducerAlg[F[_], A] {
  def sendMessage(message: A): F[Unit]
}
