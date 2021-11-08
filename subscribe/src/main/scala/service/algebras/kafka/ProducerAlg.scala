package service.algebras.kafka

trait ProducerAlg[F[_], A] {
  def sendMessage(message: A): F[Unit]
}
