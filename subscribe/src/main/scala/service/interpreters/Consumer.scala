package service.interpreters
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import fs2.kafka._
import fs2.kafka.vulcan.{AvroSettings, avroDeserializer, avroSerializer}
import service.algebras.kafka.ConsumerAlg
import service.domain.kafka.EventServiceResponse.EventServiceResponse
import service.domain.kafka.EventServiceEvent.{KafkaMessage, Message}

object Consumer {

  type KleisliM[F[_]] =
    Kleisli[F, Message[String, EventServiceResponse], Message[String, EventServiceResponse]]

  def make[F[_]: Async](
      broker: String,
      clientId: String,
      groupId: String,
      topicConfig: String,
      schemaSettings: AvroSettings[F],
      logMsg: KleisliM[F]
  ): Resource[F, ConsumerAlg[F, Message[String, EventServiceResponse]]] = {

    KafkaConsumer
      .resource(
        ConsumerSettings[F, String, EventServiceResponse](
          avroDeserializer[String].using(schemaSettings),
          avroDeserializer[EventServiceResponse].using(schemaSettings)
        )
          .withBootstrapServers(broker)
          .withEnableAutoCommit(enableAutoCommit = true)
          .withClientId(clientId)
          .withGroupId(groupId)
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
      )
      .evalTap(_.subscribeTo(topicConfig))
      .evalTap(_.seekToBeginning)
      .map(c =>
        new ConsumerAlg[F, Message[String, EventServiceResponse]] {
          override def consumeAndSendMessage(
              topic: String
          ): fs2.Stream[F, Message[String, EventServiceResponse]] = {
            {

              c.stream.evalMap(m => logMsg(Message(m.record.key, m.record.value)))
            }
          }
        }
      )
  }

  def stub1[F[_]: Sync, M]: ConsumerAlg[F, Message[String, EventServiceResponse]] =
    new ConsumerAlg[F, Message[String, EventServiceResponse]] {

      override def consumeAndSendMessage(
          topic: String
      ): fs2.Stream[F, Message[String, EventServiceResponse]] = {
        val list: List[F[Message[String, EventServiceResponse]]] =
          List[F[Message[String, EventServiceResponse]]]()

        fs2.Stream.evalSeq(list.sequence)
      }
    }

}
