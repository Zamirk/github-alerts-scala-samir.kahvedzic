package service.database.connection

import cats.effect.IO
import cats.effect.kernel.Resource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

object DBConnection {

  def buildTransactor(): Resource[IO, HikariTransactor[IO]] = {
    for {
      db <- for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32)
        xa <- HikariTransactor.newHikariTransactor[IO](
          "org.postgresql.Driver",
          "jdbc:postgresql://localhost:5436/test",
          "postgres",
          "mysecretpassword",
          ce
        )
      } yield xa
    } yield db
  }

  val fw = Flyway
    .configure()
    .dataSource("jdbc:postgresql://localhost:5436/test", "postgres", "mysecretpassword")
    .load()

}
