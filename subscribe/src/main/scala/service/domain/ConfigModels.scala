package service.domain

import service.domain.ConfigModels.KafkaConfig.SchemaRegistry

object ConfigModels {
  case class Config(server: ServerConfig, database: DatabaseConfig, kafka: KafkaConfig)

  case class ServerConfig(host: String, port: Int)

  case class DatabaseConfig(
      driver: String,
      url: String,
      databaseUser: String,
      password: String
  )

  case class KafkaConfig(
      server: String,
      schemaRegistry: SchemaRegistry,
      topic: String,
      clientId: String
  )

  object KafkaConfig {
    final case class SchemaRegistry(uri: String, cachedSchemasPerSubject: Int)
  }

  final case class KeyValueMessageHolder[K, V](key: K, value: V)

}
