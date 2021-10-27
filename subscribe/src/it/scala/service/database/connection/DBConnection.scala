package service.database.connection

import cats.effect.IO
import cats.effect.kernel.Resource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

object DBConnection {

  val driver   = "org.postgresql.Driver"
  val url      = "jdbc:postgresql://localhost:5436/test"
  val user     = "postgres"
  val password = "mysecretpassword"

  def buildTransactor(): Resource[IO, HikariTransactor[IO]] = {
    for {
      db <- for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32)
        xa <- HikariTransactor.newHikariTransactor[IO](driver, url, user, password, ce)
      } yield xa
    } yield db
  }

  val fw: Flyway = Flyway
    .configure()
    .dataSource(url, user, password)
    .load()

}
