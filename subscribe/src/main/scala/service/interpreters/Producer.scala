package service.interpreters

import cats.effect._
import vulcan.Codec
import cats.implicits._
import fs2.kafka._
import fs2.kafka.vulcan.{AvroSettings, avroSerializer}
import service.algebras.kafka.ProducerAlg
import service.domain.ConfigModels.KeyValueMessageHolder

object Producer {
  def make[F[_]: Async, K: Codec, V: Codec](
      broker: String,
      s: AvroSettings[F],
      clientId: String,
      topic: String
  ): Resource[F, ProducerAlg[F, KeyValueMessageHolder[K, V]]] = {

    KafkaProducer
      .resource(
        ProducerSettings[F, K, V](avroSerializer[K].using(s), avroSerializer[V].using(s))
          .withBootstrapServers(broker)
          .withClientId(clientId)
      )
      .map(kf =>
        new ProducerAlg[F, KeyValueMessageHolder[K, V]] {
          override def sendMessage(message: KeyValueMessageHolder[K, V]): F[Unit] =
            kf
              .produce(ProducerRecords.one(ProducerRecord(topic, message.key, message.value)))
              .flatten
              .void
        }
      )
  }

  def stub1[F[_]: Sync, M]: ProducerAlg[F, M] = new ProducerAlg[F, M] {
    override def sendMessage(message: M): F[Unit] = Sync[F].pure(println(message))
  }

}
