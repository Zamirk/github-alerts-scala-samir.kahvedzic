package service.interpreters

import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.{KafkaContainer, SchemaRegistryContainer}
import org.testcontainers.containers.Network
import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

trait KafkaBaseSuite
    extends AsyncFreeSpec
    with Matchers
    with AsyncIOSpec
    with TestContainersForAll {

  override type Containers = KafkaContainer and SchemaRegistryContainer

  //this should be the same version that your lib is using under the hood
  val kafkaVersion = "7.0.0"

  //these are the default kafka host name but because that may change
  //we need to ensure that these are the values for container network, kafka and the schema registry
  val brokerId = 1
  val hostName = s"kafka$brokerId"

  def getKafkaAddress: String = withContainers { case kafkaContainer and _ =>
    (kafkaContainer.bootstrapServers)
  }

  override def startContainers(): Containers = {
    //a way to communicate containers
    val network: Network = Network.newNetwork()

    val kafkaContainer: KafkaContainer = KafkaContainer.Def(kafkaVersion).createContainer()
    kafkaContainer.container
      .withNetwork(network)
      .withNetworkAliases(hostName)
      .withEnv(
        Map[String, String](
          "KAFKA_BROKER_ID"                 -> brokerId.toString,
          "KAFKA_HOST_NAME"                 -> hostName,
          "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "false"
        ).asJava
      )
    kafkaContainer.start

    val schemaRegistryContainer: SchemaRegistryContainer =
      SchemaRegistryContainer.Def(network, hostName, kafkaVersion).start()

    kafkaContainer and schemaRegistryContainer
  }

}
