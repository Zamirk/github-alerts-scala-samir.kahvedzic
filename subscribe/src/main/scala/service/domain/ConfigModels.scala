package service.domain

object ConfigModels {
  case class Config(server: ServerConfig, database: DatabaseConfig)

  case class ServerConfig(host: String, port: Int)

  case class DatabaseConfig(
      driver: String,
      url: String,
      databaseUser: String,
      password: String
  )

}
